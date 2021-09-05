package axi4

import chisel3._

class TimerIO extends Bundle {
  val mtip = Output(Bool())
}

class AXI4Timer
(
  sim: Boolean = false
  address: 
)(implicit val p: AXIConfigs)
  extends Module
{

}
