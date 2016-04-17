package sys.mips;

import sys.malta.MaltaUtil;

/**
 * cpu exception parameters
 */
public class CpuExceptionParams {
	
	public final int excode;
	public final Integer interrupt;
	public final Integer irq;
	public final Integer vaddr;
	public final Boolean tlbRefill;
	
	public CpuExceptionParams (int excode) {
		this(excode, null, null, null, null);
	}
	
	/** hardware interrupt */
	public CpuExceptionParams (int excode, int interrupt, int irq) {
		this(excode, interrupt, irq, null, null);
	}
	
	/** virtual address error */
	public CpuExceptionParams (int excode, int vaddr) {
		this(excode, null, null, vaddr, null);
	}
	
	/** tlb error */
	public CpuExceptionParams (int excode, int vaddr, boolean isTlbRefill) {
		this(excode, null, null, vaddr, isTlbRefill);
	}
	
	private CpuExceptionParams (int excode, Integer interrupt, Integer irq, Integer vaddr, Boolean isTlbRefill) {
		if (interrupt != null && (interrupt < 0 || interrupt >= 16)) {
			throw new RuntimeException("invalid interrupt " + interrupt);
		}
		if (irq != null && (irq < 0 || irq >= 16)) {
			throw new RuntimeException("invalid irq " + irq);
		}
		this.excode = excode;
		this.interrupt = interrupt;
		this.irq = irq;
		this.vaddr = vaddr;
		this.tlbRefill = isTlbRefill;
	}
	
	@Override
	public String toString () {
		String exs = InstructionUtil.exceptionName(excode);
		String ints = interrupt != null ? " int=" + MaltaUtil.interruptName(interrupt) : "";
		String irqs = irq != null ? " irq=" + MaltaUtil.irqName(irq) : "";
		String vas = vaddr != null ? " vaddr=" + Integer.toHexString(vaddr) : "";
		String tlbs = tlbRefill != null ? " refill=" + tlbRefill : "";
		return getClass().getSimpleName() + "[ex=" + exs + ints + irqs + vas + tlbs + "]";
	}
}
