package org.fiz.ise.gwifi.util;

import java.util.concurrent.TimeUnit;

public class TimeUtil {
	public static long getStart() {
		return System.currentTimeMillis();
	}
	
	public static long getEnd(TimeUnit u,long start) {
		switch (u) {
		case MILLISECONDS:
			return System.currentTimeMillis()-start;
		case SECONDS:
			return TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()-start);
		case NANOSECONDS:
			return TimeUnit.NANOSECONDS.toNanos(System.currentTimeMillis()-start);
		case MINUTES:
			return TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis()-start);
		default:
			return 0;
		}
	}
}
