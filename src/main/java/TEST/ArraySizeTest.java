package TEST;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import TEST.SparseMatrix.SparseMatrixNode;

public class ArraySizeTest {
	private static final int SIZE = 1000;
	private static final SparseMatrix m = new SparseMatrix(SIZE, SIZE);
	
	private static Map<String,Integer> map = new ConcurrentHashMap<>();
	private static AtomicLong ID = new AtomicLong(1);
	
	private static ExecutorService e = Executors.newFixedThreadPool(4);

	public static void main(String[] args) throws InterruptedException {
		for(int i=0;i<SIZE;i++) {
			for(int j=i+1;j<SIZE;j++) {
				System.err.println(i+" "+j);
				e.execute(handle(i));
			}
		}	
		e.shutdown();
		e.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		for(int i=0;i<2000;i++) {
			SparseMatrixNode search = m.search(i, i);
			if(search==null) {

			}else {
				System.err.println(i+" with "+ i + " == "+search.key);
			}
		}
		for(int i=0;i<SIZE;i++) {
			for(int j=0;j<SIZE;j++) {
				m.search(i, j);
			}	
		}
	}

	private static Runnable handle(int i) {
		return ()->{
			addOrIncreamet(i, i);
			};
	}
	private static synchronized void addOrIncreamet(int x,int y) {		
		final SparseMatrixNode search = m.search(x, y);
		if(search==null) {
			m.add(1, x, y);
		}else {
			m.add(search.key+1, x, y);
		}
		System.out.println(x+" "+y);
	}
}

