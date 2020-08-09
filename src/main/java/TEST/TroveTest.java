package TEST;

import cern.colt.map.OpenIntIntHashMap;
import cern.colt.map.OpenIntObjectHashMap;

public class TroveTest {

	public static void main(String[] args) {
		OpenIntIntHashMap a = new OpenIntIntHashMap();
		
		int size = 50000;
		
		int k=0;
		for(int i=0;i<size;i++) {
			for(int j=0;j<size;j++) {
				a.put(k++, i+j);
			}
			System.err.println(i);
		}
		
		


	}

}
