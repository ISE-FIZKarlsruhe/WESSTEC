package TEST;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.fiz.ise.gwifi.util.TimeUtil;

public class MemoryTest {
	public static void main(String[] args) throws InterruptedException {
		long mainStart = TimeUtil.getStart();
		Map<String, Long> map = new HashMap<>();
		for (int i = 0; i < 16734; i++) {
			for (int j = 0; j < 16734; j++) {
				String random = generateString();
				
				map.put(random,new Date().getTime());
			}
		}
		System.out.println("main dk "+(TimeUtil.getEnd(TimeUnit.SECONDS, mainStart)/60));
		System.out.println("Sizeof the map "+map.size());
	}	
	    

	    public static String generateString() {
	        String uuid = UUID.randomUUID().toString();
	        return "uuid = " + uuid;
	    }

}
