package nucpu

import chisel3._
import chisel3.util._

class CLint(implicit val p: Configs) extends Module {
  val io = IO(new Bundle {
    val mtip: Bool = Output(Bool())
    val msip: Bool = Output(Bool())
    val addr: UInt = Input(UInt(p.busWidth.W))
    val readData: UInt = Output(UInt(p.busWidth.W))
    val wEn: Bool = Input(Bool())
    val writeData: UInt = Input(UInt(p.busWidth.W))
  })
  protected val mtime: UInt = RegInit(0.U(p.busWidth.W))
  protected val mtimecmp: UInt = RegInit(0.U(p.busWidth.W))
  protected val msip: UInt = RegInit(0.U(p.busWidth.W))
  protected val (tickCnt, cntWrap) = Counter(this.clock.asBool(), p.tickCnt)
  msip := Mux((io.addr === p.msipOffset) && io.wEn, io.writeData, msip)
  mtime := Mux((io.addr === p.mtimeOffset) && io.wEn, io.writeData, Mux(cntWrap, mtime + 1.U, mtime))
  mtimecmp := Mux((io.addr === p.mtimecmpOffset) && io.wEn, io.writeData, mtimecmp)
  io.readData := MuxLookup(io.addr, 0.U, Array(
    p.msipOffset -> msip,
    p.mtimeOffset -> mtime,
    p.mtimecmpOffset -> mtimecmp,
  ))
  io.mtip := RegNext(mtime >= mtimecmp)
  io.msip := RegNext(msip =/= 0.U)
}
