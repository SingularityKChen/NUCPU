package nucpu

import chisel3._
import chisel3.util._

class IDStage()(implicit val p: Configs) extends Module {
  val io = IO(new Bundle {
    val inst: UInt = Input(UInt(p.instWidth.W))
    val rs1_data: UInt = Input(UInt(p.busWidth.W))
    val rs2_data: UInt = Input(UInt(p.busWidth.W))
    val rs1_r_ena: Bool = Output(Bool())
    val rs2_r_ena: Bool = Output(Bool())
    val rs1_r_addr: UInt = Output(UInt(p.addrWidth.W))
    val rs2_r_addr: UInt = Output(UInt(p.addrWidth.W))
    val rd_w_ena: Bool = Output(Bool())
    val rd_w_addr: UInt = Output(UInt(p.addrWidth.W))
    val inst_type: UInt = Output(UInt(p.typeWidth.W))
    val inst_opcode: UInt = Output(UInt(p.opcodeWidth.W))
    val op1: UInt = Output(UInt(p.busWidth.W))
    val op2: UInt = Output(UInt(p.busWidth.W))
  })
  override val desiredName = "id_stage"
  // I-type
  protected val opcode: UInt = Wire(io.inst(p.iTypeOpWidth - 1, 0))
  protected val rd: UInt = Wire(io.inst(p.iTypeOpWidth + p.rdWidth - 1, p.iTypeOpWidth))
  protected val func3: UInt = Wire(io.inst(p.iTypeOpWidth + p.rdWidth + p.func3Width - 1, p.iTypeOpWidth + p.rdWidth))
  protected val rs1: UInt = Wire(io.inst(p.instWidth - p.immWidth -1, p.instWidth - p.immWidth - p.rsWidth))
  protected val imm: UInt = Wire(io.inst(p.instWidth - 1, p.instWidth - p.immWidth))
  protected val inst_addi: Bool = Wire(!opcode(2) & !opcode(3) & opcode(4) & !opcode(5) & !opcode(6)
    & !func3(0) & !func3(1) & !func3(2))

  // arith inst: 10000; logic: 01000;
  // load-store: 00100; j: 00010;  sys: 000001
  io.inst_type(4) := Mux(this.reset.asBool(), 0.U, inst_addi)
  io.inst_opcode(0) := Mux(this.reset.asBool(), 0.U, inst_addi)
  io.inst_opcode(1) := Mux(this.reset.asBool(), 0.U, 0.U)
  io.inst_opcode(2) := Mux(this.reset.asBool(), 0.U, 0.U)
  io.inst_opcode(3) := Mux(this.reset.asBool(), 0.U, 0.U)
  io.inst_opcode(4) := Mux(this.reset.asBool(), 0.U, inst_addi)
  io.inst_opcode(5) := Mux(this.reset.asBool(), 0.U, 0.U)
  io.inst_opcode(6) := Mux(this.reset.asBool(), 0.U, 0.U)
  io.inst_opcode(7) := Mux(this.reset.asBool(), 0.U, 0.U)

  io.rs1_r_ena := Mux(this.reset.asBool(), false.B, io.inst_type(4))
  io.rs2_r_ena := false.B
  io.rs1_r_addr := Mux(this.reset.asBool(), 0.U, Mux(io.inst_type(4) === 1.U, rs1, 0.U))
  io.rs2_r_addr := 0.U

  io.rd_w_ena := Mux(this.reset.asBool(), false.B, io.inst_type(4))
  io.rd_w_addr := Mux(this.reset.asBool(), 0.U, Mux(io.inst_type(4) === 1.U, rd, 0.U))

  io.op1 := Mux(this.reset.asBool(), 0.U, Mux(io.inst_type(4) === 1.U, io.rs1_data, 0.U))
  io.op2 := Mux(this.reset.asBool(), 0.U, Mux(io.inst_type(4) === 1.U, Cat(Cat(Seq.fill(52)(imm(11))), imm), 0.U ))
}
