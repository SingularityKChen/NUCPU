package nucpu

import chisel3._
import chisel3.util._
import difftest._

class NUCPU()(implicit val p: Configs) extends Module {
  val io = IO(new Bundle {
    val inst: UInt = Input(UInt(p.instW.W))
    val inst_addr: UInt = Output(UInt(p.busWidth.W))
    val inst_ena: Bool = Output(Bool())
  })
  // TODO: if_stage.reset, to clear PC
  protected val if_stage: IFStage = Module(new IFStage())
  protected val id_stage: IDStage = Module(new IDStage())
  protected val exe_stage: EXEStage = Module(new EXEStage())
  protected val regfile: RegFile = Module(new RegFile())
  // inst fetch
  io.inst_addr := if_stage.io.inst_adder
  io.inst_ena := if_stage.io.inst_ena
  // if_stage -> exe_stage
  exe_stage.io.pc := if_stage.io.inst_adder
  // inst decode
  id_stage.io.inst := io.inst
  id_stage.io.curPC := if_stage.io.inst_adder
  // id_stage -> if_stage
  if_stage.io.nextPC := id_stage.io.jumpPCVal
  // id_stage -> regfile
  regfile.io.r_addr1 := id_stage.io.rs1_r_addr
  regfile.io.r_ena1 := id_stage.io.rs1_r_ena
  regfile.io.r_addr2 := id_stage.io.rs2_r_addr
  regfile.io.r_ena2 := id_stage.io.rs2_r_ena
  // exe
  // regfile -> exe_stage
  exe_stage.io.rs1_data := regfile.io.r_data1
  exe_stage.io.rs2_data := regfile.io.r_data2
  // id_stage -> exe_stage
  exe_stage.io.imm := id_stage.io.imm_data
  exe_stage.io.alu_fn := id_stage.io.alu_fn
  exe_stage.io.sel_alu1 := id_stage.io.sel_alu1
  exe_stage.io.sel_alu2 := id_stage.io.sel_alu2
  // write back
  // exe_stage -> if_stage
  protected val brTaken: Bool = id_stage.io.br & exe_stage.io.rd_data(0)
  if_stage.io.jumpPC := id_stage.io.jal | brTaken
  // id_stage -> regfile
  regfile.io.w_ena := id_stage.io.rd_w_ena
  regfile.io.w_addr := id_stage.io.rd_w_addr
  // exe_stage -> regfile
  regfile.io.w_data := exe_stage.io.rd_data
  // For DiffTest
  // Commit
  if (p.diffTest) {
    val commitDiffTest: DifftestInstrCommit = Module(new DifftestInstrCommit())
    commitDiffTest.io.clock := this.clock
    commitDiffTest.io.coreid := 0.U
    commitDiffTest.io.index := 0.U
    commitDiffTest.io.valid := RegNext(RegNext(if_stage.io.inst_ena && !this.reset.asBool(), false.B), false.B)
    commitDiffTest.io.pc := RegNext(RegNext(if_stage.io.inst_adder, 0.U), 0.U)
    commitDiffTest.io.instr := RegNext(RegNext(io.inst))
    commitDiffTest.io.skip := false.B
    commitDiffTest.io.isRVC := false.B
    commitDiffTest.io.scFailed := false.B
    commitDiffTest.io.wen := RegNext(RegNext(id_stage.io.rd_w_ena))
    commitDiffTest.io.wdata := RegNext(exe_stage.io.rd_data)
    commitDiffTest.io.wdest := RegNext(RegNext(id_stage.io.rd_w_addr))
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
    trapDIffTest.io.pc       := RegNext(RegNext(if_stage.io.inst_adder, 0.U), 0.U)
    trapDIffTest.io.cycleCnt := 0.U
    trapDIffTest.io.instrCnt := 0.U
  }
}
