package difftestSim

import chisel3._
import nucpu.Configs

class RAMHelper()(implicit val p: Configs) extends BlackBox {
  val io = IO(new Bundle {
    val clk: Clock = Input(Clock())
    val en: Bool = Input(Bool())
    val rIdx: UInt = Input(UInt(p.busWidth.W))
    val rdata: UInt = Output(UInt(p.busWidth.W))
    val wIdx: UInt = Input(UInt(p.busWidth.W))
    val wdata: UInt = Input(UInt(p.busWidth.W))
    val wmask: UInt = Input(UInt(p.busWidth.W))
    val wen: Bool = Input(Bool())
  })
}
