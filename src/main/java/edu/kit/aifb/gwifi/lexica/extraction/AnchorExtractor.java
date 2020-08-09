package edu.kit.aifb.gwifi.lexica.extraction;

import java.io.File;
import java.io.PrintWriter;

import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Page;
import edu.kit.aifb.gwifi.model.Page.PageType;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.util.PageIterator;

public class AnchorExtractor {
	
	public static void main(String[] args) throws Exception {
		File databaseDirectory = new File(args[0]);
		Wikipedia wikipedia = new Wikipedia(databaseDirectory, false);
		System.out.println("The Wikipedia environment has been initialized.");

		PrintWriter pw = new PrintWriter(new File("DBpediaAnchor_" + args[1] + ".nt"));

		PageIterator iter = wikipedia.getPageIterator(PageType.article);

		String blcokProp = "<http://dbpedia.org/wiki-anchor-block>";
		String textProp = "<http://dbpedia.org/wiki-anchor-text>";
		String countProp = "<http://dbpedia.org/wiki-anchor-count>"; 
		String langProp = "<http://dbpedia.org/wiki-anchor-lang>";
		
		String lang = "\"" + args[1] + "\"";
		
		int i = 0, j = 0;
		while (iter.hasNext()) {

			j++;
			if(j%1000 == 0)
				System.out.println(j + " articles have been processed!"); 
			
			Page page = iter.next();
			
			if (!page.getType().equals(PageType.article))
				continue;

			String title = page.getTitle();

			if (title == null || title.equals(""))
				continue;

			Article article = wikipedia.getArticleByTitle(title);
			String uri = "<http://dbpedia.org/resource/" + title + ">";
			
			if (article == null) {
				System.out.println("Could not find exact match. Searching through anchors instead");
				continue;
			} else {
				for (Article.Label at : article.getLabels()) {
					String anchroText = "\"" + at.getText() + "\"";
					String linkOccCount = "\"" + at.getLinkOccCount() + "\""; 
					String block = "_:b" + ++i;
					pw.println(uri + " " + blcokProp + " " + block + ".");
					pw.println(block + " " + textProp + " " + anchroText + ".");
					pw.println(block + " " + countProp + " " + linkOccCount + ".");
					pw.println(block + " " + langProp + " " + lang + ".");
					
					if(i%10000 == 0)
						System.out.println(i + " labels have been processed!"); 
				}
			}
			
		}
		
		iter.close();
		pw.close();

	}
}