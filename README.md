# Sysmips

A MIPS Technologies Malta system emulator, with a MIPS32 4Kc processor, Galileo GT-64120A northbridge and Intel PIIX4 southbridge

Intended to run Linux 3.2

Loosely based on the mipsem project (a user-level MIPS R2000A/Linux emulator)

This project is NOT officially associated with Imagination Technologies, Galileo Technology, Intel or any other company

## Suggested Reading

* [LinuxMIPS Wiki](http://www.linux-mips.org/wiki/MIPS_Malta)
* MIPS32 Architecture For Programmers Volume I: Introduction to the MIPS32 Architecture (c. 2001 for MIPS32 release 1)
* MIPS32 Architecture For Programmers Volume II: The MIPS32 Instruction Set (c. 2001)
* MIPS32 Architecture For Programmers Volume III: The MIPS32 Privileged Resource Architecture (c. 2001)
* MIPS32 4K Processor Core Family Software User's Manual (c. 2001-2002)
* Tool Interface Standard (TIS) Executable and Linking Format (ELF) Specification Version 1.2 (1995)
* MIPS Malta User's Manual (c. 2001-2002)
* MIPS YAMON User's Manual
* MIPS YAMON Reference Manual (c. 2001)
* Galileo GT-64120A System Controller Datasheet (2001)
* Intel 82371AB PCI-TO-ISA / IDE XCELERATOR (PIIX4) (1997)
* System V Application Binary Interface MIPS RISC Processor Supplement 3rd Edition (1996)

## Suggested Software

* [Linux 3.2 kernel image](https://packages.debian.org/stable/kernel/linux-image-3.2.0-4-4kc-malta), extract using ar -x *.deb; tar -xf data.tar.xz
* Linux 3.2.68 source code, particularly arch/mips/include/asm and arch/mips/mti-malta
* MIPS YAMON source code
* GRUB source code

## Screenshot

![Screenshot](https://dl.dropboxusercontent.com/u/8069847/sysmips.png)

## Download

[Runnable jar file](https://dl.dropboxusercontent.com/u/8069847/sysmips.jar)
