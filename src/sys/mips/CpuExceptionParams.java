package sys.mips;

import sys.malta.MaltaUtil;

/**
 * cpu exception parameters
 */
public class CpuExceptionParams {
	
	public final int excode;
	public final int interrupt;
	public final int irq;
	public final int vaddr;
	public final boolean isTlbRefill;
	
	public CpuExceptionParams (int excode) {
		this(excode, -1, -1, -1, false);
	}
	
	/** hardware interrupt */
	public CpuExceptionParams (int excode, int interrupt, int irq) {
		this(excode, interrupt, irq, -1, false);
	}
	
	/** virtual address error */
	public CpuExceptionParams (int excode, int vaddr) {
		this(excode, -1, -1, vaddr, false);
	}
	
	/** tlb error */
	public CpuExceptionParams (int excode, int vaddr, boolean isTlbRefill) {
		this(excode, -1, -1, vaddr, isTlbRefill);
	}
	
	private CpuExceptionParams (int excode, int interrupt, int irq, int vaddr, boolean isTlbRefill) {
		this.excode = excode;
		this.interrupt = interrupt;
		this.irq = irq;
		this.vaddr = vaddr;
		this.isTlbRefill = isTlbRefill;
	}
	
	@Override
	public String toString () {
		return getClass().getSimpleName() + "[ex=" + IsnUtil.exceptionName(excode) + " int=" + MaltaUtil.interruptName(interrupt) + " irq=" + MaltaUtil.irqName(irq) + " va=" + Integer.toHexString(vaddr) + " tlb=" + isTlbRefill + "]";
	}
}
