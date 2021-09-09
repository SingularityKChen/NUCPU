package axi4

import chisel3._
import chisel3.util._
//import scala.math.min

class TimerIOs extends Bundle {
  val mtip: Bool = Output(Bool())

}
//Version1: Timer is an individual part
//AXI4Timer isnot a kind of AXI slave module
class AXI4Timer
(
  sim: Boolean = false
) (implicit p: AXIConfigs )extends Module() {
  val io: TimerIOs = IO(new TimerIOs)

  val mtime: UInt = RegInit(0.U(64.W)) // unit is 1us: 10^-6
  val mtimecmp: UInt = RegInit(1000.U(64.W))

  val clk: Int = if (!sim) 40 else 10000 //40MHz * 10^-6 = 40s
  val freq: UInt = RegInit(clk.U(16.W))
  val inc: UInt = RegInit(1000.U(16.W))  //timer interrupt gap

  val cnt: UInt = RegInit(0.U(16.W))
  val nextcnt: UInt = cnt + 1.U
  cnt := Mux(nextcnt < freq, nextcnt, 0.U)
  val beat: Bool = nextcnt === freq
  when(beat) {
    mtime := mtime + inc
  }

  object LookupTree {
    def apply[T <: Data](key: UInt, mapping: Iterable[(UInt, T)]): T =
      Mux1H(mapping.map(p => (p._1 === key, p._2)))
  }

  object MaskData {
    def apply(oldData: UInt, newData: UInt, fullmask: UInt): UInt = {
      val inverseMask: UInt = (~fullmask).asUInt()
      (newData & fullmask) | (oldData & inverseMask)
    }
  }

  object RegMap {
    def Unwritable: Null = null
    def apply(addr: Int, reg: UInt, wfn: UInt => UInt = x => x): (Int, (UInt, UInt => UInt)) = (addr, (reg, wfn))
    def generate(mapping: Map[Int, (UInt, UInt => UInt)], raddr: UInt, rdata: UInt,
                 waddr: UInt, wen: Bool, wdata: UInt, wmask: UInt):Unit = {
      val chiselMapping = mapping.map { case (a, (r, w)) => (a.U, r, w) }
      rdata := LookupTree(raddr, chiselMapping.map { case (a, r, w) => (a, r) })
      chiselMapping.map { case (a, r, w) =>
        if (w != null) when (wen && waddr === a) { r := w(MaskData(r, wdata, wmask)) }
      }
    }
    def generate(mapping: Map[Int, (UInt, UInt => UInt)], addr: UInt, rdata: UInt,
                 wen: Bool, wdata: UInt, wmask: UInt):Unit = generate(mapping, addr, rdata, addr, wen, wdata, wmask)
  }

  val mapping = Map(
    RegMap(0x4000, mtimecmp),
    RegMap(0x8000, freq),
    RegMap(0x8008, inc),
    RegMap(0xbff8, mtime)
  )

  io.mtip := RegNext(mtime >= mtimecmp)

  when(mtime >= mtimecmp){
    mtimecmp := mtimecmp + inc
  }
}

//Version2: Timer is an internal part
//AXI4Timer is a kind of AXI slave module

//class AXI4Timer
//(
//  sim: Boolean = false
//  //  address: Seq[AddressSet]
//  //) (implicit p: AXIConfigs )extends AXI4LiteSlaveModule() {
//) (implicit p: AXIConfigs )extends Module() {
//  val timerio: TimerIOs = IO(new TimerIOs)
//
//  val mtime: UInt = RegInit(0.U(64.W)) // unit is 1us: 10^-6
//  val mtimecmp: UInt = RegInit(0.U(64.W))
//
//  val clk: Int = if (!sim) 40 else 10000 //40MHz * 10^-6 = 40s
//  val freq: UInt = RegInit(clk.U(16.W))
//  val inc: UInt = RegInit(1.U(16.W))
//
//  val cnt: UInt = RegInit(0.U(16.W))
//  val nextcnt: UInt = cnt + 1.U
//  cnt := Mux(nextcnt < freq, nextcnt, 0.U)
//  val beat: Bool = nextcnt === freq
//  when(beat) {
//    mtime := mtime + inc
//  }
//
//  object LookupTree {
//    def apply[T <: Data](key: UInt, mapping: Iterable[(UInt, T)]): T =
//      Mux1H(mapping.map(p => (p._1 === key, p._2)))
//  }
//
//  object MaskData {
//    def apply(oldData: UInt, newData: UInt, fullmask: UInt): UInt = {
//      val inverseMask: UInt = (~fullmask).asUInt()
//      (newData & fullmask) | (oldData & inverseMask)
//    }
//  }
//
//  object RegMap {
//    def Unwritable: Null = null
//    def apply(addr: Int, reg: UInt, wfn: UInt => UInt = x => x): (Int, (UInt, UInt => UInt)) = (addr, (reg, wfn))
//    def generate(mapping: Map[Int, (UInt, UInt => UInt)], raddr: UInt, rdata: UInt,
//                 waddr: UInt, wen: Bool, wdata: UInt, wmask: UInt):Unit = {
//      val chiselMapping = mapping.map { case (a, (r, w)) => (a.U, r, w) }
//      rdata := LookupTree(raddr, chiselMapping.map { case (a, r, w) => (a, r) })
//      chiselMapping.map { case (a, r, w) =>
//        if (w != null) when (wen && waddr === a) { r := w(MaskData(r, wdata, wmask)) }
//      }
//    }
//    def generate(mapping: Map[Int, (UInt, UInt => UInt)], addr: UInt, rdata: UInt,
//                 wen: Bool, wdata: UInt, wmask: UInt):Unit = generate(mapping, addr, rdata, addr, wen, wdata, wmask)
//  }
//
//  val mapping = Map(
//    RegMap(0x4000, mtimecmp),
//    RegMap(0x8000, freq),
//    RegMap(0x8008, inc),
//    RegMap(0xbff8, mtime)
//  )
//  def getOffset(addr: UInt): UInt = addr(15,0)
//
//  object MaskExpand {
//    def apply(m: UInt): UInt = Cat(m.asBools().map(Fill(8, _)).reverse)
//  }
//
//  RegMap.generate(mapping, getOffset(raddr), in.r.bits.data,
//    getOffset(waddr),in.w.fire,in.w.bits.data, MaskExpand(in.w.bits.strb))
//
//  timerio.mtip := RegNext(mtime >= mtimecmp)
//}