package TEST;

import java.io.IOException;

public class LargeMatrixTest {

	public static void main(String[] args) throws IOException {
		long start = System.nanoTime();
	    final long used0 = usedMemory();
	    int size = 500000;
	    
		LargeDoubleMatrix matrix = new LargeDoubleMatrix("/tmp/ldm.test", size, size);
	    for(int i=0;i<size;i++) {
	        matrix.set(i,i,i);
	        System.err.println(i);
	    }
	    
	    //for(int i=0;i<matrix.width();i++);
	        
	    long time = System.nanoTime() - start;
	    final long used = usedMemory() - used0;
	    if (used==0)
	        System.err.println("You need to use -XX:-UsedTLAB to see small changes in memory usage.");
	    System.out.printf("Setting the diagonal took %,d ms, Heap used is %,d KB%n", time/1000/1000, used/1024);
	    matrix.close();

	}

	private static long usedMemory() {
	    return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
	}
}
