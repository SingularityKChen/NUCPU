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

class NUCPUTest extends AnyFlatSpec with Matchers with ChiselScalatestTester {
  implicit val p: Configs = new Configs(diffTest = false)
  protected val instructions: Instructions.type = Instructions
  val timeoutCycle = 50000
  val binDir = "./AM/am-kernels/tests/cpu-tests/build/"
  val binFileNames: Seq[String] = getBinFilenames(new File(binDir)).map(x => x.getName)
  val testcaseNames: Seq[String] = binFileNames.map(x => x.stripSuffix("-riscv64-mycpu.bin"))
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
    instArray.map(x => "h" + x)
  }

  def getInstFromBinFileName(filename: String): Array[String] = {
    getInstFromHexArray(getHexArray(binDir + filename))
  }

  def runTest(instArray: Array[String]): TestResult = {
    test(new NUCPU()).withAnnotations(Seq(WriteVcdAnnotation, VerilatorBackendAnnotation)) { dut =>
      val dataArray = new Array[String](10000) // 64 Bit
      val dutIO = dut.io
      val clock = dut.clock
      dut.reset.poke(true.B)
      clock.step()
      dut.reset.poke(false.B)
      var trap = false
      var timeout = false
      var curCycle = 0
      fork {
        while (!trap) {
          while (!dutIO.instValid.peek().litToBoolean) {
            clock.step()
            curCycle += 1
            timeout = curCycle > timeoutCycle
            if (timeout) {
              throw new TimeoutException("[ERROR] Timeout Now!")
            }
          }
          val curPC = dutIO.instAddr.peek().litValue().toInt
          val curInstIdx = (curPC - 2147483648L).toInt / 4
          val curInst = instArray(curInstIdx)
          trap = curInst == p.instTrap.stripPrefix("h")
          dutIO.inst.poke(curInst.U)
          println(s"[INFO] PC 0x${curPC.toHexString}: 0x$curInst")
          clock.step()
          curCycle += 1
          timeout = curCycle > timeoutCycle
          if (timeout) {
            throw new TimeoutException("[ERROR] Timeout Now! Never hit trap.")
          }
        }
      } .fork.withRegion(Monitor) {
        while (!trap) {
          while (!dutIO.memValid.peek().litToBoolean) {
            clock.step()
          }
          val curAddr = dutIO.memAddr.peek().litValue().toInt
          val curAddrIdx = (curAddr - 2147483648L).toInt / 4
          if(curAddrIdx < instArray.length) {
            val readData = instArray(curAddrIdx)
            dutIO.memRData.poke(readData.U)
            println(s"[INFO] Read 0x$readData from 0x${curAddr.toHexString}")
          } else {
            val dataIdx = (curAddrIdx - instArray.length) / 2
            require(dataIdx < dataArray.length,
              s"[ERROR] Memory index should less than ${dataArray.length} but $dataIdx")
            if (dutIO.memDoWrite.peek().litToBoolean) {
              val writeData = "h" + dutIO.memWData.peek().litValue().toString(16)
              dataArray(dataIdx) = writeData
              println(s"[INFO] Write 0x$writeData into 0x${curAddr.toHexString}")
            } else {
              val readData = dataArray(dataIdx)
              dutIO.memRData.poke(readData.U)
              println(s"[INFO] Read 0x$readData from 0x${curAddr.toHexString}")
            }
          }
          clock.step()
        }
      } .joinAndStep(clock)

    }
  }

  behavior of "NUCPU"

  // CPU TEST
  for (idx <- testcaseNames.indices) {
    it should s"cpu-test-${testcaseNames(idx)}".replaceAll("-", "_") in {
      val instArray = getInstFromBinFileName(binFileNames(idx))
      runTest(instArray)
    }
  }

  // RISCV-AM TEST
  it should "riscv_test" in {
    val instArray = getInstFromHexArray(getHexArray(
      "./AM/am-kernels/tests/am-tests/build/amtest-riscv64-mycpu.bin")
    )
    runTest(instArray)
  }

}

object Generator extends App {
  implicit val p: Configs = new Configs(diffTest = true)
  (new ChiselStage).run(Seq(
    ChiselGeneratorAnnotation(() => new SimTop()),
    TargetDirAnnotation(directory = "test_run_dir/NUCPU")
  ))
}
