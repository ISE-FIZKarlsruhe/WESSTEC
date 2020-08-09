/*
 *    StopwordRemover.java
 *    Copyright (C) 2007 David Milne, d.n.milne@gmail.com
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package edu.kit.aifb.gwifi.util.text;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;

import edu.kit.aifb.gwifi.db.WEnvironment;
import edu.kit.aifb.gwifi.util.WikipediaConfiguration;


/**
 * This class provides moderate morphology. This involves cleaning the text using a TextCleaner then
 * removing all stopwords.
 */	
public class StopwordRemover extends TextProcessor {
	
	HashSet<String> stopwords ;
	Cleaner cleaner = new Cleaner() ;
	
	/**
	 * Initializes a newly created StopwordRemover with a list of stopwords contained within the given file. 
	 * The file must be in a format where each word is found on its own line.  
	 * 
	 * @param	stopwordFile	the file of stopwords
	 * @throws	IOException		if there is a problem reading from the file of stopwords
	 */	
	public StopwordRemover(File stopwordFile) throws IOException {
		
		stopwords = new HashSet<String>() ;
		
		BufferedReader input = new BufferedReader(new FileReader(stopwordFile)) ;
		
		String line ;
		while ((line = input.readLine()) != null) {
			String word = line.trim().toLowerCase() ;
			stopwords.add(word) ;
		}
	}
	
	/**
	 * Initializes a newly created StopwordRemover with a list of stopwords contained within the HashSet. 
	 * 
	 * @param	stopwords	a HashSet of stopwords
	 */	
	public StopwordRemover(HashSet<String> stopwords){
		this.stopwords = stopwords ;
	}
	
	/**
	 * Returns the processed version of the argument string. This involves 
	 * removing all stopwords, then cleaning each remaining term.
	 * 
	 * @param	text	the string to be processed
	 * @return the processed string
	 */	
	public String processText(String text) {
		String t = text ;
		String t2 = "" ;
		
		String[] terms = t.split(" ") ;
		for(int i=0;i<terms.length; i++)
			if (!stopwords.contains(terms[i]))
				t2 = t2 + cleaner.processText(terms[i]) + " " ;
		
		return t2.trim() ;	
	}
	
	public static void main(String args[]) throws Exception {

		String stopwords = args[1];
		StopwordRemover folder = new StopwordRemover(new File(stopwords)) ;

	    WikipediaConfiguration conf = new WikipediaConfiguration(new File(args[0])) ;
	    WEnvironment.prepareTextProcessor(folder, conf, new File("tmp"), true, 1) ;
	}
}


