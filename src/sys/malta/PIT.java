package sys.malta;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import sys.mips.Cpu;
import sys.mips.CpuConstants;
import sys.mips.CpuExceptionParams;
import sys.util.Logger;
import sys.util.Symbols;

/**
 * I8253 programmable interval timer
 */
public class PIT implements Device {
	
	public static final int M_COUNTER_0 = 0x0;
	public static final int M_COUNTER_1 = 0x1;
	public static final int M_COUNTER_2 = 0x2;
	public static final int M_TCW = 0x3;
	
	private final Logger log = new Logger("PIT");
	private final int baseAddr;
	
	private int timerCounter0;
	private int timerControlWord = -1;
	private int timerCounterByte;
	private Future<?> timerFuture;
	
	public PIT(int baseAddr) {
		this.baseAddr = baseAddr;
	}

	@Override
	public void init (Symbols sym) {
		sym.init(PIT.class, "M_", "M_PIT_", baseAddr, 1);
	}

	@Override
	public boolean isMapped (int addr) {
		final int offset = addr - baseAddr;
		return offset >= 0 && offset < 4;
	}

	@Override
	public int systemRead (int addr, int size) {
		final int offset = addr - baseAddr;
		throw new RuntimeException("unknown system read offset " + Integer.toHexString(offset));
	}

	@Override
	public void systemWrite (int addr, int size, int value) {
		final int offset = addr - baseAddr;
		switch (offset) {
			case M_TCW:
				timerControlWrite((byte) value);
				return;
				
			case M_COUNTER_0:
				timerCounter0Write((byte) value);
				return;
				
			default:
				throw new RuntimeException("unknown system write offset " + Integer.toHexString(offset));
		}
	}

	private void timerControlWrite (final int value) {
		log.println("timer control word write " + Integer.toHexString(value));
		// i8253.c init_pit_timer
		// 34 = binary, rate generator, r/w lsb then msb
		// 38 = software triggered strobe
		if (value == 0x34 || value == 0x38) {
			timerControlWord = value;
			timerCounterByte = 0;
		} else {
			throw new RuntimeException("unexpected tcw write " + Integer.toHexString(value));
		}
	}
	
	private void timerCounter0Write (final byte value) {
		log.println("timer counter 0 write " + Integer.toHexString(value));
		// lsb then msb
		// #define CLOCK_TICK_RATE 1193182
		// #define LATCH  ((CLOCK_TICK_RATE + HZ/2) / HZ)
		// default HZ_250
		// linux sets this to 12a5 (4773 decimal) = ((1193182 + 250/2) / 250)
		// hz = 1193182/(c-0.5)
		// dur = (c-0.5)/1193182
		
		if (timerCounterByte == 0) {
			timerCounter0 = value & 0xff;
			timerCounterByte++;
			
		} else if (timerCounterByte == 1) {
			timerCounter0 = (timerCounter0 & 0xff) | ((value & 0xff) << 8);
			timerCounterByte = 0;
			if (timerFuture != null) {
				timerFuture.cancel(false);
			}
			
			final Cpu cpu = Cpu.getInstance();
			final ScheduledExecutorService e = cpu.getExecutor();
			final CpuExceptionParams ep = new CpuExceptionParams(CpuConstants.EX_INTERRUPT, MaltaUtil.INT_SOUTHBRIDGE_INTR, MaltaUtil.IRQ_TIMER);
			final Runnable r = () -> cpu.addException(ep);
			
			if (timerControlWord == 0x34) {
				// counter never reaches 0...
				final double hz = 1193182.0 / (timerCounter0 - 1.5);
				final long durns = Math.round(1000000000.0 / hz);
				log.println("schedule pit at fixed rate " + hz + " hz " + (1.0/hz) + " s " + durns + " ns");
				timerFuture = e.scheduleAtFixedRate(r, durns, durns, TimeUnit.NANOSECONDS);
				
			} else if (timerControlWord == 0x38) {
				final double s = (timerCounter0 - 0.5) / 1193182.0;
				final long ns = (long) (s * 1_000_000_000.0);
				log.println("schedule pit once %.4f s %s ns", s, ns);
				timerFuture = e.schedule(r, ns, TimeUnit.NANOSECONDS);
			}
			
		} else {
			throw new RuntimeException("tcw write " + timerCounterByte);
		}
	}
}
