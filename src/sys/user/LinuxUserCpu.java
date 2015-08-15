package sys.user;

import sys.mips.*;
import sys.user.LinuxConstants.utsname;

import static sys.mips.MipsConstants.*;

class LinuxUserCpu extends Cpu {
	
	private static final int SYS_UNAME = 122;
	
	public LinuxUserCpu () {
		super(true);
	}
	
	@Override
	protected int lookup (int addr) {
		return addr;
	}
	
	@Override
	public void execExn (String type, int value) {
		if (type.equals(SYSCALL_EX)) {
			syscall(getRegister(REG_V0) - 4000);
		} else {
			super.execExn(type, value);
		}
	}
	
	private void syscall (int call) {
		getLog().debug("syscall " + call);
		int a = getRegister(REG_A0);
		switch (call) {
			case SYS_UNAME:
				uname(a);
				break;
			default:
				throw new CpuException("undefined linux syscall " + call);
		}
		// XXX no errors...
		setRegister(REG_V0, 0);
		setRegister(REG_A3, 0);
	}
	
	private void storeString (int a, int[] f, String value) {
		if (value.length() > f[1]) {
			throw new RuntimeException();
		}
		MemoryUtil.storeString(getMemory(), a + f[0], value);
	}

	private void uname (int a) {
		getLog().debug("uname " + Integer.toHexString(a));
		// u.sysname=Linux
		// u.nodename=ci20-2927
		// u.release=3.0.8-12439-gf697891
		// u.version=#98 SMP PREEMPT Thu Sep 11 15:52:29 BST 2014
		// u.machine=mips
		storeString(a, utsname.sysname, "Linux");
		storeString(a, utsname.nodename, "usermips");
		storeString(a, utsname.release, "3.0.8");
		storeString(a, utsname.version, "#1");
		storeString(a, utsname.machine, "mips");
	}
	
}