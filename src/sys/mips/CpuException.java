package sys.mips;

import sys.malta.MaltaUtil;

/** internal exception */
public class CpuException extends RuntimeException {
	
	public final int extype;
	public final int interrupt;
	public final int irq;
	public final int vaddr;
	public final boolean tlbRefill;

	public CpuException (int extype, final int interrupt, final int irq, int vaddr, boolean tlbRefill) {
		this.extype = extype;
		this.interrupt = interrupt;
		this.irq = irq;
		this.vaddr = vaddr;
		this.tlbRefill = tlbRefill;
	}
	
	@Override
	public String toString () {
		return "CpuException[" + IsnUtil.exceptionName(extype) + ", " + MaltaUtil.interruptName(interrupt) + ", " + MaltaUtil.irqName(irq) + ", " + Integer.toHexString(vaddr) + ", " + tlbRefill + "]";
	}
}
