package edu.kit.aifb.gwifi.util.nlp;

import edu.kit.aifb.gwifi.util.nlp.Language;

/**
 * Interface of a token stream. A token stream gives access to the tokens of
 * a document. Each token has a surface string and a specified language.
 * 
 * @author pso
 *
 */
public interface ITokenStream {
	
	/**
	 * Move stream to the next token.
	 * 
	 * @return Returns true if there is a next token, false if the end of the stream is reached.
	 */
	public boolean next();

	/**
	 * @return The current token in this stream.
	 */
	public String getToken();
	
	/**
	 * @return The language of the current token.
	 */
	public Language getLanguage();
	
	public void reset();
}
