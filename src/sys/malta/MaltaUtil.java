package sys.malta;

public class MaltaUtil {
	
	public static final int IRQ_TIMER = 0;
	public static final int IRQ_KEYBOARD = 1;
	public static final int IRQ_CASCADE = 2;
	public static final int IRQ_UART0 = 3;
	public static final int IRQ_UART1 = 4;
	public static final int IRQ_FLOPPY = 6;
	public static final int IRQ_PARALLEL = 7;
	public static final int IRQ_RTC = 8;
	public static final int IRQ_I2C = 9;
	public static final int IRQ_PCI_AB = 10;
	public static final int IRQ_PCI_CD = 11;
	public static final int IRQ_MOUSE = 12;
	public static final int IRQ_IDE0 = 14;
	public static final int IRQ_IDE1 = 15;
	
	public static final int INT_SW0 = 0;
	public static final int INT_SW1 = 1;
	public static final int INT_SB_INTR = 2;
	public static final int INT_SB_SMI = 3;
	public static final int INT_CBUS_UART = 4;
	public static final int INT_COREHI = 5;
	public static final int INT_CORELO = 6;
	public static final int INT_R4KTIMER = 7;
	
	public static String interruptName (int interrupt) {
		switch (interrupt) {
			case INT_SW0:
				return "Software0";
			case INT_SW1:
				return "Software1";
			// the Malta user guide calls these INT0 to INT5
			case INT_SB_INTR:
				return "SouthBridgeINTR";
			case INT_SB_SMI:
				return "SouthBridgeSMI";
			case INT_CBUS_UART:
				return "CBUS-UART";
			case INT_COREHI:
				return "COREHI";
			case INT_CORELO:
				return "CORELO";
			case INT_R4KTIMER:
				return "R4KTimer";
			default:
				return null;
		}
	}
	
	public static String irqName (int irq) {
		switch (irq) {
			case IRQ_TIMER:
				return "Timer";
			case IRQ_KEYBOARD:
				return "Keyboard";
			case IRQ_CASCADE:
				return "Cascade";
			case IRQ_UART0:
				return "Uart0";
			case IRQ_UART1:
				return "Uart1";
			case IRQ_FLOPPY:
				return "Floppy";
			case IRQ_PARALLEL:
				return "Parallel";
			case IRQ_RTC:
				return "RealTimeClock";
			case IRQ_I2C:
				return "I2C";
			case IRQ_PCI_AB:
				return "PCI-AB";
			case IRQ_PCI_CD:
				return "PCI-CD";
			case IRQ_MOUSE:
				return "Mouse";
			case IRQ_IDE0:
				return "IDE0";
			case IRQ_IDE1:
				return "IDE1";
			default:
				return null;
		}
	}
	
}