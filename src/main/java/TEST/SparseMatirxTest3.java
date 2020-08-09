package TEST;

public class SparseMatirxTest3 {

	private static final int SIZE = 500000;
	private static final SparseMatrix m = new SparseMatrix(SIZE, SIZE);
	
	
	public static void main(String[] args) {
		System.err.println("we are here");
		for(int i=0;i<SIZE;i++) {
			for(int j=0;j<SIZE;j++) {
				m.add(i, j, i+j);
				System.out.println(j);
			}
			System.err.println(i);
		}
		
	}

}
