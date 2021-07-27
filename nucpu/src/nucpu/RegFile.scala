package nucpu

import chisel3._
import difftest._

class RegFile()(implicit val p: Configs) extends Module {
  val io: RegFileIOs = IO(new RegFileIOs())
  override val desiredName = "regfile"
  protected val regFiles: Vec[UInt] = RegInit(VecInit(Seq.fill(p.regNum)(0.U(p.busWidth.W))))
  protected val rData1: UInt = RegInit(0.U(p.busWidth.W))
  protected val rData2: UInt = RegInit(0.U(p.busWidth.W))
  protected val w_addrRegNext: UInt = RegNext(io.wAddr)
  when(io.wEn & (w_addrRegNext =/= 0.U)){
    regFiles(w_addrRegNext) := io.wData
  }
  rData1 := Mux(io.rs1REn, regFiles(io.rs1RAddr), 0.U)
  rData2 := Mux(io.rs2REn, regFiles(io.rs2RAddr), 0.U)
  io.rs1RData := rData1
  io.rs2RData := rData2
  // for DiffTest
  if (p.diffTest) {
    val mod: DifftestArchIntRegState = Module(new DifftestArchIntRegState)
    mod.io.clock := this.clock
    mod.io.coreid := 0.U
    mod.io.gpr.zipWithIndex.foreach({ case (int, i) =>
      int := regFiles(i.U)
    })
  }
}
