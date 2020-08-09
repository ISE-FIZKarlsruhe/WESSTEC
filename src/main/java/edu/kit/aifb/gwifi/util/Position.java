package edu.kit.aifb.gwifi.util;

public class Position implements Comparable<Position> {

	private int start;
	private int end;

	/**
	 * Initializes a new position with the given start and end indexes.
	 * 
	 * @param start
	 *            the start index of this position
	 * @param end
	 *            the end index of this posion
	 */
	public Position(int start, int end) {
		this.start = start;
		this.end = end;
	}

	/**
	 * Identifies whether this position overlaps with another one.
	 * 
	 * @param pos
	 * @return true if the positions overlap, false otherwise.
	 */
	public boolean overlaps(Position pos) {
		return !(end <= pos.start || start >= pos.end);
	}

	/**
	 * @return the start index of this position
	 */
	public int getStart() {
		return start;
	}

	/**
	 * @return the end index of this position
	 */
	public int getEnd() {
		return end;
	}

	public String toString() {
		return "(" + start + "," + end + ")";
	}

	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof Position))
			return false;
		Position position = (Position) obj;
		if(compareTo(position) == 0)
			return true;
		else 
			return false;
	}

	public int hashCode() {
		int hash = 1;
		hash = hash * 17 + start;
		hash = hash * 31 + end;
		return hash;
	}

	public int compareTo(Position position) {
		// starts first, then goes first
		int c = new Integer(start).compareTo(position.getStart());
		if (c != 0)
			return c;
		// starts at same time, so longest one goes first
		c = new Integer(position.getEnd()).compareTo(end);
		if (c != 0)
			return c;
		return 0;
	}
	
}
