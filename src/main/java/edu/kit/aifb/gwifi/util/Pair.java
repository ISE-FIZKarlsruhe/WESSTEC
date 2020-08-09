package edu.kit.aifb.gwifi.util;

public class Pair<T, U> {
	public final T t;

	public final U u;

	public Pair(T t, U u) {
		this.t = t;
		this.u = u;
	}

	public T getFirst() {
		return t;
	}

	public U getSecond() {
		return u;
	}

	@Override
	public int hashCode() {
		return (u == null ? 0 : u.hashCode()) ^ (t == null ? 0 : t.hashCode());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (null == obj) {
			return false;
		}

		if (!(obj instanceof Pair)) {
			return false;
		}

		@SuppressWarnings("unchecked")
		Pair<T, U> pair = (Pair<T, U>) obj;

		if (t.equals(pair.t) && u.equals(pair.u)) {
			return true;
		}

		return false;
	}
	
	public String toString() {
		return "[" + t.toString() + ":" + u.toString() + "]";
	}
}