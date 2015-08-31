package sys.mips;

import java.beans.PropertyChangeSupport;
import java.util.Calendar;

/**
 * this class kind-of represents the southbridge, but it is also doing device
 * mapping
 */
public class Malta implements Device {
	
	public static final int M_SDRAM = 0x0;
	public static final int M_UNCACHED_EX_H = 0x100;
	
	public static final int M_PCI1 = 0x0800_0000;
	
	// connected with value of [GT_PCI0IOLD]
	public static final int M_PIIX4 = 0x1000_0000;
	
	// 82371AB (PIIX4) 3.1.2. IO SPACE REGISTERS
	public static final int M_PIC_MASTER_CMD = M_PIIX4 + 0x20;
	public static final int M_PIC_MASTER_IMR = M_PIIX4 + 0x21;
	
	// programmable interval timer
	public static final int M_I8253_COUNTER_0 = M_PIIX4 + 0x40;
	public static final int M_I8253_COUNTER_1 = M_PIIX4 + 0x41;
	public static final int M_I8253_COUNTER_2 = M_PIIX4 + 0x42;
	public static final int M_I8253_TCW = M_PIIX4 + 0x43;
	
	public static final int M_RTCADR = M_PIIX4 + 0x70;
	public static final int M_RTCDAT = M_PIIX4 + 0x71;
	public static final int M_PIC_SLAVE_CMD = M_PIIX4 + 0xa0;
	public static final int M_PIC_SLAVE_IMR = M_PIIX4 + 0xa1;
	public static final int M_DMA2_MASK_REG = M_PIIX4 + 0xD4;
	
	// the first uart is on the isa bus connected to the PIIX4
	public static final int M_PORT = M_PIIX4 + 0x03f8;
	public static final int M_UART_TX = M_PORT;
	public static final int M_UART_LSR = M_PORT + 5;
	
	public static final int M_PCI2 = 0x1800_0000;
	
	public static final int M_GTBASE = 0x1be0_0000;
	
	public static final int M_CBUS = 0x1c00_0000;
	
	public static final int M_MONITORFLASH = 0x1e00_0000;
	public static final int M_RESERVED = 0x1e40_0000;
	
	public static final int M_DEVICES = 0x1f00_0000;
	public static final int M_DISPLAY = 0x1f00_0400;
	public static final int M_DISPLAY_LEDBAR = M_DISPLAY + 0x8;
	public static final int M_DISPLAY_ASCIIWORD = M_DISPLAY + 0x10;
	public static final int M_DISPLAY_ASCIIPOS = M_DISPLAY + 0x18;
	
	public static final int M_SCSPEC1 = 0x1f10_0010;
	
	public static final int M_BOOTROM = 0x1fc0_0000;
	public static final int M_REVISION = 0x1fc0_0010;
	
	public static final int M_SCSPEC2 = 0x1fd0_0010;
	public static final int M_SCSPEC2_BONITO = 0x1fe0_0010;
	
	private static int indexToCalendar (int index) {
		switch (index) {
			case 0: 
				return Calendar.SECOND;
			case 2: 
				return Calendar.MINUTE;
			case 4:
				// depends on control register b hour format
				return Calendar.HOUR;
			case 6: 
				return Calendar.DAY_OF_WEEK;
			case 7: 
				return Calendar.DAY_OF_MONTH;
			case 8: 
				return Calendar.MONTH;
			case 9: 
				return Calendar.YEAR;
			default: 
				throw new RuntimeException("invalid index " + index);
		}
	}
	
	// this should probably live on the cpu
	private final PropertyChangeSupport support = new PropertyChangeSupport(this);
	private final StringBuilder consoleSb = new StringBuilder();
	// the GT is the northbridge
	private final GT gt = new GT();
	private final IOMemory iomem = new IOMemory();
	
	private int offset;
	private int counter0;
	
