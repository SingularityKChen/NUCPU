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
  protected val mem: RAMHelper = Module(new RAMHelper())
  mem.io.clk := this.clock
  mem.io.en := !this.reset.asBool() && nucpu.io.instValid
  mem.io.rIdx := (nucpu.io.instAddr - p.pcStart.U) >> 3
  mem.io.wIdx := DontCare
  mem.io.wen := DontCare
  mem.io.wdata := DontCare
  mem.io.wmask := DontCare
  nucpu.io.inst := Mux(nucpu.io.instAddr(2), mem.io.rdata(63, 32), mem.io.rdata(31, 0))

  io.uart.in.valid := false.B
  io.uart.out.valid := false.B
  io.uart.out.ch := 0.U

}

