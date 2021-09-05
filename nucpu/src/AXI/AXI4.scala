package axi4

import chisel3._
import chisel3.util._

//IO
class AXI4()(implicit val p: AXIConfigs) extends Module {
  val io: AXI4IOs = IO(new AXI4IOs())

//Logic
  //master device signals
  //val Write:   Bool = Bool()
  //val Read:    Bool = Bool()

  //req: 1-write 0-read
  val w_trans: Bool = io.rw_i.bits.req === 1.U
  val r_trans: Bool = io.rw_i.bits.req === 0.U

  val w_valid: Bool = io.rw_i.valid & w_trans
  val r_valid: Bool = io.rw_i.valid & r_trans

  //handshake
  val ar_hs: Bool =  io.ar.valid & io.ar.ready
  val r_hs:  Bool =  io.r.valid & io.r.ready
  val aw_hs: Bool =  io.aw.valid & io.aw.ready
  val w_hs:  Bool =  io.w.valid & io.w.ready
  val b_hs:  Bool =  io.b.valid & io.b.ready

  val w_done:     Bool = w_hs & io.w.bits.last
  val r_done:     Bool = r_hs & io.r.bits.last
  val trans_done: Bool = Mux(w_trans, b_hs, r_done)

  //write state machine
  val w_idle :: w_address :: w_write :: w_response :: Nil = Enum(4)
  val state_w: UInt = RegInit(w_idle)

  when(w_valid){
    switch (state_w){
      is (w_idle){
        {state_w := w_address}
      }
      is (w_address){
        when (aw_hs) {state_w := w_write}
      }
      is (w_write){
        when (w_done) {state_w := w_response}
      }
      is (w_response){
        when (b_hs) {state_w := w_idle}
      }
    }
  }

  //read state machine
  val r_idle :: r_address :: r_read                :: Nil = Enum(3)
  val state_r: UInt = RegInit(r_idle)

  when(r_valid){
    switch (state_r){
      is (r_idle){
        {state_r := r_address}
      }
      is (r_address){
        when (ar_hs) {state_r := r_read}
      }
      is (r_read){
        when (r_done) {state_r := r_idle}
      }
    }
  }

  //process data
  val BLOCK_TRANS:    Bool = Mux(p.TRANS_LEN.U > 1.U, true.B, false.B)

  val aligned: Bool = BLOCK_TRANS | (io.rw_i.bits.rw_addr(p.ALIGNED_WIDTH-1, 0) === 0.U)

  //B H W D, rw_size= 00-B; 01-H; 10-W; 11-D
  val addr_op1: UInt = Wire(UInt(4.W))
  addr_op1      := Cat(Fill(4-p.ALIGNED_WIDTH,0.U), io.rw_i.bits.rw_addr(p.ALIGNED_WIDTH-1, 0))
  val addr_op2: UInt = Wire(UInt(4.W))
  addr_op2 := Mux(io.rw_i.bits.rw_size === "b-00".U,"b0000".U,Mux(io.rw_i.bits.rw_size === "b01".U,"b0001".U,Mux(io.rw_i.bits.rw_size === "b10".U,"b0011".U,"b0111".U)))
//  when(io.rw_i.bits.rw_size === "b-00".U) {
//    addr_op2 := "b0000".U
//  }
//    .elsewhen(io.rw_i.bits.rw_size === "b01".U){
//      addr_op2 := "b0001".U
//    }
//    .elsewhen(io.rw_i.bits.rw_size === "b10".U){ //IFStage size-w
//      addr_op2 := "b0011".U
//    }
//    .elsewhen(io.rw_i.bits.rw_size === "b11".U){
//      addr_op2 := "b0111".U
//    }

  val addr_end: UInt = Wire(UInt(4.W))
  addr_end := addr_op1 + addr_op2

  val overstep: Bool = addr_end(3,p.ALIGNED_WIDTH) =/= 0.U

  val axi_len: UInt = Wire(UInt(8.W))
  axi_len  := Mux(aligned, (p.TRANS_LEN - 1).U , Cat(Fill(7,0.U), overstep))

  //number of transmission
  val len:          UInt = RegInit(0.U(8.W))
  val len_reset:    UInt = (w_trans & w_idle) | (r_trans & r_idle)
  val len_incr_en:  UInt = (len =/= axi_len) & (w_hs | r_hs)

  when(len_reset === 1.U) {
    len := 0.U
  }
  .elsewhen(len_incr_en === 1.U){
    len := len + 1.U
  }


