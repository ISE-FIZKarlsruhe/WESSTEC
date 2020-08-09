package org.fiz.ise.gwifi.util;

public class MutableInt {
	int value = 1; // note that we start at 1 since we're counting
	  public void increment () { ++value;      }
	  public int  get ()       { return value; }
}
