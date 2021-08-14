package nucpu

import chisel3._
import chisel3.util._
import DecodeParams._

class EXEStage()(implicit val p: Configs) extends Module {
  val io: EXEStageIOs = IO(new EXEStageIOs())
  override val desiredName = "exe_stage"
  protected val alu: ALU = Module(new ALU())
  protected val isSRA: Bool = io.aluFn === ("b" + FN_SRA).U
  protected val isSR: Bool = io.aluFn === ("b" + FN_SR).U
  // True then only take the lsb 32bit of rs1 data
  protected val rs1Only32: Bool = !io.aluDW && (isSR || isSRA)
  // When the results will be signed extended, keep the sign bit. Otherwise, chuck with zeros.
  protected val rs1Fill: UInt = Mux(isSRA,
    Cat(Fill(p.busWidth - 32, io.rs1Data(31))),
    0.U((p.busWidth - 32).W)
  )
  protected val rs1DataWire: UInt = Mux(rs1Only32, Cat(rs1Fill, io.rs1Data(31, 0)), io.rs1Data
  )
  // True then only take the lsb 5 bit of rs2 data
  protected val rs2Only5: Bool = !io.aluDW && (isSR || (io.aluFn === ("b" + FN_SL).U) || isSRA)
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
