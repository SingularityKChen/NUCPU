package axi4

import chisel3._
import chisel3.util._

class AXI4BundleAIOs()(implicit val p: AXIConfigs) extends Bundle {
  val id:     UInt = Output(UInt(p.idBits.W))
  val addr:   UInt = Output(UInt(p.addrBits.W))
  val len:    UInt = Output(UInt(p.lenBits.W))
  val size:   UInt = Output(UInt(p.sizeBits.W))
  val burst:  UInt = Output(UInt(p.burstBits.W))
  val lock:   Bool = Output(Bool())
  val cache:  UInt = Output(UInt(p.cacheBits.W))
  val prot:   UInt = Output(UInt(p.protBits.W))
  val qos:    UInt = Output(UInt(p.qosBits.W))
  val user:   UInt = Output(UInt(p.userBits.W))
}

class AXI4BundleRIOs()(implicit val p: AXIConfigs) extends Bundle {
  val id:     UInt = Output(UInt(p.idBits.W))
  val data:   UInt = Output(UInt(p.dataBits.W))
  val resp:   UInt = Output(UInt(p.respBits.W))
  val last:   Bool = Output(Bool())
  val user:   UInt = Output(UInt(p.userBits.W))
}

class AXI4BundleWIOs()(implicit val p: AXIConfigs) extends Bundle {
  val data:   UInt = Output(UInt(p.dataBits.W))
  val strb:   UInt = Output(UInt((p.dataBits/8).W))
  val last:   Bool = Output(Bool())
  val user:   UInt = Output(UInt(p.userBits.W))
}

class AXI4BundleBIOs()(implicit val p: AXIConfigs) extends Bundle {
  val id:     UInt = Output(UInt(p.idBits.W))
  val resp:   UInt = Output(UInt(p.respBits.W))
  val user:   UInt = Output(UInt(p.userBits.W))
}

class rwIOs_i()(implicit val p: AXIConfigs) extends Bundle {
  val req:        UInt = Output(UInt(p.rwreqBits.W))
  val data_write: UInt = Output(UInt(p.rwdataBits.W))
  val rw_addr:    UInt = Output(UInt(p.dataBits.W))
  val rw_size:    UInt = Output(UInt(p.rwsizeBits.W))

}
class rwIOs_o()(implicit val p: AXIConfigs) extends Bundle {
  val rw_resp:    UInt = Output(UInt(p.respBits.W))
  val data_read:  UInt = Output(UInt(p.rwdataBits.W))
}

class AXI4IOs()(implicit val p: AXIConfigs) extends Bundle {
  val ar: DecoupledIO[AXI4BundleAIOs] = Decoupled(new AXI4BundleAIOs())             //AXI4BundleA is the common Address channel
  val r: DecoupledIO[AXI4BundleRIOs]  = Flipped(Decoupled(new AXI4BundleRIOs()))    //AXI4BundleR is the Read channel
  val aw: DecoupledIO[AXI4BundleAIOs] = Decoupled(new AXI4BundleAIOs())             //AXI4BundleA is the common Address channel
  val w: DecoupledIO[AXI4BundleWIOs]  = Decoupled(new AXI4BundleWIOs())             //AXI4BundleW is the Write channel
  val b: DecoupledIO[AXI4BundleBIOs]  = Flipped(Decoupled(new AXI4BundleBIOs()))    //AXI4BundleW is the Write Response channel
  val rw_i: DecoupledIO[rwIOs_i]      = Flipped(Decoupled(new rwIOs_i()))           //rwIOs are used to connected with master device, CPU
  val rw_o: rwIOs_o                   = new rwIOs_o()
}
