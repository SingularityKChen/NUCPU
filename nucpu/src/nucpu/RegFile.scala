package nucpu

import chisel3._

class RegFile()(implicit val p: Configs) extends Module {
  val io = IO(new Bundle {
    val w_addr: UInt = Input(UInt(p.addrWidth.W))
    val w_data: UInt = Input(UInt(p.busWidth.W))
    val w_ena: Bool = Input(Bool())
    val r_addr1: UInt = Input(UInt(p.addrWidth.W))
    val r_data1: UInt = Output(UInt(p.busWidth.W))
    val r_ena1: Bool = Input(Bool())
    val r_addr2: UInt = Input(UInt(p.addrWidth.W))
    val r_data2: UInt = Output(UInt(p.busWidth.W))
    val r_ena2: Bool = Input(Bool())
  })
  override val desiredName = "regfile"
  protected val regFiles: Mem[UInt] = Mem(p.regNum, UInt(p.busWidth.W))
  protected val rData1: UInt = RegInit(0.U(p.busWidth.W))
  protected val rData2: UInt = RegInit(0.U(p.busWidth.W))
  protected val w_addrRegNext: UInt = RegNext(io.w_addr)
  when(io.w_ena & (w_addrRegNext =/= 0.U)){
    regFiles(w_addrRegNext) := io.w_data
  }
  rData1 := Mux(io.r_ena1, regFiles(io.r_addr1), 0.U)
  rData2 := Mux(io.r_ena2, regFiles(io.r_addr2), 0.U)
  io.r_data1 := rData1
  io.r_data2 := rData2
}
