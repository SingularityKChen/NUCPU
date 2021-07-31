# NUCPU

## Introduction

This is an RISC-V 64I inorder toy CPU in Chisel which is built via [mill](https://com-lihaoyi.github.io/mill/page/configuring-mill.html).

## Environment

### IDE support
For mill use
```bash
mill mill.bsp.BSP/install
```
then open by Intellij IDEA.

### CPU-Test

We wrote several Scala functions to read the binary file as the sequencer of CPU-Test. To execute the Chisel tests or DiffTest Framework, you need to build the binary files firstly:
```bash
cd ./AM/am-kernels/tests/cpu-tests
make ARCH=riscv64-mycpu
```

### DiffTest Framework based on NEMU

This RISC-V CPU also is connected to [DiffTest Framework](https://github.com/OpenXiangShan/difftest) to verify the function.

To use is, compile the NEMU firstly:
```bash
make -C dependencies/difftest emu
```

Then use the binary file `xxx.bin`:
```bash
./build/emu -i xxx.bin
```

## Project Structure

### RTL

+ difftestSim
+ nucpu
+ axi

### Testers

+ nucpu.testers
+ axi.testers

## Architecture
