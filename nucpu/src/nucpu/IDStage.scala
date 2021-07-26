package nucpu

import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._
import Instructions._
import DecodeParams._

class IDStage()(implicit val p: Configs) extends Module {
  val io = IO(new Bundle {
    val inst: UInt = Input(UInt(p.instW.W))
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
  protected val opcode: UInt = io.inst(p.instOpW - 1, 0)
  protected val rd: UInt = io.inst(p.instOpW + p.instRdW - 1, p.instOpW)
  protected val func3: UInt = io.inst(p.instOpW + p.instRdW + p.instFunc3W - 1, p.instOpW + p.instRdW)
  protected val rs1: UInt = io.inst(p.instW - p.instImmW -1, p.instW - p.instImmW - p.instRsW)
  protected val imm: UInt = io.inst(p.instW - 1, p.instW - p.instImmW)
  protected val inst_addi: Bool = !opcode(2) & !opcode(3) & opcode(4) & !opcode(5) & !opcode(6) &
    !func3(0) & !func3(1) & !func3(2)
  protected val instDecoder: UInt = decoder(minimizer = QMCMinimizer, input = io.inst, truthTable = rv64ITruthTable)
  protected val instCtrlWires: InstCtrlIOs = Wire(new InstCtrlIOs)
  instCtrlWires.connectDecoder(instDecoder)
  // arith inst: 10000; logic: 01000;
  // load-store: 00100; j: 00010;  sys: 000001
  protected val inst_type_bool: Vec[Bool] = VecInit(0.U(p.typeWidth.W).asBools())
  protected val inst_opcode_bool: Vec[Bool] = VecInit(0.U(p.opcodeWidth.W).asBools())
  inst_type_bool(4) := Mux(this.reset.asBool(), 0.U, inst_addi)
  inst_opcode_bool(0) := Mux(this.reset.asBool(), 0.U, inst_addi)
  inst_opcode_bool(1) := Mux(this.reset.asBool(), 0.U, 0.U)
  inst_opcode_bool(2) := Mux(this.reset.asBool(), 0.U, 0.U)
  inst_opcode_bool(3) := Mux(this.reset.asBool(), 0.U, 0.U)
  inst_opcode_bool(4) := Mux(this.reset.asBool(), 0.U, inst_addi)
  inst_opcode_bool(5) := Mux(this.reset.asBool(), 0.U, 0.U)
  inst_opcode_bool(6) := Mux(this.reset.asBool(), 0.U, 0.U)
  inst_opcode_bool(7) := Mux(this.reset.asBool(), 0.U, 0.U)
  io.inst_type := inst_type_bool.asUInt()
  io.inst_opcode := inst_opcode_bool.asUInt()

  io.rs1_r_ena := Mux(this.reset.asBool(), false.B, io.inst_type(4))
  io.rs2_r_ena := false.B
  io.rs1_r_addr := Mux(this.reset.asBool(), 0.U, Mux(io.inst_type(4), rs1, 0.U))
  io.rs2_r_addr := 0.U

  io.rd_w_ena := Mux(this.reset.asBool(), false.B, io.inst_type(4))
  io.rd_w_addr := Mux(this.reset.asBool(), 0.U, Mux(io.inst_type(4), rd, 0.U))

  io.op1 := Mux(this.reset.asBool(), 0.U, Mux(io.inst_type(4), io.rs1_data, 0.U))
  io.op2 := Mux(this.reset.asBool(), 0.U, Mux(io.inst_type(4), Cat(Fill(52, imm(11)), imm), 0.U ))

}
