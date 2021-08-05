package nucpu

import chisel3._
import DecodeParams._

class IFStageIOs()(implicit val p: Configs) extends Bundle {
  val nextPC: UInt = Input(UInt(p.busWidth.W))
  val jumpPC: Bool = Input(Bool())
  val curPC: UInt = Output(UInt(p.busWidth.W))
  val instEn: Bool = Output(Bool())
}

class RegFileIOs()(implicit val p: Configs) extends Bundle {
  val wAddr: UInt = Input(UInt(p.addrWidth.W))
  val wData: UInt = Input(UInt(p.busWidth.W))
  val wEn: Bool = Input(Bool())
  val rs1RAddr: UInt = Input(UInt(p.addrWidth.W))
  val rs1RData: UInt = Output(UInt(p.busWidth.W))
  val rs1REn: Bool = Input(Bool())
  val rs2RAddr: UInt = Input(UInt(p.addrWidth.W))
  val rs2RData: UInt = Output(UInt(p.busWidth.W))
  val rs2REn: Bool = Input(Bool())
}

class IDStageIOs()(implicit val p: Configs) extends Bundle {
  val inst: UInt = Input(UInt(p.instW.W))
  val curPC: UInt = Input(UInt(p.busWidth.W))
  val rs1REn: Bool = Output(Bool())
  val rs2REn: Bool = Output(Bool())
  val rs1RAddr: UInt = Output(UInt(p.addrWidth.W))
  val rs2RAddr: UInt = Output(UInt(p.addrWidth.W))
  val rdWEn: Bool = Output(Bool())
  val rdWAddr: UInt = Output(UInt(p.addrWidth.W))
  val immData: UInt = Output(UInt(p.busWidth.W))
  val aluFn: UInt = Output(UInt(FN_X.length.W))
  val alu1Sel: UInt = Output(UInt(width = A1_X.length.W))
  val alu2Sel: UInt = Output(UInt(width = A2_X.length.W))
  val jal: Bool = Output(Bool())
  val jalr: Bool = Output(Bool())
  val br: Bool = Output(Bool())
  /** The targeted PC address if branch is taken or jal*/
  val jumpPCVal: UInt = Output(UInt(p.busWidth.W))
  val mem: Bool = Output(Bool())
  val memCmd: UInt = Output(UInt(M_X.length.W))
  val func3: UInt = Output(UInt(3.W))
  // False for taking only 32 bit and signed extend
  val aluDW: Bool = Output(Bool())
}

class EXEStageIOs()(implicit val p: Configs) extends Bundle {
  val aluFn: UInt = Input(UInt(FN_X.length.W))
  val aluDW: Bool = Input(Bool())
  val pc: UInt = Input(UInt(p.busWidth.W))
  val imm: UInt = Input(UInt(p.busWidth.W))
  val rs1Data: UInt = Input(UInt(p.busWidth.W))
  val rs2Data: UInt = Input(UInt(p.busWidth.W))
  val alu1Sel: UInt = Input(UInt(width = A1_X.length.W))
  val alu2Sel: UInt = Input(UInt(width = A2_X.length.W))
  val rdData: UInt = Output(UInt(p.busWidth.W))
}

class InstCtrlIOs extends Bundle {
  val legal: Bool = Bool()
  val fp: Bool = Bool()
  val rocc: Bool = Bool()
  val branch: Bool = Bool()
  val jal: Bool = Bool()
  val jalr: Bool = Bool()
  /** Read enable RS2 */
  val rxs2: Bool = Bool()
  /** Read enable RS1 */
  val rxs1: Bool = Bool()
  val scie: Bool = Bool()
  /** Select signal for op2 in ALU module */
  val alu2Sel: UInt = Bits(width = A2_X.length.W)
  /** Select signal for op1 in ALU module */
  val alu1Sel: UInt = Bits(width = A1_X.length.W)
  /** Select signal for imm value*/
  val immSel: UInt = Bits(width = IMM_X.length.W)
  /** Whether valid data width in ALU is 64 bit. False for only 32 bit*/
  val aluDW: Bool = Bool()
  /** ALU function code */
  val aluFn: UInt = Bits(width = FN_X.length.W)
  val mem: Bool = Bool()
  val mem_cmd: UInt = Bits(width = M_X.length.W)
  val rfs1: Bool = Bool()
  val rfs2: Bool = Bool()
  val rfs3: Bool = Bool()
  val wfd: Bool = Bool()
  val mul: Bool = Bool()
  val div: Bool = Bool()
  /** write enable */
  val wxd: Bool = Bool()
  val csr: UInt = Bits(width = CSR_X.length.W)
  val fenceI: Bool = Bool()
  val fence: Bool = Bool()
  val amo: Bool = Bool()
  val dp: Bool = Bool()
  def connectDecoder(decoderOutput: UInt): Unit = {
    val ios = Seq(legal, fp, rocc, branch, jal, jalr, rxs2, rxs1, scie, alu2Sel,
      alu1Sel, immSel, aluDW, aluFn, mem, mem_cmd,
      rfs1, rfs2, rfs3, wfd, mul, div, wxd, csr, fenceI, fence, amo, dp)
    var curBit = 0
    for (io <- ios.reverse) {
      val curIOBit = io.getWidth
      if (curIOBit == 1) {
        io := decoderOutput(curBit)
      } else {
        io := decoderOutput(curBit + curIOBit -1, curBit)
      }
      curBit += curIOBit
    }
  }
}

class NUCPUIOs()(implicit val p: Configs) extends Bundle {
  val inst: UInt = Input(UInt(p.instW.W))
  val instAddr: UInt = Output(UInt(p.busWidth.W))
  val instValid: Bool = Output(Bool())
  val memAddr: UInt = Output(UInt(p.busWidth.W))
  val memRData: UInt = Input(UInt(p.busWidth.W))
  val memDoWrite: Bool = Output(Bool())
  val memWData: UInt = Output(UInt(p.busWidth.W))
  val memMask: UInt = Output(UInt(p.busWidth.W))
  val memValid: Bool = Output(Bool())
}