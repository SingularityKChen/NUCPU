package nucpu

import chisel3._
import chisel3.util._
import nucpu.DecodeParams._

class ALU()(implicit val p: Configs) extends Module {
  val io = IO(new Bundle {
    val op1: UInt = Input(UInt(p.busWidth.W))
    val op2: UInt = Input(UInt(p.busWidth.W))
    val func: UInt = Input(UInt(FN_X.length.W))
    val results: UInt = Output(UInt(p.busWidth.W))
  })
  // TODO: or shift 4,0 in RV32
  protected val aluShamt: UInt = io.op2(5, 0)
  protected val addWire: UInt = io.op1 + io.op2
  protected val slWire: Bits = io.op1 << aluShamt
  protected val srWire: Bits = io.op1 >> aluShamt
  protected val seqWire: Bool = io.op1 === io.op2
  protected val xorWire: UInt = io.op1 ^ io.op2
  protected val bigSWire: Bool = io.op1.asSInt() > io.op2.asSInt()
  protected val bigUWire: Bool = io.op1 > io.op2
  protected val orWire: UInt = io.op1 | io.op2
  protected val andWire: UInt = io.op1 & io.op2
  protected val subWire: UInt = io.op1 - io.op2
  protected val sraWire: Bits = io.op1.asSInt() >> aluShamt
  io.results := MuxLookup(key = io.func, default = 0.U, mapping = Array(
      FN_ADD -> addWire,
      FN_SL -> slWire,
      FN_SEQ -> seqWire,
      FN_SNE -> !seqWire,
      FN_XOR -> xorWire,
      FN_SR -> srWire,
      FN_OR -> orWire,
      FN_AND -> andWire,
      FN_SUB -> subWire,
      FN_SRA -> sraWire,
      FN_SLT -> (!bigSWire && !seqWire),
      FN_SGE -> (bigSWire | seqWire),
      FN_SLTU -> (!bigUWire && !seqWire),
      FN_SGEU -> (bigUWire | seqWire),
    ).map({ case (str, bits) => s"b$str".U -> bits.asUInt()})
  )
}
