package nucpu

import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._
import nucpu.DecodeParams._

class IDStage()(implicit val p: Configs) extends Module {
  val io: IDStageIOs = IO(new IDStageIOs())
  override val desiredName = "id_stage"
  protected val rd: UInt = io.inst(11, 7)
  protected val rs1: UInt = io.inst(19, 15)
  protected val rs2: UInt = io.inst(24, 20)
  protected val instDecoder: UInt = decoder(minimizer = EspressoMinimizer, input = io.inst, truthTable = rv64ITruthTable)
  protected val instCtrlWires: InstCtrlIOs = Wire(new InstCtrlIOs)
  instCtrlWires.connectDecoder(instDecoder)
  protected val immSign: UInt = io.inst(31).asUInt()
  protected val imm30_20: UInt = Mux(instCtrlWires.immSel === s"b$IMM_U".U, io.inst(30, 20), Cat(Fill(11, immSign)))
  protected val imm19_12: UInt = Mux(instCtrlWires.immSel === BitPat("b01?"), io.inst(19, 12), Cat(Fill(8, immSign)))
  // TODO: IMM_Z
  protected val imm11: UInt = MuxLookup(instCtrlWires.immSel, immSign, Array(
    s"b$IMM_UJ".U -> io.inst(20),
    s"b$IMM_SB".U -> io.inst(7),
    s"b$IMM_U".U -> 0.U,
  ))
  protected val imm10_5: UInt = Mux(instCtrlWires.immSel === s"b$IMM_U".U, 0.U, io.inst(30, 25))
  protected val imm4_1: UInt = Mux(instCtrlWires.immSel === BitPat("b00?"),
    io.inst(11, 8),
    Mux(instCtrlWires.immSel === s"b$IMM_U".U, 0.U, io.inst(24, 21))
  )
  protected val imm0: UInt = Mux(instCtrlWires.immSel === s"b$IMM_I".U,
    io.inst(20),
    Mux(instCtrlWires.immSel === s"b$IMM_S".U, io.inst(7), 0.U)
  )
  protected val imm: UInt = Cat(Fill(p.busWidth - 31, immSign), imm30_20, imm19_12, imm11, imm10_5, imm4_1, imm0)
  io.aluFn := instCtrlWires.aluFn
  io.alu1Sel := instCtrlWires.alu1Sel
  io.alu2Sel := instCtrlWires.alu2Sel
  io.jal := instCtrlWires.jal
  io.jalr := instCtrlWires.jalr
  io.br := instCtrlWires.branch
  io.jumpPCVal := io.curPC + imm
  io.rs1REn := instCtrlWires.rxs1
  io.rs2REn := instCtrlWires.rxs2
  io.rs1RAddr := rs1
  io.rs2RAddr := rs2
  io.rdWEn := instCtrlWires.wxd
  io.rdWAddr := rd
  io.immData := imm
  // Memory related
  io.mem := instCtrlWires.mem
  io.memCmd := instCtrlWires.mem_cmd
  io.func3 := io.inst(14, 12)
  io.aluDW := instCtrlWires.aluDW
}
