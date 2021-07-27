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
  // FIXME: correct the connections
  mem.io.en := !this.reset.asBool() && nucpu.io.instValid
  mem.io.rIdx := nucpu.io.instAddr
  mem.io.wIdx := DontCare
  mem.io.wen := DontCare
  mem.io.wdata := DontCare
  mem.io.wmask := DontCare
  nucpu.io.inst := mem.io.rdata

  io.uart.in.valid := false.B
  io.uart.out.valid := false.B
  io.uart.out.ch := 0.U

}

