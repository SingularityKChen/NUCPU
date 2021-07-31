package nucpu

import chisel3._
import chisel3.util._

class Cache()(implicit val p: CacheConfigs) extends Module {
  val io: CacheIOs = IO(new CacheIOs())
  // TODO: check whether need a state machine
  val tagArray: SyncReadMem[Vec[UInt]] = SyncReadMem(p.set, Vec(p.way, UInt(p.tagBit.W)))
  // Data: set sel, way sel, byte sel, 1B data
  val dataArray: SyncReadMem[Vec[Vec[UInt]]] = SyncReadMem(p.set, Vec(p.way, Vec(p.lineByte, UInt(8.W))))
  /** Valid flag to indicate whether the data in current cache is Valid*/
  val vFlag: Vec[UInt] = RegInit(VecInit(Seq.fill(p.way)(0.U(p.set.W))))
  /** Dirty flag to indicate whether the data in current cache is dirty*/
  val dFlag: Vec[UInt] = RegInit(VecInit(Seq.fill(p.way)(0.U(p.set.W))))
  /** The wires for each way to check whether any way hits*/
  val tagsHit: Vec[Bool] = Wire(Vec(p.way, Bool()))
  /** Any way hits, then cache hits.*/
  val hit: Bool = tagsHit.reduce(_ || _)
  val hitWayIdx: UInt = OHToUInt(tagsHit)
  /** The tag from the cpu*/
  val cpuTag: UInt = io.coreReq.bits.addr(p.busWidth - 1, p.busWidth - p.tagBit)
  val cpuIndex: UInt = io.coreReq.bits.addr(p.setBit + p.offsetBit - 1, p.offsetBit)
  val cpuOffset: UInt = io.coreReq.bits.addr(p.offsetBit - 1, 0)
  val tags: Vec[UInt] = tagArray.read(cpuOffset)
  tagsHit.zipWithIndex.foreach({ case (tagHit, wayIdx) =>
    tagHit := vFlag(wayIdx)(cpuIndex) && !dFlag(wayIdx)(cpuIndex) && (tags(wayIdx) === cpuTag)
  })
  io.coreResp.valid := RegNext(hit) // data will be read out one cycle layer
  io.coreResp.bits.data := dataArray.read(cpuIndex)(hitWayIdx)(cpuOffset)
  // read memory if cache miss
  io.memReq.valid := !hit
  io.memReq.bits.addr := io.coreReq.bits.addr
  io.memReq.bits.mask := io.coreReq.bits.addr
  val refillTag: UInt = io.memResp.bits.data(p.busWidth - 1, p.busWidth - p.tagBit)
  val refillData: UInt = io.memResp.bits.data(p.busWidth - p.tagBit - 1, 0)
  val (refillWayIdx, _) = Counter(io.memResp.valid && io.memReq.valid, p.way)
  tagArray.write(cpuIndex, VecInit(Seq.fill(p.way)(refillTag)), Seq.tabulate(p.way)(refillWayIdx === _.U))
}

class CacheReq()(implicit val p: Configs) extends Bundle {
  val addr: UInt = UInt(p.busWidth.W)
  val data: UInt = UInt(p.busWidth.W)
  val mask: UInt = UInt((p.busWidth / 8).W)
}

class CacheResp()(implicit val p: Configs) extends Bundle {
  val data: UInt = UInt(p.busWidth.W)
}

class CacheIOs()(implicit val p: Configs) extends Bundle {
  // CPU -> Cache
  val coreReq: ValidIO[CacheReq] = Flipped(Valid(new CacheReq()))
  val coreResp: ValidIO[CacheResp] = Valid(new CacheResp())
  // Cache -> Memory
  val memReq: ValidIO[CacheReq] = Valid(new CacheReq())
  val memResp: ValidIO[CacheResp] = Flipped(Valid(new CacheResp()))
}
