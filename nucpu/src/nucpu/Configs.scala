package nucpu

import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode.TruthTable
import Instructions._

class Configs(val diffTest: Boolean) {
  val busWidth = 64
  // for io
  val addrWidth = 5
  // for instruction
  val instW = 32
  val instRdW = 5
  val instRsW = 5
  val instImmW = 12
  // regfile
  val regNum = 32
  // pc init value
  val pcStart = "h80000000"
  val instTrap = "h0000006f"
  val instPutch = "h0000007f"
}

class CacheConfigs(diffTest: Boolean) extends Configs(diffTest) {
  val way = 4
  /** The byte in one cache line*/
  val lineByte = 16
  /** The number of memory cells in one cache way*/
  val set = 32
  val offsetBit: Int = log2Ceil(lineByte)
  val setBit: Int = log2Ceil(set)
  val tagBit: Int = busWidth - setBit - offsetBit
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
  /** shift left */
  val FN_SL = "0001"
  val FN_SEQ = "0010"
  val FN_SNE = "0011"
  val FN_XOR = "0100"
  /** shift right logical */
  val FN_SR = "0101"
  val FN_OR = "0110"
  val FN_AND = "0111"
  val FN_SUB = "1010"
  /** shift right arithmetic */
  val FN_SRA = "1011"
  /** set if less than, signed */
  val FN_SLT = "1100"
  /** set if greater or equals to, signed */
  val FN_SGE = "1101"
  /** set if less than, unsigned */
  val FN_SLTU = "1110"
  /** set if greater or equals to, unsigned */
  val FN_SGEU = "1111"
  val defaultDec: String = Seq(
    //           jal                                                          renf1               fence.i
    //   val     | jalr                                                       | renf2             |
    //   | fp_val| | renx2                                                    | | renf3           |
    //   | | rocc| | | renx1       s_alu1                          mem_val    | | | wfd           |
    //   | | | br| | | |   s_alu2  |       imm    dw     alu       | mem_cmd  | | | | mul         |
    //   | | | | | | | |   |       |       |      |      |         | |        | | | | | div       | fence
    //   | | | | | | | |   |       |       |      |      |         | |        | | | | | | wxd     | | amo
    //   | | | | | | | | scie      |       |      |      |         | |        | | | | | | |       | | | dp
    N,   X,X,X,X,X,X,X,X,A2_X,   A1_X,   IMM_X, DW_X,  FN_X,      N,M_X,      X,X,X,X,X,X,X,CSR_X,X,X,X,X
  ).reduce(_ + _)
  val outWidth: Int = defaultDec.length
  val rv64ITruthTable: TruthTable =  TruthTable(Map(
    BNE->   Seq(Y,N,N,Y,N,N,Y,Y,N,A2_RS2, A1_RS1, IMM_SB,DW_X,  FN_SNE, N,M_X,   N,N,N,N,N,N,N,CSR_N,N,N,N,N),
    BEQ->   Seq(Y,N,N,Y,N,N,Y,Y,N,A2_RS2, A1_RS1, IMM_SB,DW_X,  FN_SEQ, N,M_X,   N,N,N,N,N,N,N,CSR_N,N,N,N,N),
    BLT->   Seq(Y,N,N,Y,N,N,Y,Y,N,A2_RS2, A1_RS1, IMM_SB,DW_X,  FN_SLT, N,M_X,   N,N,N,N,N,N,N,CSR_N,N,N,N,N),
    BLTU->  Seq(Y,N,N,Y,N,N,Y,Y,N,A2_RS2, A1_RS1, IMM_SB,DW_X,  FN_SLTU,N,M_X,   N,N,N,N,N,N,N,CSR_N,N,N,N,N),
    BGE->   Seq(Y,N,N,Y,N,N,Y,Y,N,A2_RS2, A1_RS1, IMM_SB,DW_X,  FN_SGE, N,M_X,   N,N,N,N,N,N,N,CSR_N,N,N,N,N),
    BGEU->  Seq(Y,N,N,Y,N,N,Y,Y,N,A2_RS2, A1_RS1, IMM_SB,DW_X,  FN_SGEU,N,M_X,   N,N,N,N,N,N,N,CSR_N,N,N,N,N),
    JAL->   Seq(Y,N,N,N,Y,N,N,N,N,A2_SIZE,A1_PC,  IMM_UJ,DW_XPR,FN_ADD, N,M_X,   N,N,N,N,N,N,Y,CSR_N,N,N,N,N),
    JALR->  Seq(Y,N,N,N,N,Y,N,Y,N,A2_IMM, A1_RS1, IMM_I, DW_XPR,FN_ADD, N,M_X,   N,N,N,N,N,N,Y,CSR_N,N,N,N,N),
    AUIPC-> Seq(Y,N,N,N,N,N,N,N,N,A2_IMM, A1_PC,  IMM_U, DW_XPR,FN_ADD, N,M_X,   N,N,N,N,N,N,Y,CSR_N,N,N,N,N),
    LB->    Seq(Y,N,N,N,N,N,N,Y,N,A2_IMM, A1_RS1, IMM_I, DW_XPR,FN_ADD, Y,M_XRD, N,N,N,N,N,N,Y,CSR_N,N,N,N,N),
    LH->    Seq(Y,N,N,N,N,N,N,Y,N,A2_IMM, A1_RS1, IMM_I, DW_XPR,FN_ADD, Y,M_XRD, N,N,N,N,N,N,Y,CSR_N,N,N,N,N),
    LW->    Seq(Y,N,N,N,N,N,N,Y,N,A2_IMM, A1_RS1, IMM_I, DW_XPR,FN_ADD, Y,M_XRD, N,N,N,N,N,N,Y,CSR_N,N,N,N,N),
    LBU->   Seq(Y,N,N,N,N,N,N,Y,N,A2_IMM, A1_RS1, IMM_I, DW_XPR,FN_ADD, Y,M_XRD, N,N,N,N,N,N,Y,CSR_N,N,N,N,N),
    LHU->   Seq(Y,N,N,N,N,N,N,Y,N,A2_IMM, A1_RS1, IMM_I, DW_XPR,FN_ADD, Y,M_XRD, N,N,N,N,N,N,Y,CSR_N,N,N,N,N),
    SB->    Seq(Y,N,N,N,N,N,Y,Y,N,A2_IMM, A1_RS1, IMM_S, DW_XPR,FN_ADD, Y,M_XWR, N,N,N,N,N,N,N,CSR_N,N,N,N,N),
    SH->    Seq(Y,N,N,N,N,N,Y,Y,N,A2_IMM, A1_RS1, IMM_S, DW_XPR,FN_ADD, Y,M_XWR, N,N,N,N,N,N,N,CSR_N,N,N,N,N),
    SW->    Seq(Y,N,N,N,N,N,Y,Y,N,A2_IMM, A1_RS1, IMM_S, DW_XPR,FN_ADD, Y,M_XWR, N,N,N,N,N,N,N,CSR_N,N,N,N,N),
    LUI->   Seq(Y,N,N,N,N,N,N,N,N,A2_IMM, A1_ZERO,IMM_U, DW_XPR,FN_ADD, N,M_X,   N,N,N,N,N,N,Y,CSR_N,N,N,N,N),
    ADDI->  Seq(Y,N,N,N,N,N,N,Y,N,A2_IMM, A1_RS1, IMM_I, DW_XPR,FN_ADD, N,M_X,   N,N,N,N,N,N,Y,CSR_N,N,N,N,N),
    SLTI->  Seq(Y,N,N,N,N,N,N,Y,N,A2_IMM, A1_RS1, IMM_I, DW_XPR,FN_SLT, N,M_X,   N,N,N,N,N,N,Y,CSR_N,N,N,N,N),
    SLTIU-> Seq(Y,N,N,N,N,N,N,Y,N,A2_IMM, A1_RS1, IMM_I, DW_XPR,FN_SLTU,N,M_X,   N,N,N,N,N,N,Y,CSR_N,N,N,N,N),
    ANDI->  Seq(Y,N,N,N,N,N,N,Y,N,A2_IMM, A1_RS1, IMM_I, DW_XPR,FN_AND, N,M_X,   N,N,N,N,N,N,Y,CSR_N,N,N,N,N),
    ORI->   Seq(Y,N,N,N,N,N,N,Y,N,A2_IMM, A1_RS1, IMM_I, DW_XPR,FN_OR,  N,M_X,   N,N,N,N,N,N,Y,CSR_N,N,N,N,N),
    XORI->  Seq(Y,N,N,N,N,N,N,Y,N,A2_IMM, A1_RS1, IMM_I, DW_XPR,FN_XOR, N,M_X,   N,N,N,N,N,N,Y,CSR_N,N,N,N,N),
    ADD->   Seq(Y,N,N,N,N,N,Y,Y,N,A2_RS2, A1_RS1, IMM_X, DW_XPR,FN_ADD, N,M_X,   N,N,N,N,N,N,Y,CSR_N,N,N,N,N),
    SUB->   Seq(Y,N,N,N,N,N,Y,Y,N,A2_RS2, A1_RS1, IMM_X, DW_XPR,FN_SUB, N,M_X,   N,N,N,N,N,N,Y,CSR_N,N,N,N,N),
    SLT->   Seq(Y,N,N,N,N,N,Y,Y,N,A2_RS2, A1_RS1, IMM_X, DW_XPR,FN_SLT, N,M_X,   N,N,N,N,N,N,Y,CSR_N,N,N,N,N),
    SLTU->  Seq(Y,N,N,N,N,N,Y,Y,N,A2_RS2, A1_RS1, IMM_X, DW_XPR,FN_SLTU,N,M_X,   N,N,N,N,N,N,Y,CSR_N,N,N,N,N),
    AND->   Seq(Y,N,N,N,N,N,Y,Y,N,A2_RS2, A1_RS1, IMM_X, DW_XPR,FN_AND, N,M_X,   N,N,N,N,N,N,Y,CSR_N,N,N,N,N),
    OR->    Seq(Y,N,N,N,N,N,Y,Y,N,A2_RS2, A1_RS1, IMM_X, DW_XPR,FN_OR,  N,M_X,   N,N,N,N,N,N,Y,CSR_N,N,N,N,N),
    XOR->   Seq(Y,N,N,N,N,N,Y,Y,N,A2_RS2, A1_RS1, IMM_X, DW_XPR,FN_XOR, N,M_X,   N,N,N,N,N,N,Y,CSR_N,N,N,N,N),
    SLL->   Seq(Y,N,N,N,N,N,Y,Y,N,A2_RS2, A1_RS1, IMM_X, DW_XPR,FN_SL,  N,M_X,   N,N,N,N,N,N,Y,CSR_N,N,N,N,N),
    SRL->   Seq(Y,N,N,N,N,N,Y,Y,N,A2_RS2, A1_RS1, IMM_X, DW_XPR,FN_SR,  N,M_X,   N,N,N,N,N,N,Y,CSR_N,N,N,N,N),
    SRA->   Seq(Y,N,N,N,N,N,Y,Y,N,A2_RS2, A1_RS1, IMM_X, DW_XPR,FN_SRA, N,M_X,   N,N,N,N,N,N,Y,CSR_N,N,N,N,N),
    FENCE-> Seq(Y,N,N,N,N,N,N,N,N,A2_X,   A1_X,   IMM_X, DW_X,  FN_X,   N,M_X,   N,N,N,N,N,N,N,CSR_N,N,Y,N,N),
    // TODO: Check FENCE_I
    FENCE_I->Seq(Y,N,N,N,N,N,N,N,N,A2_X,  A1_X,   IMM_X, DW_X,  FN_X,   N,M_X,   N,N,N,N,N,N,N,CSR_N,Y,Y,N,N),
    ADDIW-> Seq(Y,N,N,N,N,N,N,Y,N,A2_IMM, A1_RS1, IMM_I, DW_32, FN_ADD, N,M_X,   N,N,N,N,N,N,Y,CSR_N,N,N,N,N),
    SLLIW-> Seq(Y,N,N,N,N,N,N,Y,N,A2_IMM, A1_RS1, IMM_I, DW_32, FN_SL,  N,M_X,   N,N,N,N,N,N,Y,CSR_N,N,N,N,N),
    SRLIW-> Seq(Y,N,N,N,N,N,N,Y,N,A2_IMM, A1_RS1, IMM_I, DW_32, FN_SR,  N,M_X,   N,N,N,N,N,N,Y,CSR_N,N,N,N,N),
    SRAIW-> Seq(Y,N,N,N,N,N,N,Y,N,A2_IMM, A1_RS1, IMM_I, DW_32, FN_SRA, N,M_X,   N,N,N,N,N,N,Y,CSR_N,N,N,N,N),
    ADDW->  Seq(Y,N,N,N,N,N,Y,Y,N,A2_RS2, A1_RS1, IMM_X, DW_32, FN_ADD, N,M_X,   N,N,N,N,N,N,Y,CSR_N,N,N,N,N),
    SUBW->  Seq(Y,N,N,N,N,N,Y,Y,N,A2_RS2, A1_RS1, IMM_X, DW_32, FN_SUB, N,M_X,   N,N,N,N,N,N,Y,CSR_N,N,N,N,N),
    SLLW->  Seq(Y,N,N,N,N,N,Y,Y,N,A2_RS2, A1_RS1, IMM_X, DW_32, FN_SL,  N,M_X,   N,N,N,N,N,N,Y,CSR_N,N,N,N,N),
    SRLW->  Seq(Y,N,N,N,N,N,Y,Y,N,A2_RS2, A1_RS1, IMM_X, DW_32, FN_SR,  N,M_X,   N,N,N,N,N,N,Y,CSR_N,N,N,N,N),
    SRAW->  Seq(Y,N,N,N,N,N,Y,Y,N,A2_RS2, A1_RS1, IMM_X, DW_32, FN_SRA, N,M_X,   N,N,N,N,N,N,Y,CSR_N,N,N,N,N),
    LD->    Seq(Y,N,N,N,N,N,N,Y,N,A2_IMM, A1_RS1, IMM_I, DW_XPR,FN_ADD, Y,M_XRD, N,N,N,N,N,N,Y,CSR_N,N,N,N,N),
    LWU->   Seq(Y,N,N,N,N,N,N,Y,N,A2_IMM, A1_RS1, IMM_I, DW_XPR,FN_ADD, Y,M_XRD, N,N,N,N,N,N,Y,CSR_N,N,N,N,N),
    SD->    Seq(Y,N,N,N,N,N,Y,Y,N,A2_IMM, A1_RS1, IMM_S, DW_XPR,FN_ADD, Y,M_XWR, N,N,N,N,N,N,N,CSR_N,N,N,N,N),
    SLLI->  Seq(Y,N,N,N,N,N,N,Y,N,A2_IMM, A1_RS1, IMM_I, DW_XPR,FN_SL,  N,M_X,   N,N,N,N,N,N,Y,CSR_N,N,N,N,N),
    SRLI->  Seq(Y,N,N,N,N,N,N,Y,N,A2_IMM, A1_RS1, IMM_I, DW_XPR,FN_SR,  N,M_X,   N,N,N,N,N,N,Y,CSR_N,N,N,N,N),
    SRAI->  Seq(Y,N,N,N,N,N,N,Y,N,A2_IMM, A1_RS1, IMM_I, DW_XPR,FN_SRA, N,M_X,   N,N,N,N,N,N,Y,CSR_N,N,N,N,N),
    BitPat("b00000000000000000000000001111111") ->
      Seq(Y,N,N,N,N,N,N,Y,N,A2_IMM,A1_RS1,IMM_I,DW_XPR,FN_ADD,N,M_X,N,N,N,N,N,N,Y,CSR_N,N,N,N,N),
  ).map({case (k, v) => k -> BitPat(s"b${v.reduce(_ + _)}")}), BitPat(s"b$defaultDec"))
}
