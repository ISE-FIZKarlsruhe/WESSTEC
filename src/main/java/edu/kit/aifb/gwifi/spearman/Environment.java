package edu.kit.aifb.gwifi.spearman;

public class Environment {

	// if this value is 0, then the window size is 1;
	// if this value is 1, then the window size is 3;
	// if this value is 2, then the window size is 5;
	// if this value is 3, then the window size is 7;
	// if this value is 5, then the window size is 11 (but we consider it as 10);
	public static int NUM_SORROUNDING_SENTENCES_ON_ONE_SIDE = 5;
	
	public static int INDEX_LENGTH_THRESHOLD = 1000;

}
