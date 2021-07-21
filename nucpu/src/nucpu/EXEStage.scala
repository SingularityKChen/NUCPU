package nucpu

import chisel3._

class EXEStage()(implicit val p: Configs) extends Module {
  val io = IO(new Bundle {
    val inst_type_i: UInt = Input(UInt(p.typeWidth.W))
    val inst_opcode: UInt = Input(UInt(p.opcodeWidth.W))
    val op1: UInt = Input(UInt(p.busWidth.W))
    val op2: UInt = Input(UInt(p.busWidth.W))
    val inst_type_o: UInt = Output(UInt(p.typeWidth.W))
    val rd_data: UInt = Output(UInt(p.busWidth.W))
  })
  private val isa = new RVI()
  override val desiredName = "exe_stage"
  protected val rdDataReg: UInt = RegInit(0.U(p.busWidth.W))
  // TODO: check the sign
  protected val immSum: SInt = io.op1.asSInt() + io.op2.asSInt()
  // TODO: change the exe logic with ALU
  rdDataReg := Mux(io.inst_opcode === isa.addi.U, immSum.asUInt(), 0.U)
  io.inst_type_o := io.inst_type_i
  io.rd_data := rdDataReg
}
