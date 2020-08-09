package TEST;

import jeigen.SparseMatrixLil;
import static jeigen.Shortcuts.*;

public class JMatirxTest {

	public static void main(String[] args) {
		SparseMatrixLil sm1;
		int size = 50000;
		sm1 = spzeros(size,size); // creates an empty 5*3 sparse matrix
		
		
		for(int i=0;i<size;i++) {
			for(int j=0;j<size;j++) {
				sm1.append(i, j, i+j);
			}
			System.err.println(i);
		}
		
		
		System.err.println(sm1.toString());
	}

}
