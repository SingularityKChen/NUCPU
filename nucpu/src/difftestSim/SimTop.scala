package difftestSim

import chisel3._
import nucpu._
import difftest._

class SimTop()(implicit val p: Configs) extends Module {
  val io = IO(new Bundle {
    val logCtrl = new LogCtrlIO
    val perfInfo = new PerfInfoIO
    val uart = new UARTIO
  })
  protected val nucpu: NUCPU = Module(new NUCPU())
  protected val iMem: RAMHelper = Module(new RAMHelper())
  protected val dMem: RAMHelper = Module(new RAMHelper())
  iMem.io.clk := this.clock
  iMem.io.en := !this.reset.asBool() && nucpu.io.instValid
  iMem.io.rIdx := (nucpu.io.instAddr - p.pcStart.U) >> 3
  iMem.io.wIdx := DontCare
  iMem.io.wen := DontCare
  iMem.io.wdata := DontCare
  iMem.io.wmask := DontCare
  nucpu.io.inst := Mux(nucpu.io.instAddr(2), iMem.io.rdata(63, 32), iMem.io.rdata(31, 0))
  dMem.io.clk := this.clock
  dMem.io.en := !this.reset.asBool() && nucpu.io.memValid
  dMem.io.rIdx := (nucpu.io.memAddr - p.pcStart.U) >> 3
  nucpu.io.memRData := dMem.io.rdata
  dMem.io.wen := nucpu.io.memDoWrite
  dMem.io.wIdx := (nucpu.io.memAddr - p.pcStart.U) >> 3
  dMem.io.wdata := nucpu.io.memWData
  dMem.io.wmask := nucpu.io.memMask

  io.uart.in.valid := false.B
  io.uart.out.valid := false.B
  io.uart.out.ch := 0.U

}

