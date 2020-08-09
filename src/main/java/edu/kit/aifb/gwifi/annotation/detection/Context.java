/*
 *    Context.java
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

package edu.kit.aifb.gwifi.annotation.detection;

import java.util.* ;
import java.text.* ;


import edu.kit.aifb.gwifi.model.*;
import edu.kit.aifb.gwifi.util.*;

/**
 * A selection of unambiguous terms and their corresponding articles, which are used to resolve ambiguous terms.
 * 
 * @author David Milne 
 */
public class Context {
		
	private Vector<Article> contextArticles ;
	private float totalWeight ;
	private RelatednessCache relatednessCache ;
	
	/**
	 * Initialises a collection of context articles from the given set of unambiguous anchors. 
	 * 
	 * @param unambigAnchors a set of unambiguous anchors, the most useful of which will be used to disambiguate other terms
	 * @param relatednessCache a cache in which relatedness measures will be saved so they aren't repeatedly calculated.
	 * @param maxSize the maximum number of anchors that will be used (the more there are, the longer disambiguation takes, but the more accurate it is likely to be).
	 * @throws Exception 
	 */
	public Context(Collection<Label> unambigLabels, RelatednessCache relatednessCache, double maxSize) throws Exception {
		
		this.relatednessCache = relatednessCache ;
		
		HashSet<Integer> doneIds = new HashSet<Integer>() ;		
		Vector<Label.Sense> senses = new Vector<Label.Sense>() ;
		for (Label label: unambigLabels) {
			
			Label.Sense sense = label.getSenses()[0] ;	
			if (!isDate(sense) && !doneIds.contains(sense.getId())) {
				sense.setWeight(label.getLinkProbability()) ; // linkDocCount/textDocCount
				senses.add(sense) ;
				doneIds.add(sense.getId()) ;
			}
		}
		
		TreeSet<Article> sortedContextArticles = new TreeSet<Article>() ;
		for (Label.Sense s:senses) {
			double linkProb = s.getWeight() ;
			
			double avgRelatedness = 0 ;
			
			for (Label.Sense s2: senses) 
				avgRelatedness += this.relatednessCache.getRelatedness(s, s2) ; 
				
			avgRelatedness = avgRelatedness / (senses.size()) ;
			
			// the effect of linkProb should be greater 
			double weight = (linkProb + avgRelatedness + avgRelatedness)/3 ;
			
			s.setWeight(weight) ;
			sortedContextArticles.add(s) ;
		}
		
		contextArticles = new Vector<Article>() ; 
		int c = 0 ;
		for (Article art: sortedContextArticles) {
			if (c++ > maxSize)
				break ;
			
			//System.out.println(" - cntxt art:" + art + ", w: " + art.getWeight()) ;
			
			totalWeight += art.getWeight() ;
			contextArticles.add(art) ;			
		}		
	}
	
	
	/**
	 * Initialises a collection of context articles from the given set of ambiguous anchors,  
	 * 
	 * @param ambigAnchors a set of ambiguous anchors, the most useful of which will be used to disambiguate other terms
	 * @param relatednessCache a cache in which relatedness measures will be saved so they aren't repeatedly calculated. 
	 * @param maxSize the maximum number of anchors that will be used (the more there are, the longer disambiguation takes, but the more accurate it is likely to be).
	 * @param minSenseLimit the minimum prior probability of an anchors sense that will be used as context.  
	 * @throws Exception 
	 */
	public Context(Collection<Label> ambigAnchors, RelatednessCache relatednessCache, double maxSize, double minSenseLimit) throws Exception {
		
		this.relatednessCache = relatednessCache ;
		
		HashSet<Integer> doneIds = new HashSet<Integer>() ;		
		Vector<Label.Sense> senses = new Vector<Label.Sense>() ;
		for (Label label: ambigAnchors) {
			
			for (Label.Sense sense:label.getSenses()) {
				
				double pp = sense.getPriorProbability() ;
				
				if (pp < minSenseLimit) break ;
				
				if (!isDate(sense) && !doneIds.contains(sense.getId())) {
					sense.setWeight(label.getLinkProbability() * pp) ;
					senses.add(sense) ;
					doneIds.add(sense.getId()) ;
				}
			}
		}
		
		TreeSet<Article> sortedContextArticles = new TreeSet<Article>() ;
		for (Label.Sense s:senses) {
			double linkProb = s.getWeight() ;
			
			double avgRelatedness = 0 ;
			
			for (Label.Sense s2: senses) 
				avgRelatedness += this.relatednessCache.getRelatedness(s, s2) ; 
				
			avgRelatedness = avgRelatedness / (senses.size()) ;
			
			double weight = (linkProb + avgRelatedness + avgRelatedness)/3 ;
			
			s.setWeight(weight) ;
			sortedContextArticles.add(s) ;
		}
		
		contextArticles = new Vector<Article>() ; 
		int c = 0 ;
		for (Article art: sortedContextArticles) {
			if (c++ > maxSize)
				break ;
			
			totalWeight += art.getWeight() ;
			contextArticles.add(art) ;			
		}	
	}

	/**
	 * @return the quality (size and homogeneity) of the available context. 
	 */
	public float getQuality() {
		return totalWeight ;		
	}	

	/**
	 * Compares the given article to all context anchors.
	 * 
	 * @param art the article to be compared
	 * @return the average relatedness between the article and context anchors
	 * @throws Exception 
	 */
	public double getRelatednessTo(Article art) throws Exception {
		
		if (contextArticles.size() == 0 || totalWeight == 0)
			return 0 ;

		double relatedness = 0 ;
		
		for (Article contextArt: contextArticles) { 
			
			double r = relatednessCache.getRelatedness(art, contextArt) ;
			r = r * contextArt.getWeight() ;
			relatedness = relatedness + r ;
		}
		
		return relatedness / totalWeight ;
	}
	
	private boolean isDate(Article art) {
		SimpleDateFormat sdf = new SimpleDateFormat("MMMM d") ;
		Date date = null ;
		
		try {
			date = sdf.parse(art.getTitle()) ;
		} catch (ParseException e) {
			return false ;
		}

		return (date != null) ;		
	}
}

