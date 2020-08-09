package TEST;
/**
 * @file
 *  Sparse matrix.
 *
 * Implementation with singly linked list, through the nested class SparseMatrixNode.
 * It's used nested class to improve encapsulation.
 *
 * @author Pedro Furtado
 */

public class SparseMatrix {

	/**
	 * Properties of sparse matrix.
	 */
	private int lines = 0;

	private int columns = 0;

	private int number_of_elements = 0;

	private SparseMatrixNode begin = null;

	/**
	 * Element of sparse matrix.
	 */
	public class SparseMatrixNode {

		public int key;

		public int x;

		public int y;

		public SparseMatrixNode next = null;

		SparseMatrixNode(int key, int x, int y) {

			this.key = key;
			this.x = x;
			this.y = y;
		}
	}

	/**
	 * Constructor method.
	 */
	public SparseMatrix(int l, int c) {

		this.lines = l;
		this.columns = c;
	}

	/**
	 * Search method.
	 *
	 * Search if there is an element with coordenates "x" and "y" in the sparse matrix.
	 *
	 * @param int x
	 *  Horizontal coordenate.
	 * @param int y
	 *  Vertical coordenate.
	 * @return SparseMatrixNode | null
	 *  Element founded or null, if not founded.
	 */
	public SparseMatrixNode search(int x, int y) {

		if (this.is_empty()) return null;

		SparseMatrixNode p = this.begin;

		while(p != null) {

			if ((p.x == x) && (p.y == y)) return p;

			if (p.x > x) return null;

			if ((p.x == x) && (p.y > y)) return null;

			p = p.next;
		}

		return null;
	}

	/**
	 * Search previous method.
	 *
	 * Search the previous of the element with coordenates "x" and "y" in the sparse matrix.
	 *
	 * @param int x
	 *  Horizontal coordenate.
	 * @param int y
	 *  Vertical coordenate.
	 * @return SparseMatrixNode | null
	 *  The return of this method must be interpreted together with search() method.
	 *  The method returns or the real previous of the element with the coordenates, if element exists,
	 *  or returns the intended previous of the element, if it is not present in the sparse matrix,
	 *  in order to be used to insert the element after the previous returned by this method.
	 */
	public SparseMatrixNode search_previous(int x, int y) {

		if (this.is_empty()) return null;

		SparseMatrixNode p = this.begin;

		while(p.next != null) {

			if ((p.next.x == x) && (p.next.y == y)) return p;

			if (p.next.x > x) return p;

			if ((p.next.x == x) && (p.next.y > y)) return p;

			p = p.next;
		}

		return p;
	}

	/**
	 * Add method.
	 *
	 * Add an element in the sparse matrix. It always insert a value in matrix,
	 * replacing an existing value or creating a new element inside matrix.
	 *
	 * @param int key
	 *  Integer key.
	 * @param int x
	 *  Horizontal coordenate.
	 * @param int y
	 *  Vertical coordenate.
	 * @return void
	 */
	public void add(int key, int x, int y) {

		if ((x < 0) || (y < 0)) return;

		if ((x >= this.lines) || (y >= this.columns)) {
			System.err.println("Size is not correct");
			return;
			} 

		SparseMatrixNode previous = this.search_previous(x, y);

		SparseMatrixNode element = this.search(x, y);

		// Empty sparse matrix.
		if ((previous == null) && (element == null)) {

			this.number_of_elements++;
			
			SparseMatrixNode node = new SparseMatrixNode(key, x, y);

			this.begin = node;
		}
		// First element.
		else if((previous == null) && (element != null)) {

			element.key = key;
		}
		else if((previous != null) && (element == null)) {

			// Last element.
			if (previous.next == null) {

				this.number_of_elements++;
				
				SparseMatrixNode node = new SparseMatrixNode(key, x, y);

				previous.next = node;
			}
			// Element in the middle.
			else {

				this.number_of_elements++;
				
				SparseMatrixNode node = new SparseMatrixNode(key, x, y);

				node.next = previous.next;
				previous.next = node;
			}	
		}
		// An element in the middle (or also in the end).
		else if((previous != null) && (element != null)) {

			element.key = key;
		}
	}
	
	/**
	 * Remove method.
	 *
	 * Remove an element in the sparse matrix.
	 *
	 * @param int x
	 *  Horizontal coordenate.
	 * @param int y
	 *  Vertical coordenate.
	 * @return int
	 *  Element key or -5 if there is not element with these coordenates.
	 */
	public int remove(int x, int y) {

		if (this.is_empty()) return -5;

		SparseMatrixNode node = this.search(x, y);

		if (node != null) {
			
			int key = node.key;

			SparseMatrixNode previous = this.search_previous(x, y);

			if (previous != null) {
				
				previous.next = node.next;
			}
			else {
				this.begin = node.next;
			}

			return key;
		}

		return -5;
	}

	/**
	 * Full method.
	 *
	 * Determines if the sparse matrix is full, i.e., if all coordenates of matrix are filled with some value.
	 *
	 * @return boolean
	 */
	public boolean is_full() {

		return this.number_of_elements == (this.lines * this.columns);
	}

	/**
	 * Empty method.
	 *
	 * Determines if the sparse matrix is empty, i.e., if it does not have any coordenate filled.
	 *
	 * @return boolean
	 */
	public boolean is_empty() {

		return this.number_of_elements == 0;
	}

	/**
	 * toString method.
	 *
	 * Provide some visual funcionality to see the elements inside the sparse matrix.
	 *
	 * @return String
	 *  Representation of the sparse matrix in the moment by a string.
	 */
	public String toString() {

		if (this.is_empty()) return "Empty sparse matrix.";

		SparseMatrixNode p = this.begin;

		String description = "Sparse Matrix: \n ";

		for (int i = 0; i < this.lines; i++) {
			
			for (int j = 0; j < this.columns; j++) {
				
				if ((p != null) && (p.x == i && p.y == j)) {
					
					description += String.format("%2d", p.key);
					p = p.next;
				}
				else {

					description += String.format("%2d", 0);
				}

				description += "  ";
			}

			description += " \n ";
		}

		description += "Elements: " + this.number_of_elements + " \n ";

		return description;
	}
}