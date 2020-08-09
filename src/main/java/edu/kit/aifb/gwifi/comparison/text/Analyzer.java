package edu.kit.aifb.gwifi.comparison.text;

import java.io.Reader;

import org.apache.lucene.analysis.TokenStream;

public interface Analyzer {

	public TokenStream tokenStream(String fieldName, Reader reader);
	
}
