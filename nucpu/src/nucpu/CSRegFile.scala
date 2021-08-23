package nucpu

import chisel3._
import chisel3.util._
import difftest.DifftestCSRState
import nucpu.DecodeParams._

class CSRegFile(implicit val p: Configs) extends Module {
  val io = IO(new Bundle {
    val pc: UInt = Input(UInt(p.busWidth.W))
    val cmd: UInt = Input(UInt(CSR_X.length.W))
    val addr: UInt = Input(UInt(p.instImmW.W))
    val rData: UInt = Output(UInt(p.busWidth.W))
    val wData: UInt = Input(UInt(p.busWidth.W))
    val time: UInt = Output(UInt(p.busWidth.W))
    val eRet: Bool = Output(Bool())
    val evec: UInt = Output(UInt(p.busWidth.W))
    val status = new Bundle {
      val wfi: Bool = Output(Bool())
      val mie: Bool = Output(Bool())
      val isa: UInt = Output(UInt(p.instW.W))
    }
    val exception: Bool = Input(Bool())
    val cause: UInt = Input(UInt(p.busWidth.W))
  })
  protected val wEn: Bool = io.cmd(2) && (io.cmd(1) || io.cmd(0))
  protected val systemInst: Bool = io.cmd === ("b" + CSR_I).U
  protected val misa: UInt = RegInit("h4000100".U) // RV64I FIXME
  protected val mcycle: UInt = RegInit(0.U(p.busWidth.W))
  protected val mstatus: UInt = RegInit(3.U(p.busWidth.W))
  // Sstatus Write Mask
  // -------------------------------------------------------
  //    19           9   5     2
  // 0  1100 0000 0001 0010 0010
  // 0  c    0    1    2    2
  // -------------------------------------------------------
  val sstatusWmask: UInt = "hc6122".U
  // Sstatus Read Mask = (SSTATUS_WMASK | (0xf << 13) | (1ull << 63) | (3ull << 32))
  val sstatusRmask: UInt = sstatusWmask | "h8000000300018000".U
  // the privilege mode
  protected val curMode: UInt = RegInit(p.mMode.U(2.W))
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
  // privilege mode
  protected val isUMode: Bool = curMode === p.mMode.U
  protected val isSMode: Bool = curMode === p.sMode.U
  // System Instruction
  protected val isEBreak: Bool = io.addr === p.eBreakAddr && systemInst
  protected val isECall: Bool = io.addr === p.eCallAddr && systemInst
  protected val isMRet: Bool = io.addr === p.mRetAddr && systemInst
  protected val isSRet: Bool = io.addr === p.sRetAddr && systemInst
  protected val isURet: Bool = io.addr === p.uRetAddr && systemInst
  protected val isDRet: Bool = io.addr === p.dRetAddr && systemInst
  val exception: Bool = isECall || isEBreak || io.exception
  val cause: UInt =
    Mux(isECall, curMode + Causes.user_ecall.U,
      Mux(isEBreak, Causes.breakpoint.U, io.cause)
    )
  io.eRet := isEBreak || isECall || isMRet || isSRet || isURet || isDRet
  // MRO CSRs
  protected val roRegMap: Array[(UInt, UInt)] = Array(
    CSRs.misa.U -> misa,
  )
  // MRW CSRs
  protected val rwRegMap: Array[(UInt, UInt)] = Array(
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
    CSRs.medeleg.U -> medeleg,
  )
  io.time := mcycle
  io.status.wfi := wfi
  io.status.mie := mie // FIXME
  io.status.isa := misa
  // CSR Read
  io.rData := MuxLookup(io.addr, 0.U, rwRegMap ++ roRegMap)
  // CSR Write
  mcycle := Mux(wEn && io.addr === CSRs.mcycle.U, io.wData, mcycle + 1.U)
  mepc :=
    Mux(wEn && io.addr === CSRs.mepc.U, io.wData,
    Mux(isUMode && exception, Cat(io.pc(p.busWidth-1, 1), 0.U), mepc))
  mcause :=
    Mux(wEn && io.addr === CSRs.mcause.U, io.wData, // if write
    Mux(isUMode && exception, cause, mcause) // if exception
  )
  // FIXME
  mstatus := Mux(wEn && io.addr === CSRs.mstatus.U, io.wData, mstatus)
  io.evec := Mux(isUMode, mepc, sepc)
    if (p.diffTest) {
    val csrDiffTest = Module(new DifftestCSRState)
    csrDiffTest.io.clock := clock
    csrDiffTest.io.coreid := 0.U
    csrDiffTest.io.priviledgeMode := RegNext(curMode)
    csrDiffTest.io.mstatus := mstatus
    csrDiffTest.io.sstatus := mstatus & sstatusRmask
    csrDiffTest.io.mepc := RegNext(mepc)
    csrDiffTest.io.sepc := sepc
    csrDiffTest.io.mtval:= mtval
    csrDiffTest.io.stval:= stval
    csrDiffTest.io.mtvec := mtvec
    csrDiffTest.io.stvec := stvec
    csrDiffTest.io.mcause := RegNext(mcause)
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
