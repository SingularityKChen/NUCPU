package nucpu

import chisel3._
import difftest.DifftestCSRState

class CSRegFile(implicit val p: Configs) extends Module {
  val io = IO(new Bundle {
    val cycle: UInt = Output(UInt(p.busWidth.W))
  })
  protected val mcycle: UInt = RegInit(0.U(p.busWidth.W))
  protected val priviledgeMode: UInt = RegInit(0.U(2.W))
  protected val mstatus: UInt = RegInit(0.U(p.busWidth.W))
  protected val sstatusRmask: UInt = RegInit(0.U(p.busWidth.W))
  protected val mcause: UInt = RegInit(0.U(p.busWidth.W))
  protected val mepc: UInt = RegInit(0.U(p.busWidth.W))
  protected val sepc: UInt = RegInit(0.U(p.busWidth.W))
  protected val mtval: UInt = RegInit(0.U(p.busWidth.W))
  protected val stval: UInt = RegInit(0.U(p.busWidth.W))
  protected val mtvec: UInt = RegInit(0.U(p.busWidth.W))
  protected val stvec: UInt = RegInit(0.U(p.busWidth.W))
  protected val scause: UInt = RegInit(0.U(p.busWidth.W))
  protected val satp: UInt = RegInit(0.U(p.busWidth.W))
  protected val mipReg: UInt = RegInit(0.U(p.busWidth.W))
  protected val mie: UInt = RegInit(0.U(p.busWidth.W))
  protected val mscratch: UInt = RegInit(0.U(p.busWidth.W))
  protected val sscratch: UInt = RegInit(0.U(p.busWidth.W))
  protected val mideleg: UInt = RegInit(0.U(p.busWidth.W))
  protected val medeleg: UInt = RegInit(0.U(p.busWidth.W))
  mcycle := mcycle + 1.U
  io.cycle := mcycle
  if (p.diffTest) {
    val csrDiffTest = Module(new DifftestCSRState)
    csrDiffTest.io.clock := clock
    csrDiffTest.io.coreid := 0.U
    csrDiffTest.io.priviledgeMode := priviledgeMode
    csrDiffTest.io.mstatus := mstatus
    csrDiffTest.io.sstatus := mstatus & sstatusRmask
    csrDiffTest.io.mepc := mepc
    csrDiffTest.io.sepc := sepc
    csrDiffTest.io.mtval:= mtval
    csrDiffTest.io.stval:= stval
    csrDiffTest.io.mtvec := mtvec
    csrDiffTest.io.stvec := stvec
    csrDiffTest.io.mcause := mcause
    csrDiffTest.io.scause := scause
    csrDiffTest.io.satp := satp
    csrDiffTest.io.mip := mipReg
    csrDiffTest.io.mie := mie
    csrDiffTest.io.mscratch := mscratch
    csrDiffTest.io.sscratch := sscratch
    csrDiffTest.io.mideleg := mideleg
    csrDiffTest.io.medeleg := medeleg
  }
}
