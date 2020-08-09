package edu.kit.aifb.gwifi.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class WikipediaTools {

	static Log logger = LogFactory.getLog(WikipediaTools.class);

	// e.g., {{...}}
	static Pattern s_templatesPattern = Pattern.compile("\\{\\{([^\\}]+)\\}\\}");

	// e.g., [[...]]
	static Pattern s_linkPattern = Pattern.compile("\\[\\[([^\\]]+)\\]\\]");

	// e.g., [...]
	static Pattern s_externalLinkPattern = Pattern.compile("\\[([^\\[\\]]+)\\]");

	// e.g., <...>
	static Pattern s_htmlTagPattern = Pattern.compile("<[\\!-/=\"'\\w\\s]+>");

	// e.g., ''... or ==... 
	static Pattern s_wikiMarkupPattern = Pattern.compile("'{2,}|={2,}|(----)|(^[ \t]*[\\*#]+)");

	// e.g., 
	static Pattern s_tableMarkupPattern = Pattern.compile("\\{\\||\\|[\\+\\}\\-]?|!");

	// e.g., 
	static Pattern s_languageLinkPattern = Pattern.compile("\\[\\[\\w{2,3}([-_]\\w+)?:[^\\]]+\\]\\]");

	// e.g. style="border: 1px #aaa solid; border-collapse:collapse;"
	static Pattern s_formatPattern = Pattern.compile("\\w+=(\"[\\w\\s#;:\\-_]+\"|\\w+)"); 

	// e.g., https://... or http://
	static Pattern s_webLinkPattern = Pattern.compile("https?://[\\w\\.\\-_/?&%#:]+");

	static public String extractPlainText(String wikiText) {
		logger.trace(wikiText);
		// System.out.println( wikiText );

		/*
		 * find all templates and only keep argument values
		 */
		Matcher templatesMatcher = s_templatesPattern.matcher(wikiText);
		StringBuffer sb = new StringBuffer();
		while (templatesMatcher.find()) {
			String templateText = templatesMatcher.group(1);

			if (templateText == null) {
				templatesMatcher.appendReplacement(sb, " ");
			} else {
				String[] templateTextSplit = templateText.split("\\|");
				StringBuilder newTemplateTextBuilder = new StringBuilder();
				for (int i = 1; i < templateTextSplit.length; i++) {
					newTemplateTextBuilder.append(" ");
					newTemplateTextBuilder.append(templateTextSplit[i].replaceAll("^\\s*\\w+\\s*=", ""));
				}
				newTemplateTextBuilder.append(" ");

				String newTemplateText = newTemplateTextBuilder.toString();
				newTemplateText = newTemplateText.replaceAll("\\$", "\\\\\\$");

				templatesMatcher.appendReplacement(sb, newTemplateText);
			}
		}
		templatesMatcher.appendTail(sb);
		String currentText = sb.toString();

		/*
		 * remove all language links from text
		 */
		currentText = s_languageLinkPattern.matcher(currentText).replaceAll(" ");

		/*
		 * remove type information and markup from links
		 */
		Matcher linkMatcher = s_linkPattern.matcher(currentText);
		sb = new StringBuffer();
		while (linkMatcher.find()) {
			String linkText = linkMatcher.group(1);
			if (linkText == null) {
				linkMatcher.appendReplacement(sb, " ");
			} else {
				String newLinkText = linkText.replaceFirst("^.*:", "");
				newLinkText = newLinkText.replaceAll("\\|", " ");
				// newLinkText = newLinkText.replaceFirst( "^\\s*#" , "" );

				newLinkText = newLinkText.replaceAll("\\$", "\\\\\\$");

				linkMatcher.appendReplacement(sb, " " + newLinkText + " ");
			}
		}
		linkMatcher.appendTail(sb);
		currentText = sb.toString();

		/*
		 * remove external link markup
		 */
		currentText = s_externalLinkPattern.matcher(currentText).replaceAll("$1");

		/*
		 * remove html tags
		 */
		currentText = s_htmlTagPattern.matcher(currentText).replaceAll(" ");

		/*
		 * remove wiki markup
		 */
		currentText = s_wikiMarkupPattern.matcher(currentText).replaceAll(" ");

		/*
		 * remove table markup
		 */
		currentText = s_tableMarkupPattern.matcher(currentText).replaceAll(" ");

		/*
		 * remove formating markup
		 */
		currentText = s_formatPattern.matcher(currentText).replaceAll(" ");

		/*
		 * remove web links
		 */
		currentText = s_webLinkPattern.matcher(currentText).replaceAll(" ");

		logger.trace(currentText);
		return currentText;
	}

	public static String extractPlainTitle(String title) {
		/*
		 * delete text in parentheses
		 */
		String currentTitle = title.replaceAll("\\(.+\\)", "");

		/*
		 * replace _ with space
		 */
		currentTitle = currentTitle.replaceAll("_", " ");

		return currentTitle;
	}

	public static void main(String[] args) throws Exception {
		BufferedReader br = new BufferedReader(new FileReader(new File("res/test.txt")));
		String text = "", line;
		while((line = br.readLine()) != null) {
			text += line;
		}
		
		String plainText = WikipediaTools.extractPlainText(text);
		System.out.println(plainText);
		br.close();
	}
}
