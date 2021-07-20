package nucpu.testers

import chisel3._
import chisel3.stage.{ChiselGeneratorAnnotation, ChiselStage}
import chiseltest._
import chiseltest.experimental.TestOptionBuilder._
import chiseltest.internal.{VerilatorBackendAnnotation, WriteVcdAnnotation}
import firrtl.options.TargetDirAnnotation
import nucpu._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class NUCPUTest extends AnyFlatSpec with Matchers with ChiselScalatestTester {
  implicit val p: Configs = new Configs
  behavior of "NUCPU"

  it should "pass addi instruction" in {
    test(new NUCPU()).withAnnotations(Seq(WriteVcdAnnotation, VerilatorBackendAnnotation)) { dut =>
      val dutIO = dut.io
      val clock = dut.clock
      dut.reset.poke(true.B)
      clock.step()
      dut.reset.poke(false.B)
      clock.step()
    }
  }
}

object Generator extends App {
  implicit val p: Configs = new Configs
  (new ChiselStage).run(Seq(
    ChiselGeneratorAnnotation(() => new NUCPU()),
    TargetDirAnnotation(directory = "test_run_dir/NUCPU")
  ))
}
