@set BIN=C:\mgc\embedded\codebench\bin
@set XGCC=%BIN%\mips-linux-gnu-gcc
@set XOBJDUMP=%BIN%\mips-linux-gnu-objdump
%XGCC% -Wall -O1 -mips32r2 -nostartfiles xsrc\test.c -o xbin\test
