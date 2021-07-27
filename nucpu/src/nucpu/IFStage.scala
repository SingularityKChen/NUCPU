package nucpu

import chisel3._

class IFStage()(implicit val p: Configs) extends Module {
  val io = IO(new Bundle {
    val nextPC: UInt = Input(UInt(p.busWidth.W))
    val jumpPC: Bool = Input(Bool())
    val inst_adder: UInt = Output(UInt(p.busWidth.W))
    val inst_ena: Bool = Output(Bool())
  })
  override val desiredName = "if_stage"
  protected val pc: UInt = RegInit(p.pcStart.U(p.busWidth.W))
  pc := Mux(io.jumpPC, io.nextPC, pc + 4.U)
  io.inst_adder := pc
  io.inst_ena := !this.reset.asBool()
}
