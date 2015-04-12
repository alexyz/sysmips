@set BIN=C:\mgc\embedded\codebench\bin
@set XGCC=%BIN%\mips-linux-gnu-gcc
@set XOBJDUMP=%BIN%\mips-linux-gnu-objdump

%XGCC% -Wall -nostartfiles -O1 xsrc\*.c -o xbin\a.out
%XOBJDUMP% -d xbin\a.out
