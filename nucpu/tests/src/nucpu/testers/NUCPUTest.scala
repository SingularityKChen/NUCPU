package nucpu.testers

import chisel3._
import chisel3.stage.{ChiselGeneratorAnnotation, ChiselStage}
import chiseltest._
import chiseltest.experimental.TestOptionBuilder._
import chiseltest.internal.{VerilatorBackendAnnotation, WriteVcdAnnotation}
import difftestSim.SimTop
import firrtl.options.TargetDirAnnotation
import nucpu._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.io.File
import java.nio.file.{Files, Paths}
import scala.math.pow
import scala.util.Random

class NUCPUTest extends AnyFlatSpec with Matchers with ChiselScalatestTester {
  implicit val p: Configs = new Configs(diffTest = false)
  protected val instructions: Instructions.type = Instructions
  val binDir = "./AM/am-kernels/tests/cpu-tests/build/"
  val binFileNames: Seq[String] = getBinFilenames(new File(binDir)).map(x => x.getName)
  val testcaseNames: Seq[String] = binFileNames.map(x => x.stripSuffix("-riscv64-nemu.bin"))
  testcaseNames.zipWithIndex.foreach({ case (str, i) => println(s"[INFO] Idx $i: test case name $str")})

  /**Return the bin files under current directory (not including the subdirectories.
   * @param dir: the directory to find the bin files.*/
  def getBinFilenames(dir: File): Seq[File] = {
    dir.listFiles.filter(_.isFile).filter(_.getName.matches(".*bin")).toIterator.toSeq
  }

  /**Return the hex string of the bin file but the order is the original order.*/
  def getHexArray(filename: String): Array[String] = {
    Files.readAllBytes(Paths.get(filename)).map(x => ("00" + x.toHexString).takeRight(2))
  }

  def getInstFromHexArray(hexArray: Array[String]): Array[String] = {
    val instArray = new Array[String](hexArray.length / 4)
    for (idx <- 0 until hexArray.length / 4) {
      instArray(idx) = hexArray(idx * 4 + 3) + hexArray(idx * 4 + 2) + hexArray(idx * 4 + 1) + hexArray(idx * 4)
    }
    instArray
  }

  def getInstFromBinFileName(filename: String): Array[String] = {
    getInstFromHexArray(getHexArray(binDir + filename)).map(x => "h" + x)
  }

  behavior of "NUCPU"

  it should s"cpu-test-${testcaseNames(2)}".replaceAll("-", "_") in {
    test(new NUCPU()).withAnnotations(Seq(WriteVcdAnnotation, VerilatorBackendAnnotation)) { dut =>
      val dutIO = dut.io
      val clock = dut.clock
      dut.reset.poke(true.B)
      clock.step()
      dut.reset.poke(false.B)
      for (inst <- getInstFromBinFileName(binFileNames(2))) {
        dutIO.inst.poke(inst.U)
        clock.step()
      }
    }
  }

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
      dutIO.inst.poke("h00100093".U)
      clock.step()
      dutIO.inst.poke("h00200093".U)
      clock.step()
      dutIO.inst.poke("h00108093".U)
      clock.step()
      println(s"string: ${instructions.ADDI.toString}")
      // FIXME: use address to read the instruction instead
      for (_ <- 0 until testCaseNum) {
        while (!dutIO.instValid.peek().litToBoolean) {
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
  implicit val p: Configs = new Configs(diffTest = true)
  (new ChiselStage).run(Seq(
    ChiselGeneratorAnnotation(() => new SimTop()),
    TargetDirAnnotation(directory = "test_run_dir/NUCPU")
  ))
}
