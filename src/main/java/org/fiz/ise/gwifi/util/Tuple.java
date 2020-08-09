package org.fiz.ise.gwifi.util;

public class Tuple {
	private String a;
	private String b;
	public Tuple() {
		this.a = "";
		this.b = "";
	}
	public Tuple(String a, String b) {
		super();
		this.a = a;
		this.b = b;
	}
	
	
	
	public void setA(String a) {
		this.a = a;
	}
	public void setB(String b) {
		this.b=b;
	}
	public String getA() {
		return a;
	}
	public String getB() {
		return b;
	}
	@Override
	public String toString() {
		return "Tuple [a=" + a + ", b=" + b + "]";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((a == null) ? 0 : a.hashCode());
		result = prime * result + ((b == null) ? 0 : b.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Tuple other = (Tuple) obj;
		if (a == null) {
			if (other.a != null)
				return false;
		} else if (!a.equals(other.a))
			return false;
		if (b == null) {
			if (other.b != null)
				return false;
		} else if (!b.equals(other.b))
			return false;
		return true;
	}

}
