package edu.kit.aifb.gwifi.util;

import info.bliki.wiki.filter.WikipediaParser;
import info.bliki.wiki.model.Configuration;
import info.bliki.wiki.model.WikiModel;

import java.util.Map;

import edu.kit.aifb.gwifi.model.Template;
import edu.kit.aifb.gwifi.model.Wikipedia;

public class WikiMinerModel extends WikiModel {

	private Wikipedia wikipedia ;

	public WikiMinerModel(Wikipedia wikipedia) {
		super("http://www." + wikipedia.getConfig().getLangCode() + ".wikipedia.org/wiki/${image}", "http://www." + wikipedia.getConfig().getLangCode() + ".wikipedia.org/wiki/${title}") ;
		this.wikipedia = wikipedia ;
		
	}
	
	public WikiMinerModel(Wikipedia wikipedia, String imageBaseURL, String linkBaseURL) {

		super(imageBaseURL, linkBaseURL) ; 

	}

	public String getRawWikiContent(String namespace, String articleName, Map<String, String> templateParameters) {
		String result = super.getRawWikiContent(namespace, articleName, templateParameters);
		if (result != null) {
			// found magic word template
			return result;
		}

		//String templateNS = getTemplateNamespace() + ":";
		//String name = articleName;
		if (namespace.equals(getTemplateNamespace())) {
			String content = null;
			try {

				Template template = wikipedia.getTemplateByTitle(articleName) ;
				
				System.out.println("looking for template: " + articleName + ", " + template) ;
				if (template == null)
					return null ;
				else 
					content = template.getMarkup() ;

				if (content.length() == 0)
					return null ;

				content = getRedirectedWikiContent(content, templateParameters) ;

				if (content.length() == 0)
					return null ;

				return content ;

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public String getRedirectedWikiContent(String rawWikitext, Map<String, String> templateParameters) {
		if (rawWikitext.length() < 9) {
			// less than "#REDIRECT" string
			return rawWikitext;
		}
		String redirectedLink = WikipediaParser.parseRedirect(rawWikitext, this);
		if (redirectedLink != null) {
			String redirNamespace = "";
			String redirArticle = redirectedLink;
			int index = redirectedLink.indexOf(":");
			if (index > 0) {
				redirNamespace = redirectedLink.substring(0, index);
				if (isNamespace(redirNamespace)) {
					redirArticle = redirectedLink.substring(index + 1);
				} else {
					redirNamespace = "";
				}
			}
			try {
				int level = incrementRecursionLevel();
				if (level > Configuration.PARSER_RECURSION_LIMIT) {
					return "Error - getting content of redirected link: " + redirNamespace + ":" + redirArticle;
				}
				return getRawWikiContent(redirNamespace, redirArticle, templateParameters);
			} finally {
				decrementRecursionLevel();
			}
		}
		return rawWikitext;
	}
}
