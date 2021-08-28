package nucpu

import chisel3._
import chisel3.util._
import DecodeParams._

class IFStageIOs()(implicit val p: Configs) extends Bundle {
  val nextPC: UInt = Input(UInt(p.busWidth.W))
  val jumpPC: Bool = Input(Bool())
  val curPC: UInt = Output(UInt(p.busWidth.W))
  val instEn: Bool = Output(Bool())
  val curPCAdd4: UInt = Output(UInt(p.busWidth.W))
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
  val trapCode: Option[UInt] = if (p.diffTest) Some(Output(UInt(3.W))) else None
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
  // CSR
  val csrCMD: UInt = Output(UInt(CSR_X.length.W))
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

class MemStageIOs(implicit val p: Configs) extends Bundle {
  val exeAddr: UInt = Input(UInt(p.busWidth.W))
  val exeData: UInt = Input(UInt(p.busWidth.W))
  val func3: UInt = Input(UInt(3.W))
  val memCmd: UInt = Input(UInt(M_X.length.W))
  val exeMemValid: Bool = Input(Bool())
  // To read memory
  val memAddr: UInt = Output(UInt(p.busWidth.W))
  val memRData: UInt = Input(UInt(p.busWidth.W))
  val memDoWrite: Bool = Output(Bool())
  val memWData: UInt = Output(UInt(p.busWidth.W))
  val memMask: UInt = Output(UInt(p.busWidth.W))
  val memValid: Bool = Output(Bool())
  // To Write Back
  val wbData: UInt = Output(UInt(p.busWidth.W))
}

class InterruptIOs extends Bundle {
  val mtip: Bool = Bool()
  val msip: Bool = Bool()
  val meip: Bool = Bool()
}

class MIPRegIOs extends Bundle {
  val wpri7: UInt = UInt(52.W)
  val meip: Bool = Bool()
  val wpri6: Bool = Bool()
  val seip: Bool = Bool()
  val wpri5: Bool = Bool()
  val mtip: Bool = Bool()
  val wpri4: Bool = Bool()
  val stip: Bool = Bool()
  val wpri3: Bool = Bool()
  val msip: Bool = Bool()
  val wpri2: Bool = Bool()
  val ssip: Bool = Bool()
  val wpri1: Bool = Bool()
  private val mipRegValues = Seq(wpri7, meip, wpri6, seip, wpri5,
    mtip, wpri4, stip, wpri3, msip, wpri2, ssip, wpri1)
  def mipRegRead: UInt = {
    Cat(0.U(52.W), meip, false.B, seip, false.B, mtip, false.B, stip, false.B, msip, false.B, ssip, false.B)
  }
  def mipRegWrite(wData: UInt): Unit = {
    var curBit = 0
    for (mipVal <- mipRegValues.reverse) {
      val curValBit = mipVal.getWidth
      if (curValBit == 1) {
        mipVal := wData(curBit)
      } else {
        mipVal := wData(curBit + curValBit -1, curBit)
      }
      curBit += curValBit
    }
  }
}

/** machine status related*/
class MStatusRegIOs extends Bundle {
  val sd: Bool = Bool()
  val wpri5: UInt = UInt(27.W)
  val sxl: UInt = UInt(2.W)
  val uxl: UInt = UInt(2.W)
  val wpri4: UInt = UInt(9.W)
  val tsr: Bool = Bool()
  val tw: Bool = Bool()
  val tvm: Bool = Bool()
  val mxr: Bool = Bool()
  val sum: Bool = Bool()
  val mprv: Bool = Bool()
  val xs: UInt = UInt(2.W)
  val fs: UInt = UInt(2.W)
  val mpp: UInt = UInt(2.W)
  val wpri3: UInt = UInt(2.W)
  val spp: Bool = Bool()
  val mpie: Bool = Bool()
  val wpri2: Bool = Bool()
  val spie: Bool = Bool()
  val upie: Bool = Bool()
  val mie: Bool = Bool()
  val wpri1: Bool = Bool()
  val sie: Bool = Bool()
  val uie: Bool = Bool()
  private val mStatusValues = Seq(sd, wpri5, sxl, uxl, wpri4, tsr, tw, tvm, mxr, sum, mprv,
    xs, fs, mpp, wpri3, spp, mpie, wpri2, spie, upie, mie, wpri1, sie, uie)
  def mStatusRegRead: UInt = {
    Cat(mStatusValues)
  }
  def mStatusRegWrite(wData: UInt): Unit = {
    var curBit = 0
    for (mStatusVal <- mStatusValues.reverse) {
      val curValBit = mStatusVal.getWidth
      if (curValBit == 1) {
        mStatusVal := wData(curBit)
      } else {
        mStatusVal := wData(curBit + curValBit -1, curBit)
      }
      curBit += curValBit
    }
  }
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