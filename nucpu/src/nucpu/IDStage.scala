package nucpu

import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._
import nucpu.DecodeParams._

class IDStage()(implicit val p: Configs) extends Module {
  val io = IO(new Bundle {
    val inst: UInt = Input(UInt(p.instW.W))
    val rs1_r_ena: Bool = Output(Bool())
    val rs2_r_ena: Bool = Output(Bool())
    val rs1_r_addr: UInt = Output(UInt(p.addrWidth.W))
    val rs2_r_addr: UInt = Output(UInt(p.addrWidth.W))
    val rd_w_ena: Bool = Output(Bool())
    val rd_w_addr: UInt = Output(UInt(p.addrWidth.W))
    val imm_data: UInt = Output(UInt(p.busWidth.W))
    val alu_fn: UInt = Output(UInt(FN_X.length.W))
    val sel_alu1: UInt = Output(UInt(width = A1_X.length.W))
    val sel_alu2: UInt = Output(UInt(width = A2_X.length.W))
  })
  override val desiredName = "id_stage"
  // TODO: add more
  protected val rd: UInt = io.inst(11, 7)
  protected val rs1: UInt = io.inst(19, 15)
  protected val rs2: UInt = io.inst(24, 20)
  protected val instDecoder: UInt = decoder(minimizer = EspressoMinimizer, input = io.inst, truthTable = rv64ITruthTable)
  protected val instCtrlWires: InstCtrlIOs = Wire(new InstCtrlIOs)
  instCtrlWires.connectDecoder(instDecoder)
  protected val immSign: UInt = io.inst(31).asUInt()
  protected val imm30_20: UInt = Mux(instCtrlWires.sel_imm === s"b$IMM_U".U, io.inst(30, 20), immSign)
  protected val imm19_12: UInt = Mux(instCtrlWires.sel_imm === BitPat("b01?"), io.inst(19, 12), immSign)
  // TODO: IMM_Z
  protected val imm11: UInt = MuxLookup(instCtrlWires.sel_imm, immSign, Array(
    s"b$IMM_UJ".U -> io.inst(20),
    s"b$IMM_SB".U -> io.inst(7),
    s"b$IMM_U".U -> 0.U,
  ))
  protected val imm10_5: UInt = Mux(instCtrlWires.sel_imm === s"b$IMM_U".U, 0.U, io.inst(30, 25))
  protected val imm4_1: UInt = Mux(instCtrlWires.sel_imm === BitPat("b00?"),
    io.inst(11, 8),
    Mux(instCtrlWires.sel_imm === BitPat("b01?"), io.inst(24, 21), 0.U)
  )
  protected val imm0: UInt = Mux(instCtrlWires.sel_imm === s"b$IMM_I".U,
    io.inst(20),
    Mux(instCtrlWires.sel_imm === s"b$IMM_S".U, io.inst(7), 0.U)
  )
  io.alu_fn := instCtrlWires.alu_fn
  io.sel_alu1 := instCtrlWires.sel_alu1
  io.sel_alu2 := instCtrlWires.sel_alu2
  io.rs1_r_ena := instCtrlWires.rxs1
  io.rs2_r_ena := instCtrlWires.rxs2
  io.rs1_r_addr := rs1
  io.rs2_r_addr := 0.U
  io.rd_w_ena := instCtrlWires.wxd
  //FIXME
  io.rd_w_addr := rd
  io.imm_data := Cat(Fill(p.busWidth - 31, immSign), imm30_20, imm19_12, imm11, imm10_5, imm4_1, imm0)
}
