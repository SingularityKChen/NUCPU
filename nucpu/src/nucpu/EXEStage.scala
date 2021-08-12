package nucpu

import chisel3._
import chisel3.util._
import DecodeParams._

class EXEStage()(implicit val p: Configs) extends Module {
  val io: EXEStageIOs = IO(new EXEStageIOs())
  override val desiredName = "exe_stage"
  protected val alu: ALU = Module(new ALU())
  // True then only take the lsb 32bit of rs1 data
  protected val rs1Only32: Bool = !io.aluDW && ((io.aluFn === ("b" + FN_SR).U) || (io.aluFn === ("b" + FN_SRA).U))
  protected val rs1DataWire: UInt = Mux(rs1Only32,
    Cat(Fill(p.busWidth - 32, io.rs1Data(31)), io.rs1Data(31, 0)), io.rs1Data
  )
  // True then only take the lsb 5 bit of rs2 data
  protected val rs2Only5: Bool = !io.aluDW && ((io.aluFn === ("b" + FN_SL).U) || (io.aluFn === ("b" + FN_SRA).U))
  protected val rs2DataWire: UInt = Mux(rs2Only5,
    Cat(0.U((p.busWidth - 5).W), io.rs2Data(4, 0)), io.rs2Data
  )
  alu.io.func := io.aluFn
  alu.io.op1 := MuxLookup(io.alu1Sel, 0.U, Array(
    s"b$A1_PC".U -> io.pc,
    s"b$A1_RS1".U -> rs1DataWire,
    // A1_Zero: Default, 0.U
  ))
  alu.io.op2 := MuxLookup(io.alu2Sel, 0.U, Array(
    s"b$A2_IMM".U -> io.imm,
    s"b$A2_RS2".U -> rs2DataWire,
    s"b$A2_SIZE".U -> 4.U
  ))
  io.rdData := Mux(io.aluDW, alu.io.results, Cat(Fill(p.busWidth - 32, alu.io.results(31)), alu.io.results(31, 0)))
}
