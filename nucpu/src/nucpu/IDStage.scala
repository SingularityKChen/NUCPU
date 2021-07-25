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
  protected val instDecoder: UInt = decoder(minimizer = EspressoMinimizer, input = io.inst,
    truthTable = TruthTable(Map(
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
    ).map({case (k, v) => k -> BitPat(s"b${v.reduce(_ + _)}")}),
      //           jal                                                          renf1               fence.i
      //   val     | jalr                                                       | renf2             |
      //   | fp_val| | renx2                                                    | | renf3           |
      //   | | rocc| | | renx1       s_alu1                          mem_val    | | | wfd           |
      //   | | | br| | | |   s_alu2  |       imm    dw     alu       | mem_cmd  | | | | mul         |
      //   | | | | | | | |   |       |       |      |      |         | |        | | | | | div       | fence
      //   | | | | | | | |   |       |       |      |      |         | |        | | | | | | wxd     | | amo
      //   | | | | | | | | scie      |       |      |      |         | |        | | | | | | |       | | | dp
      BitPat(s"b${
        Seq(N,X,X,X,X,X,X,X,X,A2_X, A1_X,  IMM_X, DW_X,  FN_X,      N,M_X,      X,X,X,X,X,X,X,CSR_X,X,X,X,X)
          .reduce(_ + _)}")
    ))
  protected val instCtrlWires: InstCtrlIOs = Wire(new InstCtrlIOs)
  instCtrlWires <> instDecoder
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