  //axisize
  val axi_size: UInt = Wire(UInt(3.W))
      axi_size  :=  p.AXI_SIZE.U(3.W)

  val axi_addr: UInt = Wire(UInt(p.addrBits.W))
      axi_addr := Cat(io.rw_i.bits.rw_addr(p.addrBits-1, p.ALIGNED_WIDTH), Fill(p.ALIGNED_WIDTH, 0.U))
  //aligned_offset_l=0, aligned_offset_h=64
  val aligned_offset_l: UInt = Wire(UInt(p.OFFSET_WIDTH.W))
      aligned_offset_l := Cat(Fill(p.OFFSET_WIDTH - p.ALIGNED_WIDTH, 0.U),io.rw_i.bits.rw_addr(p.ALIGNED_WIDTH-1,0)) << 3
  val aligned_offset_h: UInt = Wire(UInt(p.OFFSET_WIDTH.W))
      aligned_offset_h := p.dataBits.U - aligned_offset_l

  val mask: UInt = Wire(UInt(p.MASK_WIDTH.W))  //128bit
  mask := Mux(io.rw_i.bits.rw_size === "b00".U,"hff".U << aligned_offset_l,Mux(io.rw_i.bits.rw_size === "b01".U,"hffff".U << aligned_offset_l,Mux(io.rw_i.bits.rw_size === "b10".U,"hffffffff".U,"hffffffffffffffff".U)))
//  when(io.rw_i.bits.rw_size === "b00".U) {
//    mask := "hff".U << aligned_offset_l
//  }
//    .elsewhen(io.rw_i.bits.rw_size === "b01".U){
//      mask := "hffff".U << aligned_offset_l
//    }
//    .elsewhen(io.rw_i.bits.rw_size === "b10".U){ //IFStage size-w
//      mask := "hffffffff".U << aligned_offset_l
//    }
//    .elsewhen(io.rw_i.bits.rw_size === "b11".U) {
//      mask := "hffffffffffffffff".U << aligned_offset_l
//    }

  val mask_l: UInt = Wire(UInt(p.dataBits.W))
      mask_l := mask(p.dataBits-1, 0)

  val mask_h: UInt = Wire(UInt(p.dataBits.W))
      mask_h := mask(p.MASK_WIDTH-1, p.dataBits)

  val axi_id: UInt = Wire(UInt(p.idBits.W))
      axi_id  := Fill(p.idBits, 0.U)
  val axi_user: UInt = Wire(UInt(p.userBits.W))
      axi_user  := Fill(p.userBits, 0.U)

  val rw_ready_enable: Bool = Wire(Bool())
  val rw_ready: Bool= RegEnable(trans_done, false.B, rw_ready_enable)
      io.rw_i.ready := rw_ready
      rw_ready_enable := trans_done | rw_ready

  val rw_resp: UInt = RegEnable(Mux(w_trans, io.b.bits.resp, io.r.bits.resp), 0.U, trans_done)
      io.rw_o.rw_resp := rw_resp


  // ------------------Write Transaction------------------

  // Write address channel signals
  io.aw.valid     := (state_w === w_address)
  io.aw.bits.addr := axi_addr
  io.aw.bits.prot := 0.U
  io.aw.bits.id   := axi_id
  io.aw.bits.user := axi_user
  io.aw.bits.len  := axi_len
  io.aw.bits.size := axi_size
  io.aw.bits.burst:= 1.U
  io.aw.bits.lock := 0.U
  io.aw.bits.cache:= 2.U  //"b0010".U
  io.aw.bits.qos  := 0.U

  // Write data channel signals
  io.w.valid      := (state_w === w_write)
  io.w.bits.strb  := "b11111111".U
  io.w.bits.last  := len === axi_len             //len tranfer until the last one beat//the bigest len number
  io.w.bits.user  := axi_user

