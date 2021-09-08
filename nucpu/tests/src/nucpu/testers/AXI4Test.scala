package axi4.tester

import chisel3._
import chisel3.stage.{ChiselGeneratorAnnotation, ChiselStage}
import chiseltest._
import chiseltest.experimental.TestOptionBuilder._
import chiseltest.internal.{VerilatorBackendAnnotation, WriteVcdAnnotation}
import firrtl.options.TargetDirAnnotation
import axi4._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

//import scala.util.Random



class AXI4Test extends AnyFlatSpec with ChiselScalatestTester with Matchers{
  implicit val p: AXIConfigs= new AXIConfigs()

  //transaction

//  class info{
//    val address: UInt = UInt(p.addrBits.W)
//    val data: UInt = UInt(p.dataBits.W)
//    val req: UInt = UInt(p.rwreqBits.W)
//  }

  //sequence

  //driver

  behavior of "AXI4"

  it should "AXI4 test" in{
    test(new AXI4()).withAnnotations(Seq(WriteVcdAnnotation, VerilatorBackendAnnotation)) { dut =>
      //initialization
      val dutIO = dut.io
      val clock = dut.clock
      dut.reset.poke(true.B)
      clock.step()
      dut.reset.poke(false.B)
      clock.step()
      //sequencer
//      val testSeq = Seq.fill(4)(Random.nextInt(100))
val testSeq = Seq(32,33,34,35)
      //test begin
      for (i <- testSeq.indices){

        dutIO.rw_i.bits.req.poke(0.U)
        dutIO.rw_i.bits.rw_addr.poke((8*i).U(p.addrBits.W))
        dutIO.rw_i.bits.rw_size.poke("b10".U)
        dutIO.rw_i.valid.poke(true.B)

        clock.step()

        dutIO.ar.ready.poke(true.B)

        clock.step(2)


        dutIO.r.valid.poke(true.B)
        dutIO.r.bits.data.poke(testSeq(i).U)

        dutIO.r.bits.last.poke(true.B)
        dutIO.r.bits.resp.poke("b00".U)


      }
      clock.step(3)
    }

  }

  //score board

}







object Generator extends App {
  implicit val p: AXIConfigs = new AXIConfigs()
  (new ChiselStage).run(Seq(
    ChiselGeneratorAnnotation(() => new AXI4()),
    TargetDirAnnotation(directory = "test_run_dir/AXI")
  ))
}