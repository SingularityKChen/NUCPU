package nucpu

import chisel3._
import difftest._

class RegFile()(implicit val p: Configs) extends Module {
  val io: RegFileIOs = IO(new RegFileIOs())
  override val desiredName = "regfile"
  protected val regFiles: Vec[UInt] = RegInit(VecInit(Seq.fill(p.regNum)(0.U(p.busWidth.W))))
  protected val rs1Wire: UInt = Wire(UInt(p.busWidth.W))
  protected val rs2Wire: UInt = Wire(UInt(p.busWidth.W))
  protected val wAddrWire: UInt = io.wAddr
  protected val a0Wire: UInt = regFiles(10.U)
  when(io.wEn & (wAddrWire =/= 0.U)){
    regFiles(wAddrWire) := io.wData
  }
  rs1Wire := Mux(io.rs1REn, regFiles(io.rs1RAddr), 0.U)
  rs2Wire := Mux(io.rs2REn, regFiles(io.rs2RAddr), 0.U)
  io.rs1RData := rs1Wire
  io.rs2RData := rs2Wire
  io.putchData := a0Wire
  // for DiffTest
  if (p.diffTest) {
    io.trapCode.foreach(x => x := a0Wire(2, 0))
    val mod: DifftestArchIntRegState = Module(new DifftestArchIntRegState)
    mod.io.clock := this.clock
    mod.io.coreid := 0.U
    mod.io.gpr.zipWithIndex.foreach({ case (int, i) =>
      int := regFiles(i.U)
    })
  }
}
