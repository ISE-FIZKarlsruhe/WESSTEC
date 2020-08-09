package edu.kit.aifb.gwifi.util.nlp;

import edu.kit.aifb.gwifi.util.nlp.ITokenStream;

/**
 * This class models NLP components.
 * 
 * It specifies a method to get a modified token stream. This can e.g. be used
 * for stop word filtering, stemming, ...
 * 
 * @author pso
 *
 */
public interface ITokenAnalyzer {

	/**
	 * Returns an analyzed token stream based on a specified token stream.
	 * 
	 * In detail this new token stream gets all tokens from the specified token
	 * stream, analyzes and drops/modifies them and returns them in a new token
	 * stream
	 * 
	 * @param ts The input token stream of which the tokens will be analyzed and modified.
	 * @return A new token stream that streams the analyzed tokens.
	 */
	public ITokenStream getAnalyzedTokenStream( ITokenStream ts );
	
}