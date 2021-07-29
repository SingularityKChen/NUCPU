package nucpu

import chisel3._
import chisel3.util._
import DecodeParams._

class EXEStage()(implicit val p: Configs) extends Module {
  val io: EXEStageIOs = IO(new EXEStageIOs())
  override val desiredName = "exe_stage"
  protected val alu: ALU = Module(new ALU())
  alu.io.func := io.aluFn
  alu.io.op1 := MuxLookup(io.alu1Sel, 0.U, Array(
    s"b$A1_PC".U -> io.pc,
    s"b$A1_RS1".U -> io.rs1Data,
    // A1_Zero: Default, 0.U
  ))
  alu.io.op2 := MuxLookup(io.alu2Sel, 0.U, Array(
    s"b$A2_IMM".U -> io.imm,
    s"b$A2_RS2".U -> io.rs2Data,
    s"b$A2_SIZE".U -> 4.U
  ))
  io.rdData := alu.io.results
}
