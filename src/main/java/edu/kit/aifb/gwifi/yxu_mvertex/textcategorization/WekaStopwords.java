package edu.kit.aifb.gwifi.yxu_mvertex.textcategorization;

import weka.core.Stopwords;

public class WekaStopwords extends Stopwords {
	
	private static final String WRITING_FAIL_MSG = "Fail to write the stopwords to: ";
	private static final String READING_FAIL_MSG = "Fail to read the stopwords from: ";
	
	
	public static boolean genOutputStopwordsFile(String inputStopwordsFile, String outputStopwordsFile){
		boolean isWritten = false;
		Stopwords sw = new Stopwords(); //rainbow by default
		try {
			sw.read(inputStopwordsFile);
			return isWritten;
		} catch (Exception e) {
			System.out.println(READING_FAIL_MSG+inputStopwordsFile);
			sw = new Stopwords();
			return isWritten;
		} finally {
			isWritten = writeSW2OutputFile(sw, outputStopwordsFile);
		}
	}
	
	private static boolean writeSW2OutputFile(Stopwords sw, String outputFile){
		try {
			sw.write(outputFile);
			return true;
		} catch (Exception e) {
			System.out.println(WRITING_FAIL_MSG+outputFile);
			return false;
		}
	}

}