	public Malta () {
		iomem.putWord(M_REVISION, 1);
		iomem.putByte(M_UART_LSR, (byte) 0x20);
		iomem.putWord(M_DISPLAY_LEDBAR, 0);
		iomem.putWord(M_DISPLAY_ASCIIWORD, 0);
		for (int n = 0; n < 8; n++) {
			iomem.putWord(M_DISPLAY_ASCIIPOS + (n * 8), 0);
		}
	}
	
	@Override
	public void init (Symbols sym, int offset) {
		System.out.println("init malta at " + Integer.toHexString(offset));
		this.offset = offset;
		
		sym.put(offset + M_SDRAM, "M_SDRAM");
		sym.put(offset + M_UNCACHED_EX_H, "M_UNCACHED_EX_H", 0x80);
		sym.put(offset + M_PCI1, "M_PCI1");
		sym.put(offset + M_PIIX4, "M_PIIX4");
		sym.put(offset + M_PIC_MASTER_CMD, "M_PIC_MASTER_CMD", 1);
		sym.put(offset + M_PIC_MASTER_IMR, "M_PIC_MASTER_IMR", 1);
		sym.put(offset + M_I8253_COUNTER_0, "M_I8253_COUNTER_0", 1);
		sym.put(offset + M_I8253_COUNTER_1, "M_I8253_COUNTER_1", 1);
		sym.put(offset + M_I8253_COUNTER_2, "M_I8253_COUNTER_2", 1);
		sym.put(offset + M_I8253_TCW, "M_I8253_TCW", 1);
		sym.put(offset + M_RTCADR, "M_RTCADR");
		sym.put(offset + M_RTCDAT, "M_RTCDAT");
		sym.put(offset + M_PIC_SLAVE_CMD, "M_PIC_SLAVE_CMD", 1);
		sym.put(offset + M_PIC_SLAVE_IMR, "M_PIC_SLAVE_IMR", 1);
		sym.put(offset + M_DMA2_MASK_REG, "M_DMA2_MASK_REG");
		sym.put(offset + M_PORT, "M_PORT", 0x10000);
		sym.put(offset + M_UART_LSR, "M_UART_LSR", 1);
		sym.put(offset + M_PCI2, "M_PCI2");
		sym.put(offset + M_GTBASE, "M_GTBASE");
		sym.put(offset + M_MONITORFLASH, "M_MONITORFLASH");
		sym.put(offset + M_RESERVED, "M_RESERVED");
		sym.put(offset + M_DEVICES, "M_DEVICES");
		sym.put(offset + M_DISPLAY, "M_DISPLAY");
		sym.put(offset + M_SCSPEC1, "M_SCSPEC1");
		sym.put(offset + M_SCSPEC2, "M_SCSPEC2");
		sym.put(offset + M_SCSPEC2_BONITO, "M_SCSPEC2_BONITO");
		sym.put(offset + M_CBUS, "M_CBUS");
		sym.put(offset + M_BOOTROM, "M_BOOTROM");
		sym.put(offset + M_REVISION, "M_REVISION", 8);
		
		gt.init(sym, offset + M_GTBASE);
	}
	
	@Override
	public int systemRead (int addr, int size) {
		if (addr >= M_GTBASE && addr < M_GTBASE + 0x1000) {
			return gt.systemRead(addr - M_GTBASE, size);
		}

//		final CpuLogger log = Cpu.getInstance().getLog();
//		log.debug("malta read " + getName(addr) + " size " + size);
		
		return iomem.get(addr, size);
	}
	
