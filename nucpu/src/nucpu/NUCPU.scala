package nucpu

import chisel3._
import chisel3.util._
import difftest._

class NUCPU()(implicit val p: Configs) extends Module {
  val io: NUCPUIOs = IO(new NUCPUIOs())
  // TODO: if_stage.reset, to clear PC
  protected val ifStage: IFStage = Module(new IFStage())
  protected val idStage: IDStage = Module(new IDStage())
  protected val exeStage: EXEStage = Module(new EXEStage())
  protected val regFile: RegFile = Module(new RegFile())
  // inst fetch
  io.instAddr := ifStage.io.curPC
  io.instValid := ifStage.io.instEn
  // if_stage -> exe_stage
  exeStage.io.pc := ifStage.io.curPC
  // inst decode
  idStage.io.inst := io.inst
  idStage.io.curPC := ifStage.io.curPC
  // id_stage -> if_stage
  ifStage.io.nextPC := idStage.io.jumpPCVal
  // id_stage -> regfile
  regFile.io.rs1RAddr := idStage.io.rs1RAddr
  regFile.io.rs1REn := idStage.io.rs1REn
  regFile.io.rs2RAddr := idStage.io.rs2RAddr
  regFile.io.rs2REn := idStage.io.rs2REn
  // exe
  // regfile -> exe_stage
  exeStage.io.rs1Data := regFile.io.rs1RData
  exeStage.io.rs2Data := regFile.io.rs2RData
  // id_stage -> exe_stage
  exeStage.io.imm := idStage.io.immData
  exeStage.io.aluFn := idStage.io.aluFn
  exeStage.io.alu1Sel := idStage.io.alu1Sel
  exeStage.io.alu2Sel := idStage.io.alu2Sel
  // write back
  // exe_stage -> if_stage
  protected val brTaken: Bool = idStage.io.br & exeStage.io.rdData(0)
  ifStage.io.jumpPC := idStage.io.jal | brTaken
  // id_stage -> regfile
  regFile.io.wEn := idStage.io.rdWEn
  regFile.io.wAddr := idStage.io.rdWAddr
  // exe_stage -> regfile
  regFile.io.wData := exeStage.io.rdData
  // For DiffTest
  // Commit
  if (p.diffTest) {
    val commitDiffTest: DifftestInstrCommit = Module(new DifftestInstrCommit())
    commitDiffTest.io.clock := this.clock
    commitDiffTest.io.coreid := 0.U
    commitDiffTest.io.index := 0.U
    commitDiffTest.io.valid := RegNext(RegNext(ifStage.io.instEn && !this.reset.asBool(), false.B), false.B)
    commitDiffTest.io.pc := RegNext(RegNext(ifStage.io.curPC, 0.U), 0.U)
    commitDiffTest.io.instr := RegNext(RegNext(io.inst))
    commitDiffTest.io.skip := false.B
    commitDiffTest.io.isRVC := false.B
    commitDiffTest.io.scFailed := false.B
    commitDiffTest.io.wen := RegNext(RegNext(idStage.io.rdWEn))
    commitDiffTest.io.wdata := RegNext(exeStage.io.rdData)
    commitDiffTest.io.wdest := RegNext(RegNext(idStage.io.rdWAddr))
  }
  // CSR State
  if (p.diffTest) {
    val csrDiffTest = Module(new DifftestCSRState())
    csrDiffTest.io.clock := this.clock
    csrDiffTest.io.coreid := 0.U
    csrDiffTest.io.mstatus := 0.U
    csrDiffTest.io.mcause := 0.U
    csrDiffTest.io.mepc := 0.U
    csrDiffTest.io.sstatus := 0.U
    csrDiffTest.io.scause := 0.U
    csrDiffTest.io.sepc := 0.U
    csrDiffTest.io.satp := 0.U
    csrDiffTest.io.mip := 0.U
    csrDiffTest.io.mie := 0.U
    csrDiffTest.io.mscratch := 0.U
    csrDiffTest.io.sscratch := 0.U
    csrDiffTest.io.mideleg := 0.U
    csrDiffTest.io.medeleg := 0.U
    csrDiffTest.io.mtval:= 0.U
    csrDiffTest.io.stval:= 0.U
    csrDiffTest.io.mtvec := 0.U
    csrDiffTest.io.stvec := 0.U
    csrDiffTest.io.priviledgeMode := 0.U
  }
  // Trap
  if (p.diffTest) {
    val trapDIffTest = Module(new DifftestTrapEvent)
    trapDIffTest.io.clock    := this.clock
    trapDIffTest.io.coreid   := 0.U
    trapDIffTest.io.valid    := RegNext(RegNext(io.inst === "h0000006b".U))
    trapDIffTest.io.code     := 0.U // GoodTrap
    trapDIffTest.io.pc       := RegNext(RegNext(ifStage.io.curPC, 0.U), 0.U)
    trapDIffTest.io.cycleCnt := 0.U
    trapDIffTest.io.instrCnt := 0.U
  }
}
