package nucpu

class Configs(val diffTest: Boolean) {
  val busWidth = 64
  // for io
  val addrWidth = 5
  val typeWidth = 5
  val opcodeWidth = 8
  // for instruction
  val instW = 32
  val instOpW = 7
  val instRdW = 5
  val instRsW = 5
  val instFunc3W = 3
  val instImmW = 12
  // regfile
  val regNum = 32
}

class RVI {
  val addi = 17
}
