package nucpu

import chisel3._
import chisel3.util._
import DecodeParams._

class EXEStage()(implicit val p: Configs) extends Module {
  val io = IO(new Bundle {
    val alu_fn: UInt = Input(UInt(FN_X.length.W))
    val pc: UInt = Input(UInt(p.busWidth.W))
    val imm: UInt = Input(UInt(p.busWidth.W))
    val rs1_data: UInt = Input(UInt(p.busWidth.W))
    val rs2_data: UInt = Input(UInt(p.busWidth.W))
    val sel_alu1: UInt = Input(UInt(width = A1_X.length.W))
    val sel_alu2: UInt = Input(UInt(width = A2_X.length.W))
    val rd_data: UInt = Output(UInt(p.busWidth.W))
  })
  override val desiredName = "exe_stage"
  protected val alu: ALU = Module(new ALU())
  alu.io.func := io.alu_fn
  alu.io.op1 := MuxLookup(io.sel_alu1, 0.U, Array(
    ("b" + A1_PC).U -> io.pc,
    ("b" + A1_RS1).U -> io.rs1_data
  ))
  // FIXME: A2_SIZE
  alu.io.op2 := MuxLookup(io.sel_alu2, 0.U, Array(
    ("b" + A2_IMM).U -> io.imm,
    ("b" + A2_RS2).U -> io.rs2_data
  ))
  protected val rdDataReg: UInt = RegInit(0.U(p.busWidth.W))
  rdDataReg := alu.io.results
  io.rd_data := rdDataReg
}
