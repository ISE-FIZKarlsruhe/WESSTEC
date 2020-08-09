package TEST;

import java.util.concurrent.TimeUnit;

import org.fiz.ise.gwifi.util.TimeUtil;

public class PrintTest {

	public static void main(String[] args) {
		long now = TimeUtil.getStart();
		int x = 0;
		for(int i=0;i<100000;i++) {
			x=i;
			System.err.println(i);
		}
		System.err.println(TimeUtil.getEnd(TimeUnit.NANOSECONDS, now));
	}

}
