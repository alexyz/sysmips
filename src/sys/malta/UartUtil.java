package sys.malta;

/**
 * uart constants
 */
public class UartUtil {

	/** receive (read) / transmit (write) */
	public static final int M_RX_TX = 0;
	/** interrupt enable */
	public static final int M_IER = 1;
	/**
	 * fifo control register (writes) / interrupt identification register
	 * (reads) / extended features register (not supported)
	 */
	public static final int M_FCR_IIR_EFR = 2;
	/** line control register */
	public static final int M_LCR = 3;
	/** modem control register */
	public static final int M_MCR = 4;
	/** line status register */
	public static final int M_LSR = 5;
	/** modem status register */
	public static final int M_MSR = 6;
	// scratch register?
	
	public static int lcrWordLength(int x) {
		return (x & 0x3) + 5;
	}
	
	public static float lcrStopBits(int x) {
		if ((x & 0x4) == 0) {
			return 1;
		} else {
			if ((x & 0x3) == 0) {
				return 1.5f;
			} else {
				return 2;
			}
		}
	}
	
	public static char lcrParity(int x) {
		if ((x & 0x8) == 0) {
			return 'N';
		} else switch ((x & 0x30) >> 4) {
			case 0: return 'O';
			case 1: return 'E';
			case 2: return 'H';
			case 3: return 'L';
			default: throw new RuntimeException();
		}
	}
	
	/** break control bit */
	public final static int LCR_BREAK = 0x40;
	/** divisor latch access bit */
	public final static int LCR_DLAB = 0x80;
	
	/** force data terminal ready */
	public final static int MCR_DTR = 0x1;
	/** request to send */
	public final static int MCR_RTS = 0x2;
	public final static int MCR_OUTPUT1 = 0x4;
	/** enable uart interrupts? */
	public final static int MCR_OUTPUT2 = 0x8;
	/** loopback mode */
	public final static int MCR_LOOPBACK = 0x10;
	
	/** enable fifo */
	public final static int FCR_ENABLE_FIFO = 0x1;
	/** clear receive fifo */
	public static final int FCR_CLEAR_RCVR = 0x2;
	/** clear transmit fifo */
	public static final int FCR_CLEAR_XMIT = 0x4;
	
	/** data ready to read */
	public static final int LSR_DR = 0x1;
	/** overrun error */
	public static final int LSR_OE = 0x2;
	public static final int LSR_PE = 0x4;
	public static final int LSR_FE = 0x8;
	public static final int LSR_BI = 0x10;
	/** transmitter holding register empty (or xmit fifo empty) */
	public static final int LSR_THRE = 0x20;
	public static final int LSR_TEMT = 0x40;
	
	/** fifo control is enabled or receiver fifo trigger level */
	public static final int IIR_FIFO = 0xc0;
	
	/** received data available interrupt */
	public static final int IER_RDAI = 0x1;
	/** transmitter holding register empty interrupt */
	public static final int IER_THREI = 0x2;
	/** received line status interrupt */
	public static final int IER_RLSI = 0x4;
	/** modem status interrupt */
	public static final int IER_MSI = 0x8;
	
	public UartUtil() {
		//
	}
}
