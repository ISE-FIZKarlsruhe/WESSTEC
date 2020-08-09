package TEST;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections15.map.FastHashMap;

public class MapTest {
	private static final int SIZE = 5000000;
	private static ExecutorService e = Executors.newFixedThreadPool(3);
	
	private static Map<String,Integer> map = new FastHashMap<>();
	
	public static void main(String[] args) throws InterruptedException {
		for(int i=0;i<SIZE;i++) {
			for(int j=0;j<SIZE;j++) {
				e.execute(handle(i,j));
			}
		}	

		e.shutdown();
		e.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		
		System.err.println("map size "+map.size());
	}

	private static Runnable handle(int i, int j) {
		return ()->{
			addOrIncreamet(String.valueOf(i), String.valueOf(j));
			};
	}

	private static void addOrIncreamet(String x,String y) {
		String key = x+"\t"+y;
		Integer integer = map.get(key);
		if(integer==null) {
			map.put(key, 1);
		}else {
			map.put(key, integer+1);
		}
		if(map.size()%10000==0) {
			System.err.println(map.size());
		}
	}

}
