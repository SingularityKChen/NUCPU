package nucpu

import chisel3._
import chisel3.util._

class Configs(val diffTest: Boolean) {
  val busWidth = 64
  // for io
  val addrWidth = 5
  val typeWidth = 5
  val opcodeWidth = 8
  // for instruction
  val instW = 32
  val instOpW = 7
  val instRdW = 5
  val instRsW = 5
  val instFunc3W = 3
  val instImmW = 12
  // regfile
  val regNum = 32
}

class RVI {
  val addi = 17
}

object DecodeParams {
  val A1_X = "??"
  val A1_ZERO = "00"
  val A1_RS1 = "01"
  val A1_PC = "10"

  val IMM_X = "???"
  val IMM_S = "000"
  val IMM_SB = "001"
  val IMM_U = "010"
  val IMM_UJ = "011"
  val IMM_I = "100"
  val IMM_Z = "101"

  val A2_X = "??"
  val A2_ZERO = "00"
  val A2_SIZE = "01"
  val A2_RS2 = "10"
  val A2_IMM = "11"

  val X = "?"
  val N = "0"
  val Y = "1"

  val DW_X: String = X
  val DW_32: String = N
  val DW_64: String = Y
  val DW_XPR: String = Y

  val M_X = "?????"
  val M_XRD = "00000"
  val M_XWR = "00001"

  val CSR_X = "???"
  val CSR_N = "000"
  val CSR_I = "100"
  val CSR_W = "101"
  val CSR_S = "110"
  val CSR_C = "111"

  val FN_X = "????"
  val FN_ADD = "0000"
  val FN_SL = "0001"
  val FN_SEQ = "0010"
  val FN_SNE = "0011"
  val FN_XOR = "0100"
  val FN_SR = "0101"
  val FN_OR = "0110"
  val FN_AND = "0111"
  val FN_SUB = "1010"
  val FN_SRA = "1011"
  val FN_SLT = "1100"
  val FN_SGE = "1101"
  val FN_SLTU = "1110"
  val FN_SGEU = "1111"
}