	@Override
	public void systemWrite (final int addr, final int value, int size) {
		if (addr >= M_GTBASE && addr < M_GTBASE + 0x1000) {
			gt.systemWrite(addr - M_GTBASE, value, size);
			return;
		}
		
		final CpuLogger log = CpuLogger.getInstance();
		//log.debug("malta write " + getName(addr) + " <= " + Integer.toHexString(value) + " size " + size);
		
		iomem.put(addr, value, size);
		
		switch (addr) {
			case M_I8253_TCW: {
				// i8253.c init_pit_timer
				log.info("tcw write " + Integer.toHexString(value));
				// 34 = binary, rate generator, r/w lsb then msb
				if (value != 0x34) {
					throw new RuntimeException("unexpected tcw write " + Integer.toHexString(value));
				}
				return;
			}
			
			case M_I8253_COUNTER_0: {
				// lsb then msb
				// #define CLOCK_TICK_RATE 1193182
				// #define LATCH  ((CLOCK_TICK_RATE + HZ/2) / HZ)
				// default HZ_250
				// linux sets this to 12a5 (4773 decimal) = ((1193182 + 250/2) / 250)
				counter0 = ((value << 8) | (counter0 >>> 8)) & 0xffff;
				log.info("counter 0 now " + Integer.toHexString(counter0));
				return;
			}
			
			case M_RTCADR: {
				// mc146818rtc.h
				// 0 = seconds, 2 = minutes, 4 = hours, 6 = dow, 7 = dom, 8 = month, 9 = year
				//log.debug("rtc adr write");
				if (value == 0xa) {
					boolean uip = (System.currentTimeMillis() % 1000) >= 990;
					iomem.putByte(M_RTCDAT, (byte) (uip ? 0x80 : 0));
				} else if (value == 0xb) {
					iomem.putByte(M_RTCDAT, (byte) 4);
				} else {
					final int f = indexToCalendar(value);
					iomem.putByte(M_RTCDAT, (byte) Calendar.getInstance().get(f));
				}
				return;
			}
			
			case M_RTCDAT: {
				int adr = iomem.getByte(M_RTCADR);
				if (adr == 0xb && value == 4) {
					// set mode binary
					return;
				}
				throw new RuntimeException("unexpected rtc adr " + adr + " dat write: " + value);
			}
			
			case M_DMA2_MASK_REG:
				// information in asm/dma.h
				log.info("enable dma channel 4+" + value);
				break;
				
			case M_UART_TX:
				consoleWrite(value);
				break;
				
			case M_PIC_MASTER_CMD:
				log.info("pic master cmd " + Integer.toHexString(value & 0xff));
				break;
				
			case M_PIC_MASTER_IMR:
				log.info("pic master imr " + Integer.toHexString(value & 0xff));
				break;
				
			case M_PIC_SLAVE_CMD:
				log.info("pic slave cmd " + Integer.toHexString(value & 0xff));
				break;
				
			case M_PIC_SLAVE_IMR:
				log.info("pic slave imr " + Integer.toHexString(value & 0xff));
				break;
				
			default:
				if (addr >= M_UNCACHED_EX_H && addr < M_UNCACHED_EX_H + 0x100) {
					log.debug("set uncached exception handler " + Symbols.getInstance().getName(offset + addr) + " <= " + Integer.toHexString(value));
					break;
					
				} else if (addr >= M_DISPLAY && addr < M_DISPLAY + 0x100) {
					support.firePropertyChange("display", null, displayText());
					break;
				}
				
				throw new RuntimeException("unknown system write " + Symbols.getInstance().getName(offset + addr) + " <= " + Integer.toHexString(value));
		}
		
	}
	
	private void consoleWrite (int value) {
		if ((value >= 32 && value < 127) || value == '\n') {
			consoleSb.append((char) value);
		} else if (value != '\r') {
			consoleSb.append("{" + Integer.toHexString(value) + "}");
		}
		if (value == '\n' || consoleSb.length() > 80) {
			support.firePropertyChange("console", null, consoleSb.toString());
			consoleSb.delete(0, consoleSb.length());
		}
	}
	
	public PropertyChangeSupport getSupport () {
		return support;
	}
	
	public String displayText() {
		final StringBuilder sb = new StringBuilder();
		final int leds = iomem.getWord(M_DISPLAY_LEDBAR) & 0xff;
		sb.append(Integer.toBinaryString(leds)).append(" ");
		final int word = iomem.getWord(M_DISPLAY_ASCIIWORD);
		sb.append(Integer.toHexString(word)).append(" ");
		for (int n = 0; n < 8; n++) {
			int w = iomem.getWord(M_DISPLAY_ASCIIPOS + (n * 8)) & 0xff;
			sb.append(w != 0 ? (char) w : ' ');
		}
		return sb.toString();
	}
	
}
