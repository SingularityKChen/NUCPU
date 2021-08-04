package nucpu

import chisel3._
import chisel3.util._
import DecodeParams._

class MemStage()(implicit val p: Configs) extends Module {
  val io = IO(new Bundle() {
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
  })
  protected val lwValidData: UInt = Mux(io.exeAddr(2), io.memRData(63, 32), io.memRData(31, 0))
  protected val lhValidData: UInt = Mux(io.exeAddr(1), lwValidData(31, 16), lwValidData(15, 0))
  protected val lbValidData: UInt = Mux(io.exeAddr(0), lhValidData(15, 8), lhValidData(7, 0))
  protected val lbWire: UInt = Cat(
    Fill(p.busWidth - 8, Mux(io.func3(2), 0.U, lbValidData(7))),
    lbValidData
  )
  protected val lhWire: UInt = Cat(
    Fill(p.busWidth - 16, Mux(io.func3(2), 0.U, lhValidData(15))),
    lhValidData
  )
  protected val lwWire: UInt = Cat(
    Fill(p.busWidth - 32, Mux(io.func3(2), 0.U, lwValidData(31))),
    lwValidData
  )
  protected val ldWire: UInt = io.memRData
  protected val loadData: UInt = MuxLookup(io.func3(1, 0), io.memRData, Array(
    0.U -> lbWire,
    1.U -> lhWire,
    2.U -> lwWire,
    3.U -> ldWire,
  ))
  // TODO
  protected val wMask: UInt = Wire(UInt(p.busWidth.W))
  wMask := 0.U
  io.memAddr := io.exeAddr
  io.wbData := loadData
  io.memDoWrite := io.memCmd === ("b" + M_XWR).U
  io.memWData := io.exeData
  io.memValid := io.exeMemValid
  io.memMask := wMask
}
