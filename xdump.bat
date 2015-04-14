@set BIN=C:\mgc\embedded\codebench\bin
@set XGCC=%BIN%\mips-linux-gnu-gcc
@set XOBJDUMP=%BIN%\mips-linux-gnu-objdump
%XOBJDUMP% -d xbin\test
