package nucpu

import chisel3._
import chisel3.util._
import difftest.DifftestCSRState
import nucpu.DecodeParams._

class CSRegFile(implicit val p: Configs) extends Module {
  val io = IO(new Bundle {
    val cmd: UInt = Input(UInt(CSR_X.length.W))
    val addr: UInt = Input(UInt(p.instImmW.W))
    val rData: UInt = Output(UInt(p.busWidth.W))
    val wData: UInt = Input(UInt(p.busWidth.W))
    val time: UInt = Output(UInt(p.busWidth.W))
    val status = new Bundle {
      val wfi: Bool = Output(Bool())
      val mie: Bool = Output(Bool())
      val isa: UInt = Output(UInt(p.instW.W))
    }
  })
  protected val misa: UInt = RegInit("h4000100".U) // RV64I FIXME
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
  protected val mip: UInt = RegInit(0.U(p.busWidth.W))
  protected val mie: Bool = RegInit(false.B)
  protected val mscratch: UInt = RegInit(0.U(p.busWidth.W))
  protected val sscratch: UInt = RegInit(0.U(p.busWidth.W))
  protected val mideleg: UInt = RegInit(0.U(p.busWidth.W))
  protected val medeleg: UInt = RegInit(0.U(p.busWidth.W))
  protected val wfi: Bool = RegInit(false.B)
  mcycle := mcycle + 1.U
  io.time := mcycle
  io.status.wfi := wfi
  io.status.mie := mie // FIXME
  io.status.isa := misa
  io.rData := MuxLookup(io.addr, 0.U, Array(
    CSRs.mcycle.U -> mcycle,
    CSRs.mstatus.U -> mstatus,
    CSRs.mcause.U -> mcause,
    CSRs.mepc.U -> mepc,
    CSRs.sepc.U -> sepc,
    CSRs.mtval.U -> mtval,
    CSRs.stval.U -> stval,
    CSRs.mtvec.U -> mtvec,
    CSRs.stvec.U -> stvec,
    CSRs.scause.U -> scause,
    CSRs.satp.U -> satp,
    CSRs.mip.U -> mip,
    CSRs.mie.U -> mie,
    CSRs.mscratch.U -> mscratch,
    CSRs.sscratch.U -> sscratch,
    CSRs.mideleg.U -> mideleg,
    CSRs.medeleg.U -> medeleg
  ))
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
    csrDiffTest.io.mip := mip
    csrDiffTest.io.mie := mie
    csrDiffTest.io.mscratch := mscratch
    csrDiffTest.io.sscratch := sscratch
    csrDiffTest.io.mideleg := mideleg
    csrDiffTest.io.medeleg := medeleg
  }
}
