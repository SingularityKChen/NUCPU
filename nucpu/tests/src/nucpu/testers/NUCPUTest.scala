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

import scala.math.pow
import scala.util.Random

class NUCPUTest extends AnyFlatSpec with Matchers with ChiselScalatestTester {
  implicit val p: Configs = new Configs
  protected val instructions: Instructions.type = Instructions
  behavior of "NUCPU"

  it should "pass addi instruction" in {
    val opcode = "00100" + "11"
    val func3 = "000"
    val testCaseNum = 1000
    test(new NUCPU()).withAnnotations(Seq(WriteVcdAnnotation, VerilatorBackendAnnotation)) { dut =>
      val dutIO = dut.io
      val clock = dut.clock
      dut.reset.poke(true.B)
      clock.step()
      dut.reset.poke(false.B)
      println(s"string: ${instructions.ADDI.toString}")
      // FIXME: use address to read the instruction instead
      for (_ <- 0 until testCaseNum) {
        while (!dutIO.inst_ena.peek().litToBoolean) {
          clock.step()
        }
        val immValue = Random.nextInt(pow(2, p.instImmW - 1).toInt).toBinaryString
        var imm = ""
        if (immValue.length > 6) {
          imm = s"%0${p.instImmW - 6}d".format(immValue.take(immValue.length - 6).toInt) + immValue.takeRight(6)
        } else {
          imm = s"%0${p.instImmW}d".format(immValue.toInt)
        }
        val rs1 = s"%0${p.instRsW}d".format(Random.nextInt(pow(2, p.instRsW).toInt).toBinaryString.toInt)
        val rd = s"%0${p.instRdW}d".format(Random.nextInt(pow(2, p.instRdW).toInt).toBinaryString.toInt)
        val inst = "b" + imm + rs1 + func3 + rd + opcode
        println(s"[INFO] Current instruction is $inst.")
        dutIO.inst.poke(inst.U)
        clock.step()
      }
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
