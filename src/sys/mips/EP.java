package sys.mips;

import sys.malta.MaltaUtil;

/**
 * cpu exception parameters
 */
public class EP {
	
	public final int excode;
	public final int interrupt;
	public final int irq;
	public final int vaddr;
	public final boolean isTlbRefill;
	
	public EP (int excode) {
		this(excode, -1, -1, -1, false);
	}
	
	public EP (int excode, int interrupt, int irq) {
		this(excode, interrupt, irq, -1, false);
	}
	
	public EP (int excode, int vaddr) {
		this(excode, -1, -1, vaddr, false);
	}
	
	public EP (int excode, int vaddr, boolean isTlbRefill) {
		this(excode, -1, -1, vaddr, isTlbRefill);
	}
	
	public EP (int excode, int interrupt, int irq, int vaddr, boolean isTlbRefill) {
		this.excode = excode;
		this.interrupt = interrupt;
		this.irq = irq;
		this.vaddr = vaddr;
		this.isTlbRefill = isTlbRefill;
	}
	
	@Override
	public String toString () {
		return "EP[" + IsnUtil.exceptionName(excode) + ", " + MaltaUtil.interruptName(interrupt) + ", " + MaltaUtil.irqName(irq) + ", " + Integer.toHexString(vaddr) + ", " + isTlbRefill + "]";
	}
}