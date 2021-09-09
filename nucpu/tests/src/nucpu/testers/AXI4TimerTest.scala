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

class AXI4TimerTest extends AnyFlatSpec with ChiselScalatestTester with Matchers{
  implicit val p: AXIConfigs= new AXIConfigs()

  behavior of "AXI4Timer"
  it should "AXI4Timer test" in {
    test(new AXI4Timer).withAnnotations(Seq(WriteVcdAnnotation, VerilatorBackendAnnotation)) { dut =>
      //initialization
      //val dutIO = dut.io
      val clock = dut.clock
      dut.reset.poke(true.B)
      clock.step()
      dut.reset.poke(false.B)
      clock.step()

      clock.step(100)
    }
  }
}

object Timer_Generator extends App {
  implicit val p: AXIConfigs = new AXIConfigs()
  (new ChiselStage).run(Seq(
    ChiselGeneratorAnnotation(() => new AXI4Timer),
    TargetDirAnnotation(directory = "test_run_dir/AXI")
  ))
}