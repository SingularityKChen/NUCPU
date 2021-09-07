package axi4.tester

import chisel3._
import chisel3.stage.{ChiselGeneratorAnnotation, ChiselStage}
import chiseltest._
import chiseltest.experimental.TestOptionBuilder._
import chiseltest.internal.{VerilatorBackendAnnotation, WriteVcdAnnotation}

import firrtl.options.TargetDirAnnotation
import axi4._
import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.io.File
import java.nio.file.{Files, Paths}

import scala.util.Random

class AXI4Test extends AnyFlatSpec with ChiselScalatestTester with Matchers{
  implicit val p: AXIConfigs= new AXIConfigs()

  //transaction

//  class info{
//    val address: UInt = UInt(p.addrBits.W)
//    val data: UInt = UInt(p.dataBits.W)
//    val req: UInt = UInt(p.rwreqBits.W)
//  }

  //sequence
  protected val AXI4_Read_Data: Vec[UInt] = RegInit(VecInit(Seq.fill(p.TRANS_LEN)(0.U(p.dataBits.W))))
  AXI4_Read_Data(0) := 0.U(p.dataBits.W)
  AXI4_Read_Data(1) := 1.U(p.dataBits.W)
  AXI4_Read_Data(2) := 2.U(p.dataBits.W)
  AXI4_Read_Data(3) := 3.U(p.dataBits.W)
  AXI4_Read_Data(4) := 4.U(p.dataBits.W)
  AXI4_Read_Data(5) := 5.U(p.dataBits.W)
  AXI4_Read_Data(6) := 6.U(p.dataBits.W)
  AXI4_Read_Data(7) := 7.U(p.dataBits.W)
  AXI4_Read_Data(8) := 8.U(p.dataBits.W)
  AXI4_Read_Data(9) := 9.U(p.dataBits.W)


  //sequencer

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
      //test begin
      for (i <- 0 to 9){
        dutIO.rw_i.bits.req.poke(i.U)
        dutIO.rw_i.bits.rw_addr.poke(0.U)
        dutIO.rw_i.bits.rw_size.poke("b10".U)
        dutIO.rw_i.valid.poke(true.B)
        clock.step()
        dutIO.ar.ready.poke(true.B)
        clock.step(3)
        dutIO.r.valid.poke(true.B)
        dutIO.r.bits.data.poke(AXI4_Read_Data(i))
        dutIO.r.bits.last.poke(true.B)
        clock.step()
        dutIO.r.bits.resp.poke("b00".U)

        dutIO.rw_o.data_read.peek()
      }

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