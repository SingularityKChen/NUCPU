package nucpu

import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._
import difftest._
import nucpu.DecodeParams.M_XRD

class NUCPU()(implicit val p: Configs) extends Module {
  val io: NUCPUIOs = IO(new NUCPUIOs())
  // TODO: if_stage.reset, to clear PC
  protected val ifStage: IFStage = Module(new IFStage())
  protected val idStage: IDStage = Module(new IDStage())
  protected val memStage: MemStage = Module(new MemStage())
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
  ifStage.io.nextPC := Mux(idStage.io.jalr, Cat(exeStage.io.rdData(p.busWidth - 1, 1), 0.U), idStage.io.jumpPCVal)
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
  exeStage.io.aluDW := idStage.io.aluDW
  exeStage.io.aluFn := idStage.io.aluFn
  exeStage.io.alu1Sel := idStage.io.alu1Sel
  exeStage.io.alu2Sel := idStage.io.alu2Sel
  // memory stage
  memStage.io.exeData := regFile.io.rs2RData
  memStage.io.exeAddr := exeStage.io.rdData
  memStage.io.func3 := idStage.io.func3
  memStage.io.memCmd := idStage.io.memCmd
  memStage.io.exeMemValid := idStage.io.mem
  // cpu mem_stage -> memory
  io.memAddr := memStage.io.memAddr
  memStage.io.memRData := io.memRData
  io.memDoWrite := memStage.io.memDoWrite
  io.memWData := memStage.io.memWData
  io.memMask := memStage.io.memMask
  io.memValid := memStage.io.memValid
  // write back
  // exe_stage -> if_stage
  protected val brTaken: Bool = idStage.io.br & exeStage.io.rdData(0)
  ifStage.io.jumpPC := idStage.io.jal | brTaken | idStage.io.jalr
  // id_stage -> regfile
  regFile.io.wEn := idStage.io.rdWEn
  regFile.io.wAddr := idStage.io.rdWAddr
  // exe_stage -> regfile
  regFile.io.wData := Mux(idStage.io.mem, memStage.io.wbData,
    Mux(idStage.io.jalr, ifStage.io.curPCAdd4,exeStage.io.rdData)
  )
  // Putch
  when(io.inst === p.instPutch.U) {
    printf("%c\n", regFile.io.wData)
  }
  // For DiffTest
  if (p.diffTest) {
    // Commit
    val commitDiffTest: DifftestInstrCommit = Module(new DifftestInstrCommit())
    val instValidWire = ifStage.io.instEn && !this.reset.asBool() && (io.inst =/= 0.U)
    val instValidReg = RegNext(instValidWire)
    val curPCReg = RegNext(ifStage.io.curPC, 0.U)
    commitDiffTest.io.clock := this.clock
    commitDiffTest.io.coreid := 0.U
    commitDiffTest.io.index := 0.U
    commitDiffTest.io.valid := instValidReg
    commitDiffTest.io.pc := curPCReg
    commitDiffTest.io.instr := RegNext(io.inst)
    commitDiffTest.io.skip := commitDiffTest.io.instr === p.instPutch.U
    commitDiffTest.io.isRVC := false.B
    commitDiffTest.io.scFailed := false.B
    commitDiffTest.io.wen := RegNext(idStage.io.rdWEn)
    commitDiffTest.io.wdata := RegNext(regFile.io.wData)
    commitDiffTest.io.wdest := RegNext(idStage.io.rdWAddr)
    // Trap
    val trapDiffTest = Module(new DifftestTrapEvent)
    val cycleCnt = RegInit(0.U(p.busWidth.W))
    val instCnt = RegInit(0.U(p.busWidth.W))
    val trapReg = RegNext(io.inst === p.instTrap.U, false.B)
    cycleCnt := Mux(trapReg, 0.U, cycleCnt + 1.U)
    instCnt := instCnt + instValidWire
    trapDiffTest.io.clock    := this.clock
    trapDiffTest.io.coreid   := 0.U
    trapDiffTest.io.valid    := trapReg
    trapDiffTest.io.code     := 0.U // GoodTrap
    trapDiffTest.io.pc       := curPCReg
    trapDiffTest.io.cycleCnt := cycleCnt
    trapDiffTest.io.instrCnt := instCnt
    // Float point
    val fpRegDiffTest = Module(new DifftestArchFpRegState)
    fpRegDiffTest.io.clock := this.clock
    fpRegDiffTest.io.coreid := 0.U
    fpRegDiffTest.io.fpr.foreach(x => x := 0.U)
    // Load
    val loadDiffTest = Module(new DifftestLoadEvent)
    loadDiffTest.io.clock := this.clock
    loadDiffTest.io.coreid := 0.U
    loadDiffTest.io.index := 0.U
    loadDiffTest.io.valid := RegNext((idStage.io.memCmd === ("b" + M_XRD).U) && memStage.io.memValid)
    loadDiffTest.io.paddr := RegNext(memStage.io.memAddr)
    loadDiffTest.io.opType := RegNext(idStage.io.func3)
    loadDiffTest.io.fuType := Mux(loadDiffTest.io.valid, "h0c".U, 0.U)
    // Store
    val storeDiffTest = Module(new DifftestStoreEvent)
    val wMaskForDiffTest: UInt = decoder(
      minimizer = EspressoMinimizer,
      input = Cat(idStage.io.func3(1, 0)),
      truthTable = TruthTable(Map(
        BitPat("b11") -> BitPat("b" + "1" * 8),
        BitPat("b10") -> BitPat("b" + "0" * 4 + "1" * 4),
        BitPat("b01") -> BitPat("b" + "0" * 6 + "1" * 2),
        BitPat("b00") -> BitPat("b" + "0" * 7 + "1" * 1),
      ), BitPat("b" + "1"*(p.busWidth / 8))))
    storeDiffTest.io.clock := this.clock
    storeDiffTest.io.coreid := 0.U
    storeDiffTest.io.index := 0.U
    storeDiffTest.io.valid := RegNext(memStage.io.memDoWrite && memStage.io.memValid)
    storeDiffTest.io.storeAddr := RegNext(memStage.io.memAddr)
    storeDiffTest.io.storeData := RegNext(regFile.io.rs2RData)
    storeDiffTest.io.storeMask := RegNext(wMaskForDiffTest)
  }
}
