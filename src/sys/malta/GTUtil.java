package sys.malta;

/**
 * GT Constants
 */
public class GTUtil {

	/** SCS[1:0]* Low Decode Address */
	public static final int GT_SCS10LD = 0x8;
	/** SCS[1:0]* High Decode Address */
	public static final int GT_SCS10HD = 0x10;
	/** SCS[3:2]* Low Decode Address */
	public static final int GT_SCS32LD = 0x18;
	/** SCS[3:2]* High Decode Address */
	public static final int GT_SCS32HD = 0x20;
	/** CS[2:0]* Low Decode Address */
	public static final int GT_CS20LD = 0x28;
	/** CS[2:0]* High Decode Address */
	public static final int GT_CS20HD = 0x30;
	/** CS[3]* & Boot CS* Low Decode Address */
	public static final int GT_CS3BOOTLD = 0x38;
	/** CS[3]* & Boot CS* High Decode Address */
	public static final int GT_CS3BOOTHD = 0x40;
	/** PCI_0 I/O Low Decode Address */
	// setting this affects ioremap
	public static final int GT_PCI0IOLD = 0x48;
	/** PCI_0 I/O High Decode Address */
	public static final int GT_PCI0IOHD = 0x50;
	/** PCI_0 Memory 0 Low Decode Address */
	public static final int GT_PCI0M0LD = 0x58;
	/** PCI_0 Memory 0 High Decode Address */
	public static final int GT_PCI0M0HD = 0x60;
	/** Internal Space Decode */
	public static final int GT_ISD = 0x68;
	/** PCI_0 Memory 1 Low Decode Address */
	public static final int GT_PCI0M1LD = 0x80;
	/** PCI_0 Memory 1 High Decode Address */
	public static final int GT_PCI0M1HD = 0x88;
	/** PCI_1 I/O Low Decode Address */
	public static final int GT_PCI1IOLD = 0x90;
	/** PCI_1 I/O High Decode Address */
	public static final int GT_PCI1IOHD = 0x98;
	/** PCI_1 Memory 0 Low Decode Address */
	public static final int GT_PCI1M0LD = 0xa0;
	/** PCI_1 Memory 0 High Decode Address */
	public static final int GT_PCI1M0HD = 0xa8;
	/** PCI_1 Memory 1 Low Decode Address */
	public static final int GT_PCI1M1LD = 0xb0;
	/** PCI_1 Memory 1 High Decode Address */
	public static final int GT_PCI1M1HD = 0xb8;
	/** SCS[1:0]* Address Remap */
	public static final int GT_SCS10AR = 0xd0;
	/** SCS[3:2]* Address Remap */
	public static final int GT_SCS32AR = 0xd8;
	/** CS[2:0]* Address Remap */
	public static final int GT_CS20R = 0xe0;
	/** CS[3]* & Boot CS* Address Remap */
	public static final int GT_CS3BOOTR = 0xe8;
	/** PCI_0 IO Address Remap */
	public static final int GT_PCI0IOREMAP = 0xf0;
	/** PCI_0 Memory 0 Address Remap */
	public static final int GT_PCI0M0REMAP = 0xf8;
	/** PCI_0 Memory 1 Address Remap */
	public static final int GT_PCI0M1REMAP = 0x100;
	/** PCI_1 IO Address Remap */
	public static final int GT_PCI1IOREMAP = 0x108;
	/** PCI_1 Memory 0 Address Remap */
	public static final int GT_PCI1M0REMAP = 0x110;
	/** PCI_1 Memory 1 Address Remap */
	public static final int GT_PCI1M1REMAP = 0x118;
	/** PCI_1 Interrupt Acknowledge Virtual Register */
	public static final int GT_PCI1_IACK = 0xc30;
	/** PCI_0 Interrupt Acknowledge Virtual Register */
	public static final int GT_PCI0_IACK = 0xc34;
	/** PCI_0 Command */
	public static final int GT_PCI0_CMD = 0xc00;
	/** Interrupt Cause Register (GT_INTRCAUSE_OFS) */ 
	public static final int GT_INTERRUPT_CAUSE = 0xc18;
	/** CPU Interrupt Mask Register */
	public static final int GT_CPU_INTERRUPT_MASK = 0xc1c;
	/** PCI_0 Interrupt Cause Mask Register */
	public static final int GT_PCI0_ICM = 0xc24;
	/** PCI_0 SErr0 Mask */
	public static final int GT_PCI0_SERR0 = 0xc28;
	/** CPU Select Cause Register */ 
	public static final int GT_CPU_SC = 0xc70;
	/** PCI_0 Interrupt Select Register */ 
	public static final int GT_PCI0_ISC = 0xc74;
	/** High Interrupt Cause Register */ 
	public static final int GT_ICH = 0xc98;
	/** CPU High Interrupt Mask Register */ 
	public static final int GT_CPU_IMH = 0xc9c;
	/** PCI_0 High Interrupt Cause Mask Register */
	public static final int GT_PCI0_ICMH = 0xca4;
	/** PCI_1 SErr1 Mask */
	public static final int GT_PCI1_SERR1 = 0xca8;
	/** PCI_1 Configuration Address */
	public static final int GT_PCI1_CFGADDR = 0xcf0;
	/** PCI_1 Configuration Data Virtual Register */
	public static final int GT_PCI1_CFGDATA = 0xcf4;
	/** PCI_0 Configuration Address */
	public static final int GT_PCI0_CFGADDR = 0xcf8;
	/** PCI_0 Configuration Data Virtual Register */
	public static final int GT_PCI0_CFGDATA = 0xcfc;
	
	public static int reg (final int cfgaddr) {
		return (cfgaddr >>> 2) & 0x3f;
	}

	public static int func (final int cfgaddr) {
		return (cfgaddr >>> 8) & 0x7;
	}

	public static int dev (final int cfgaddr) {
		return (cfgaddr >>> 11) & 0x1f;
	}

	public static int bus (final int cfgaddr) {
		return (cfgaddr >>> 16) & 0xff;
	}

	public static int en (final int cfgaddr) {
		return (cfgaddr >>> 31) & 0x1;
	}
	
}
