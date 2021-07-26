package nucpu

import chisel3._
import DecodeParams._

class ALU()(implicit val p: Configs) extends Module {
  val io = IO(new Bundle {
    val op1: SInt = Input(SInt(p.busWidth.W))
    val op2: SInt = Input(SInt(p.busWidth.W))
    val func: UInt = Input(UInt(FN_X.length.W))
    val results: SInt = Output(SInt(p.busWidth.W))
  })
  io.results := Mux(io.func === ("b" + FN_ADD).U, io.op1 + io.op2, io.op1 & io.op2)
}
