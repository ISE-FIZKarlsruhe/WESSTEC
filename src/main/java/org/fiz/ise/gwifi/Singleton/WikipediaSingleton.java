package org.fiz.ise.gwifi.Singleton;

import java.io.File;

import org.fiz.ise.gwifi.util.Config;

import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.util.PageIterator;

public class WikipediaSingleton {
	// static variable single_instance of type Singleton
    private static WikipediaSingleton single_instance = null;
 
    // variable of type String
    public Wikipedia wikipedia;
    // private constructor restricted to this class itself
    private WikipediaSingleton()
    {
    	try {
    		wikipedia = new Wikipedia(new File("configs/wikipedia-template-en.xml"), false);
    		System.out.println("The Wikipedia environment has been initialized.");
		} catch (Exception e) {
			System.out.println("Exception initializing Wikipedia");
		}
    }
    // static method to create instance of Singleton class
    public static WikipediaSingleton getInstance()
    {
        if (single_instance == null)
            single_instance = new WikipediaSingleton();
 
        return single_instance;
    }
    public  Article getArticle(String title) {
		if (wikipedia.getArticleByTitle(title) == null) {
			//System.out.println("Could not find exact match of an article for a given title " + title);
			return null;
		}
		return wikipedia.getArticleByTitle(title);
	}
}
