package TEST;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;

public class Test {
	private List<Map> maps = new ArrayList<>();
	
	
	private static final int SIZE = 5000000;
	private static Map<String,Integer> map = new ConcurrentHashMap<>();
	public static void main(String[] args) {
		
		System.out.println(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Shareholder"));
		long now  = System.currentTimeMillis();
		for(int i=0;i<SIZE;i++) {
			for(int j=0;j<SIZE;j++) { 
				//map.put(i+"\t"+j, i+j);
				
			}
			System.err.println(i);
		}
		
		System.err.println(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()-now));
		
		
		//List<String> collect = map.entrySet().parallelStream().map(entry -> new String(entry.getKey()+"\t"+entry.getValue())).collect(Collectors.toList());
		
		//collect.forEach(rima->System.err.println(rima));

	}

}
