package sys.malta;

public class MaltaUtil {
	
	/** southbridge timer */
	public static final int IRQ_TIMER = 0;
	/** superio keyboard */
	public static final int IRQ_KEYBOARD = 1;
	/** southbridge cascade */
	public static final int IRQ_CASCADE = 2;
	/** superio uart (tty1) */
	public static final int IRQ_UART0 = 3;
	/** superio uart (tty0) */
	public static final int IRQ_UART1 = 4;
	/** superio floppy disk */
	public static final int IRQ_FLOPPY = 6;
	/** superio parallel port */
	public static final int IRQ_PARALLEL = 7;
	/** southbridge real time clock */
	public static final int IRQ_RTC = 8;
	/** southbridge intelligent io controller bus */
	public static final int IRQ_I2C = 9;
	/** pci 1-4, ethernet */
	public static final int IRQ_PCI_AB = 10;
	/** pci 1-4, audio, usb */
	public static final int IRQ_PCI_CD = 11;
	/** superio mouse */
	public static final int IRQ_MOUSE = 12;
	/** primary ide */
	public static final int IRQ_IDE0 = 14;
	/** secondary ide */
	public static final int IRQ_IDE1 = 15;
	
	/** software interrupt */
	public static final int INT_SOFTWARE_0 = 0;
	/** software interrupt */
	public static final int INT_SOFTWARE_1 = 1;
	/** southbridge interrupt (INT0) */
	public static final int INT_SOUTHBRIDGE_INTR = 2;
	/** southbridge system management interrupt (INT1) */
	public static final int INT_SOUTHBRIDGE_SMI = 3;
	/** cbus uart (tty2) (INT2) */
	public static final int INT_CBUS_UART = 4;
	/** core card hi (INT3) */
	public static final int INT_COREHI = 5;
	/** core card lo (INT4) */
	public static final int INT_CORELO = 6;
	/** cpu internal timer (INT5) */
	public static final int INT_R4KTIMER = 7;
	
	public static String interruptName (int interrupt) {
		switch (interrupt) {
			case INT_SOFTWARE_0:
				return "SOFTWARE_0";
			case INT_SOFTWARE_1:
				return "SOFTWARE_1";
			// the Malta user guide calls these INT0 to INT5
			case INT_SOUTHBRIDGE_INTR:
				return "SOUTHBRIDGE_INTR";
			case INT_SOUTHBRIDGE_SMI:
				return "SOUTHBRIDGE_SMI";
			case INT_CBUS_UART:
				return "CBUS_UART";
			case INT_COREHI:
				return "COREHI";
			case INT_CORELO:
				return "CORELO";
			case INT_R4KTIMER:
				return "R4KTIMER";
			default:
				return null;
		}
	}
	
	public static String irqName (int irq) {
		switch (irq) {
			case IRQ_TIMER:
				return "TIMER";
			case IRQ_KEYBOARD:
				return "KEYBOARD";
			case IRQ_CASCADE:
				return "CASCADE";
			case IRQ_UART0:
				return "UART0";
			case IRQ_UART1:
				return "UART1";
			case IRQ_FLOPPY:
				return "FLOPPY";
			case IRQ_PARALLEL:
				return "PARALLEL";
			case IRQ_RTC:
				return "RTC";
			case IRQ_I2C:
				return "I2C";
			case IRQ_PCI_AB:
				return "PCI_AB";
			case IRQ_PCI_CD:
				return "PCI_CD";
			case IRQ_MOUSE:
				return "MOUSE";
			case IRQ_IDE0:
				return "IDE0";
			case IRQ_IDE1:
				return "IDE1";
			default:
				return null;
		}
	}
	
}