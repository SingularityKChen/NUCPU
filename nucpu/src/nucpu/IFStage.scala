package nucpu

import chisel3._

class IFStage()(implicit val p: Configs) extends Module {
  val io: IFStageIOs = IO(new IFStageIOs())
  override val desiredName = "if_stage"
  protected val pc: UInt = RegInit(p.pcStart.U(p.busWidth.W))
  protected val pcAdd4: UInt = pc + 4.U
  pc := Mux(io.jumpPC, io.nextPC, pcAdd4)
  io.curPC := pc
  io.instEn := !this.reset.asBool()
  io.curPCAdd4 := pcAdd4
}
