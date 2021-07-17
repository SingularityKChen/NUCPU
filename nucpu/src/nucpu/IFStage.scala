package nucpu

import chisel3._

class IFStage()(implicit val p: Configs) extends Module {
  val io = IO(new Bundle {
    val inst_adder: UInt = Output(UInt(p.busWidth.W))
    val inst_ena: Bool = Output(Bool())
  })
  protected val pc: UInt = RegInit(0.U(p.busWidth.W))
  pc := pc + 4.U
  io.inst_adder := pc
  io.inst_ena := !this.reset.asBool()
}
