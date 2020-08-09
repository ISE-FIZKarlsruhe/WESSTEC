package edu.kit.aifb.gwifi.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

import edu.kit.aifb.gwifi.db.WEntry;
import edu.kit.aifb.gwifi.db.WEnvironment;
import edu.kit.aifb.gwifi.db.WIterator;
import edu.kit.aifb.gwifi.db.struct.DbLabel;
import edu.kit.aifb.gwifi.model.Label;
import edu.kit.aifb.gwifi.util.text.TextProcessor;

/**
 * @author David Milne
 * 
 * Provides efficient iteration over the labels in Wikipedia
 */
public class LabelIterator implements Iterator<Label>{


	WEnvironment env ;
	TextProcessor tp ;
	WIterator<String,DbLabel> iter ;

	Label nextLabel = null ;

	/**
	 * Creates an iterator that will loop through all pages in Wikipedia.
	 * 
	 * @param database an active (connected) Wikipedia database.
	 */
	public LabelIterator(WEnvironment env, TextProcessor tp) {

		this.env = env ;
		this.tp = tp ;
		iter = env.getDbLabel(tp).getIterator() ; 

		queueNext() ;
	}

	@Override
	public boolean hasNext() {
		return (nextLabel != null) ;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException() ;
	}

	@Override
	public Label next() {

		if (nextLabel == null) 
			throw new NoSuchElementException() ;

		Label l = nextLabel ;
		queueNext() ;

		return l ;
	}

	private void queueNext() {

		try {
			nextLabel=toLabel(iter.next()) ;

		} catch (NoSuchElementException e) {
			nextLabel = null ;
		}
	}

	private Label toLabel(WEntry<String,DbLabel> e) {
		if (e== null)
			return null ;
		else
			return Label.createLabel(env, e.getKey(), e.getValue(), tp) ;
	}

	public void close() {
		iter.close();
	}
}