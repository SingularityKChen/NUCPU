package nucpu

import chisel3._
import DecodeParams._

class InstCtrlIOs extends Bundle {
  val legal: Bool = Bool()
  val fp: Bool = Bool()
  val rocc: Bool = Bool()
  val branch: Bool = Bool()
  val jal: Bool = Bool()
  val jalr: Bool = Bool()
  val rxs2: Bool = Bool()
  val rxs1: Bool = Bool()
  val scie: Bool = Bool()
  val sel_alu2: UInt = Bits(width = A2_X.length.W)
  val sel_alu1: UInt = Bits(width = A1_X.length.W)
  val sel_imm: UInt = Bits(width = IMM_X.length.W)
  val alu_dw: Bool = Bool()
  val alu_fn: UInt = Bits(width = FN_X.length.W)
  val mem: Bool = Bool()
  val mem_cmd: UInt = Bits(width = M_X.length.W)
  val rfs1: Bool = Bool()
  val rfs2: Bool = Bool()
  val rfs3: Bool = Bool()
  val wfd: Bool = Bool()
  val mul: Bool = Bool()
  val div: Bool = Bool()
  val wxd: Bool = Bool()
  val csr: UInt = Bits(width = CSR_X.length.W)
  val fence_i: Bool = Bool()
  val fence: Bool = Bool()
  val amo: Bool = Bool()
  val dp: Bool = Bool()
}