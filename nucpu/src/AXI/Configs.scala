package axi4

import chisel3.util._

//AXI parameters
class Configs {
  val addrBits   = 64
  val dataBits   = 64
  val rwaddrBits = 64
  val rwdataBits = 64
  val rwsizeBits = 2
  val rwreqBits  =1
}

class AXIConfigs extends Configs{
  val userBits  = 1
  val idBits    = 4
  val lenBits   = 8
  val sizeBits  = 3
  val burstBits = 2
  val cacheBits = 4
  val protBits  = 3
  val qosBits   = 4
  val respBits  = 2


  val ALIGNED_WIDTH:  Int = log2Ceil(dataBits / 8)              //3
  val OFFSET_WIDTH:   Int = log2Ceil(dataBits)                  //6
  val AXI_SIZE:       Int = log2Ceil(dataBits / 8)              //3
  val MASK_WIDTH:     Int = dataBits * 2                        //128
  val TRANS_LEN:      Int = rwdataBits / dataBits               //1

}
//// Burst types
//`define AXI_BURST_TYPE_FIXED                                2'b00
//`define AXI_BURST_TYPE_INCR                                 2'b01
//`define AXI_BURST_TYPE_WRAP                                 2'b10
//// Access permissions
//`define AXI_PROT_UNPRIVILEGED_ACCESS                        3'b000
//`define AXI_PROT_PRIVILEGED_ACCESS                          3'b001
//`define AXI_PROT_SECURE_ACCESS                              3'b000
//`define AXI_PROT_NON_SECURE_ACCESS                          3'b010
//`define AXI_PROT_DATA_ACCESS                                3'b000
//`define AXI_PROT_INSTRUCTION_ACCESS                         3'b100
//// Memory types (AR)
//`define AXI_ARCACHE_DEVICE_NON_BUFFERABLE                   4'b0000
//`define AXI_ARCACHE_DEVICE_BUFFERABLE                       4'b0001
//`define AXI_ARCACHE_NORMAL_NON_CACHEABLE_NON_BUFFERABLE     4'b0010
//`define AXI_ARCACHE_NORMAL_NON_CACHEABLE_BUFFERABLE         4'b0011
//`define AXI_ARCACHE_WRITE_THROUGH_NO_ALLOCATE               4'b1010
//`define AXI_ARCACHE_WRITE_THROUGH_READ_ALLOCATE             4'b1110
//`define AXI_ARCACHE_WRITE_THROUGH_WRITE_ALLOCATE            4'b1010
//`define AXI_ARCACHE_WRITE_THROUGH_READ_AND_WRITE_ALLOCATE   4'b1110
//`define AXI_ARCACHE_WRITE_BACK_NO_ALLOCATE                  4'b1011
//`define AXI_ARCACHE_WRITE_BACK_READ_ALLOCATE                4'b1111
//`define AXI_ARCACHE_WRITE_BACK_WRITE_ALLOCATE               4'b1011
//`define AXI_ARCACHE_WRITE_BACK_READ_AND_WRITE_ALLOCATE      4'b1111
//// Memory types (AW)
//`define AXI_AWCACHE_DEVICE_NON_BUFFERABLE                   4'b0000
//`define AXI_AWCACHE_DEVICE_BUFFERABLE                       4'b0001
//`define AXI_AWCACHE_NORMAL_NON_CACHEABLE_NON_BUFFERABLE     4'b0010
//`define AXI_AWCACHE_NORMAL_NON_CACHEABLE_BUFFERABLE         4'b0011
//`define AXI_AWCACHE_WRITE_THROUGH_NO_ALLOCATE               4'b0110
//`define AXI_AWCACHE_WRITE_THROUGH_READ_ALLOCATE             4'b0110
//`define AXI_AWCACHE_WRITE_THROUGH_WRITE_ALLOCATE            4'b1110
//`define AXI_AWCACHE_WRITE_THROUGH_READ_AND_WRITE_ALLOCATE   4'b1110
//`define AXI_AWCACHE_WRITE_BACK_NO_ALLOCATE                  4'b0111
//`define AXI_AWCACHE_WRITE_BACK_READ_ALLOCATE                4'b0111
//`define AXI_AWCACHE_WRITE_BACK_WRITE_ALLOCATE               4'b1111
//`define AXI_AWCACHE_WRITE_BACK_READ_AND_WRITE_ALLOCATE      4'b1111
//
//`define AXI_SIZE_BYTES_1                                    3'b000
//`define AXI_SIZE_BYTES_2                                    3'b001
//`define AXI_SIZE_BYTES_4                                    3'b010
//`define AXI_SIZE_BYTES_8                                    3'b011
//`define AXI_SIZE_BYTES_16                                   3'b100
//`define AXI_SIZE_BYTES_32                                   3'b101
//`define AXI_SIZE_BYTES_64                                   3'b110
//`define AXI_SIZE_BYTES_128                                  3'b111