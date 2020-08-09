package edu.kit.aifb.gwifi.demo;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Label;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.util.WikipediaConfiguration;
import edu.kit.aifb.gwifi.util.text.CaseFolder;
import edu.kit.aifb.gwifi.util.text.TextFolder;
import edu.kit.aifb.gwifi.util.text.TextProcessor;

public class LabelDemo {

	/**
	 * Provides a demo of functionality available to Labels
	 * 
	 */

	public static void main(String args[]) throws Exception {
		File databaseDirectory = new File("configs/wikipedia-template-en.xml");
		WikipediaConfiguration conf = new WikipediaConfiguration(databaseDirectory);
//		CaseFolder textProcessor = new CaseFolder();
//		conf.setDefaultTextProcessor(textProcessor);
		Wikipedia wikipedia = new Wikipedia(conf, false);
		System.out.println("The Wikipedia environment has been initialized.");

		BufferedReader in = new BufferedReader(new InputStreamReader(System.in,  "UTF-8"));

		while (true) {
			System.out.println("\nEnter article title (or enter to quit): ");
			String text = in.readLine();

			if (text == null || text.equals(""))
				break;

//			Label label = wikipedia.getLabel(text, textProcessor);
			Label label = wikipedia.getLabel(text, null);
			if(!label.exists())
				continue;
			
			System.out.println(" - the number of articles that contain links with this label used as an anchor: ");
			System.out.println("    - " + label.getLinkDocCount());
			System.out.println(" - the number of links that use this label as an anchor: ");
			System.out.println("    - " + label.getLinkOccCount());
			System.out.println(" - the number of articles that mention this label (either as links or in plain text): ");
			System.out.println("    - " + label.getDocCount());
			System.out.println(" - the number of times this label is mentioned in articles (either as links or in plain text): ");
			System.out.println("    - " + label.getOccCount());
			System.out.println(" - the probability that this label is used as a link in Wikipedia: ");
			System.out.println("    - " + label.getLinkProbability());
			
			System.out.println("\nSenses for " + text + ": ");
			int i = 1;
			for (Label.Sense sense : label.getSenses()) {
//				if(i>2)
//					break;
				System.out.println("\n" + i++ + ": " + sense + " with id " + sense.getId());
				System.out.println(" - the number of articles that contain this anchor text pointing to \""
						+ sense.getTitle() + "\": ");
				System.out.println("    - " + sense.getLinkDocCount());
				System.out.println(" - the number of links that use this anchor text pointing to \"" + sense.getTitle()
						+ "\": ");
				System.out.println("    - " + sense.getLinkOccCount());
				System.out.println(" - the probability that this anchor text goes to the destination \""
						+ sense.getTitle() + "\": ");
				System.out.println("    - " + sense.getPriorProbability());
				
//				String markup = sense.getMarkup();	
//				String cleanedText = sense.getPlainText();
//				System.out.println(cleanedText);
//				Matcher m = Pattern.compile("((?<=\\u005B\\u005B).*?(?=\\u005D\\u005D))").matcher(markup);
//				while (m.find()) {
//					String entityName = m.group();
//
//					if (entityName.contains("|"))
//						entityName = entityName.substring(0, entityName.indexOf("|"));
//					if (entityName != null && !entityName.equals("")) {
////							&& (nEntity.contains(nLabelText) || _sLang.equals(Language.ZH))) {
//						Article entity = wikipedia.getArticleByTitle(entityName);
//						if(entity != null)
//							System.out.println(entity.getTitle());
//					}
//				}
			}

		}

	}
}