package nucpu

import chisel3._

class NUCPU()(implicit val p: Configs) extends Module {
  val io = IO(new Bundle {
    val inst: UInt = Input(UInt(p.instWidth.W))
    val inst_addr: UInt = Output(UInt(p.busWidth.W))
    val inst_ena: Bool = Output(Bool())
  })
  protected val if_stage: IFStage = Module(new IFStage())
  protected val id_stage: IDStage = Module(new IDStage())
  protected val exe_stage: EXEStage = Module(new EXEStage())
  protected val regfile: RegFile = Module(new RegFile())
  // inst fetch
  io.inst_addr := if_stage.io.inst_adder
  io.inst_ena := if_stage.io.inst_ena
  // inst decode
  id_stage.io.inst := io.inst
  // id_stage -> regfile
  regfile.io.r_addr1 := id_stage.io.rs1_r_addr
  regfile.io.r_ena1 := id_stage.io.rs1_r_ena
  regfile.io.r_addr2 := id_stage.io.rs2_r_addr
  regfile.io.r_ena2 := id_stage.io.rs2_r_ena
  // regfile -> id_stage
  id_stage.io.rs1_data := regfile.io.r_data1
  id_stage.io.rs2_data := regfile.io.r_data2
  // exe
  // id_stage -> exe_stage
  exe_stage.io.inst_type_i := id_stage.io.inst_type
  exe_stage.io.inst_opcode := id_stage.io.inst_opcode
  exe_stage.io.op1 := id_stage.io.op1
  exe_stage.io.op2 := id_stage.io.op2
  // write back
  // id_stage -> regfile
  regfile.io.w_ena := id_stage.io.rd_w_ena
  regfile.io.w_addr := id_stage.io.rd_w_addr
  // exe_stage -> regfile
  regfile.io.w_data := exe_stage.io.rd_data
}
