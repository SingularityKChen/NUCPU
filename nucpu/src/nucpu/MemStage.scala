package nucpu

import chisel3._
import chisel3.util._
import DecodeParams._
import chisel3.util.experimental.decode._

import scala.math.pow

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
  protected def getMaskStr(func3: Int, addr: Int): String = {
    val str = "b"
    val len = pow(2, func3 + 3).toInt
    val divAddr = (8 * addr) / len
    val prefix = if( p.busWidth - (divAddr + 1) * len > 0) "0" * (p.busWidth - (divAddr + 1) * len) else ""
    val postfix = if( divAddr > 0) "0" * divAddr * len else ""
    println(s"[INFO] $func3, $addr, $prefix, $postfix")
    str + prefix + "1" * len + postfix
  }
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
  protected val wMask: UInt = decoder(
    minimizer = EspressoMinimizer,
    input = Cat(io.func3(1, 0), io.exeAddr(2, 0)),
    truthTable = TruthTable(Map(
      BitPat("b11" + "???") -> BitPat("b" + "1" * 64),
      BitPat("b10" + "1??") -> BitPat("b" + "1" * 32 + "0" * 32),
      BitPat("b10" + "0??") -> BitPat("b" + "0" * 32 + "1" * 32),
      BitPat("b01" + "11?") -> BitPat("b" + "1" * 16 + "0" * 48),
      BitPat("b01" + "10?") -> BitPat("b" + "0" * 16 + "1" * 16 + "0" * 32),
      BitPat("b01" + "01?") -> BitPat("b" + "0" * 32 + "1" * 16 + "0" * 16),
      BitPat("b01" + "00?") -> BitPat("b" + "0" * 48 + "1" * 16),
      BitPat("b00" + "111") -> BitPat("b" + "1" * 8 + "0" * 56),
      BitPat("b00" + "110") -> BitPat("b" + "0" * 8 + "1" * 8 + "0" * 48),
      BitPat("b00" + "101") -> BitPat("b" + "0" * 16 + "1" * 8 + "0" * 40),
      BitPat("b00" + "100") -> BitPat("b" + "0" * 24 + "1" * 8 + "0" * 32),
      BitPat("b00" + "011") -> BitPat("b" + "0" * 32 + "1" * 8 + "0" * 24),
      BitPat("b00" + "010") -> BitPat("b" + "0" * 40 + "1" * 8 + "0" * 16),
      BitPat("b00" + "001") -> BitPat("b" + "0" * 48 + "1" * 8 + "0" * 8),
      BitPat("b00" + "000") -> BitPat("b" + "0" * 56 + "1" * 8),
    ), BitPat("b" + "1"*p.busWidth)))
  io.memAddr := io.exeAddr
  io.wbData := loadData
  io.memDoWrite := io.memCmd === ("b" + M_XWR).U
  //  FIXME: unaligned test: 0.U -> io.exeData(7, 0),
  io.memWData := MuxLookup(io.func3(1, 0), io.exeData, Range(0, 4).map(x =>
    x.U -> Cat(Fill(pow(2, 3-x).toInt, io.exeData(pow(2, x+3).toInt - 1, 0)))
  ))
  io.memValid := io.exeMemValid
  io.memMask := wMask
}