  //wdata is cut from rwdatawrite
  //rwdatawrite is input n64, wdata is output 64,
  //divide big one into small parts
//  val axi_w_data_l: UInt = Wire(UInt(p.dataBits.W))
//  axi_w_data_l := (io.w.bits.data & mask_l) >> aligned_offset_l //just low 32bit
//  val axi_w_data_h: UInt = Wire(UInt(p.dataBits.W))
//  axi_w_data_h := (io.w.bits.data & mask_h) >> aligned_offset_h
  val data_write: UInt = RegInit(0.U(p.dataBits.W))
  for (a <- 0 until p.TRANS_LEN){
    when (io.w.ready & io.w.valid){
      when ((aligned === false.B) & overstep){
        when(len(0) === 1.U) {
          data_write := io.rw_i.bits.data_write(p.dataBits - 1, 0) >> aligned_offset_l
        }.otherwise{
          data_write := io.rw_i.bits.data_write(p.dataBits - 1, 0) << aligned_offset_h
        }
      }.elsewhen(len === a.U){
        data_write := io.rw_i.bits.data_write(((a+1)*p.dataBits)-1,a*p.dataBits)
      }
    }
  }
//  val data_write: UInt = RegInit(0.U(p.dataBits.W))
//  for (a <- 0 until p.TRANS_LEN){
//    when (io.w.ready & io.w.valid){
//      when ((aligned === false.B) & overstep){
//        when(len(0) === 1.U) {
//          data_write(p.dataBits - 1, 0) := io.rw_i.bits.data_write(p.dataBits - 1, 0) >> aligned_offset_l
//        }.otherwise{
//          data_write(p.dataBits - 1, 0) := io.rw_i.bits.data_write(p.dataBits - 1, 0) << aligned_offset_h
//        }
//      }.elsewhen(len === a.U){
//        data_write(p.dataBits - 1, 0) := io.rw_i.bits.data_write(((a+1)*p.dataBits)-1,a*p.dataBits)
//      }
//    }
//  }
  io.w.bits.data  := data_write
  // Write response channel signals
  io.b.ready      := (state_w === w_response)



  // ------------------Read Transaction------------------

  // Read address channel signals
  io.ar.valid     := (state_r === r_address)
  io.ar.bits.addr := axi_addr
  io.ar.bits.prot := 0.U
  io.ar.bits.id   := axi_id
  io.ar.bits.user := axi_user
  io.ar.bits.len  := axi_len
  io.ar.bits.size := axi_size
  io.ar.bits.burst:= 1.U
  io.ar.bits.lock := 0.U
  io.ar.bits.cache:= 2.U  //"b0010".U
  io.ar.bits.qos  := 0.U

  // Read data channel signals
  io.r.ready     := (state_r === r_read)

  //Data transfer
  val axi_r_data_l: UInt = Wire(UInt(p.dataBits.W))
      axi_r_data_l := (io.r.bits.data & mask_l) >> aligned_offset_l //just low 32bit
  val axi_r_data_h: UInt = Wire(UInt(p.dataBits.W))
      axi_r_data_h := (io.r.bits.data & mask_h) >> aligned_offset_h


  val data_read: Vec[UInt] = RegInit(VecInit(Seq.fill(p.TRANS_LEN)(0.U(p.dataBits.W))))


  for (b <- 0 until p.TRANS_LEN){
    when (io.r.ready & io.r.valid){
      when ((aligned === false.B) & overstep){
        when(len(0) === 1.U){
          data_read(0) := data_read(0) | axi_r_data_h
        }
          .otherwise{
            data_read(0) := axi_r_data_l

        }

      }.elsewhen(len === b.U){
        data_read(b) := axi_r_data_l
      }
    }
    when(b.U === p.TRANS_LEN.U){
      io.rw_o.data_read := data_read //just can use one vec each time, how to put them together???
    }.otherwise(
      io.rw_o.data_read := 0.U(p.rwdataBits.U)
    )
  }



//  val data_read: UInt = RegInit(0.U(p.rwdataBits.W))
//  for (b <- 0 until p.TRANS_LEN){
//    when (io.r.ready & io.r.valid){
//      when ((aligned === false.B) & overstep){
//          when(len(0) === 1.U){
//            data_read(p.dataBits-1,0) := data_read(p.dataBits-1,0) | axi_r_data_h
//          }.otherwise{
//            data_read(p.dataBits-1,0) := axi_r_data_l
//          }
//
//      }.elsewhen(len === b.U){
//        data_read(((b+1)*p.dataBits)-1,b*p.dataBits) := axi_r_data_l
//      }
//    }
//  }
//  io.rw_o.data_read:= data_read

}

































////AXI Master
//class Master()(implicit val p: AXIConfigs) extends Module{
//  val io = IO(new Bundle(){
//    val out = new AXI4IOs()
//  })
//  //logic
//}
//
////AXI Slave
//class Slave()(implicit val p: AXIConfigs) extends Module{
//  val io = IO(new Bundle(){
//    val in = Flipped(new AXI4())
//  })
//  //logic
//}
//
////master and slave Module
//val master = Module(new Master())
//val slave  = MOdule(new Slave())
//
////AXI connection
//master.io.out <> slave.io.in

