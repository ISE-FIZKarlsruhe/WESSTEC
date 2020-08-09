package TEST;

import org.ejml.data.DMatrixSparseTriplet;

public class SparseMatrixTest2 {

	public static void main(String[] args) throws InterruptedException {
		//Thread.sleep(5000);
		
		int size = 500000;
		DMatrixSparseTriplet work = new DMatrixSparseTriplet();
		//DMatrixSparseTriplet work = new DMatrixSparseTriplet(size,size,size*size);
		
		System.err.println("XXXX");
		for(int i=0;i<size;i++) {
			for(int j=0;j<size;j++) {
				work.addItem(i, j, i);
			}
			//System.err.println(i);
		}
		System.err.println(work.getNumElements());
		System.err.println(work.get(1, 1));
//		
//		
//		
//		System.err.println(work.unsafe_get(1, 2));
	}

}

