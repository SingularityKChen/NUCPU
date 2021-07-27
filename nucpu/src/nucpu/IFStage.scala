package nucpu

import chisel3._

class IFStage()(implicit val p: Configs) extends Module {
  val io: IFStageIOs = IO(new IFStageIOs())
  override val desiredName = "if_stage"
  protected val pc: UInt = RegInit(p.pcStart.U(p.busWidth.W))
  pc := Mux(io.jumpPC, io.nextPC, pc + 4.U)
  io.curPC := pc
  io.instEn := !this.reset.asBool()
}
