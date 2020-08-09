package edu.kit.aifb.gwifi.annotation.detection;

import java.io.IOException;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import edu.kit.aifb.gwifi.annotation.detection.ArticleCleaner.SnippetLength;
import edu.kit.aifb.gwifi.comparison.ArticleComparer;
import edu.kit.aifb.gwifi.db.WDatabase.DatabaseType;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Label;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.util.RelatednessCache;
import edu.kit.aifb.gwifi.util.WikipediaConfiguration;
import edu.kit.aifb.gwifi.util.text.TextProcessor;

public class DisambiguationUtil {

	private Wikipedia wikipedia;
	private ArticleCleaner cleaner;
	private TextProcessor tp;
	private ArticleComparer comparer;

	private int maxLabelLength = 20;
	private double minSenseProbability = 0.05;
	private double minLinkProbability = 0.005;
	private int maxContextSize = 50;

	private int sensesConsidered = 0;

	public DisambiguationUtil(Wikipedia wikipedia) throws IOException, Exception {

		WikipediaConfiguration conf = wikipedia.getConfig();

		ArticleComparer comparer = new ArticleComparer(wikipedia);

		init(wikipedia, comparer, conf.getDefaultTextProcessor(), conf.getMinSenseProbability(),
				conf.getMinLinkProbability(), conf.getMaxContextSize());

	}

	/**
	 * Initializes the Disambiguator with custom parameters.
	 * 
	 * @param wikipedia
	 *            an initialized Wikipedia instance, preferably with relatedness measures cached.
	 * @param textProcessor
	 *            an optional text processor (may be null) that will be used to alter terms before they are searched
	 *            for.
	 * @param minSenseProbability
	 *            the lowest probability (as a destination for the ambiguous Label term) for which senses will be
	 *            considered.
	 * @param minLinkProbability
	 *            the lowest probability (as a link in Wikipedia) for which terms will be mined from surrounding text
	 * @param maxContextSize
	 *            the maximum number of concepts that are used as context.
	 * @throws Exception
	 */
	public DisambiguationUtil(Wikipedia wikipedia, ArticleComparer comparer, TextProcessor textProcessor,
			double minSenseProbability, double minLinkProbability, int maxContextSize) throws Exception {
		init(wikipedia, comparer, textProcessor, minSenseProbability, minLinkProbability, maxContextSize);
	}

	private void init(Wikipedia wikipedia, ArticleComparer comparer, TextProcessor textProcessor,
			double minSenseProbability, double minLinkProbability, int maxContextSize) throws Exception {
		this.wikipedia = wikipedia;
		this.comparer = comparer;
		this.cleaner = new ArticleCleaner();
		this.tp = textProcessor;

		this.minSenseProbability = minSenseProbability;
		this.minLinkProbability = minLinkProbability;
		this.maxContextSize = maxContextSize;

		if (wikipedia.getConfig().getCachePriority(DatabaseType.label) == null)
			Logger.getLogger(DisambiguationUtil.class).warn(
					"'label' database has not been cached, so this will run significantly slower than it needs to.");

		if (wikipedia.getConfig().getCachePriority(DatabaseType.pageLinksIn) == null)
			Logger.getLogger(DisambiguationUtil.class).warn(
					"'pageLinksIn' database has not been cached, so this will run significantly slower than it needs to.");
	}

	/**
	 * returns the probability (between 0 and 1) of a sense with the given commonness and relatedness being valid given
	 * the available context.
	 * 
	 * @param commonness
	 *            the commonness of the sense (it's prior probability, irrespective of context)
	 * @param relatedness
	 *            the relatedness of the sense to the given context (the result of calling context.getRelatednessTo()
	 * @param context
	 *            the available context.
	 * @return the probability that the sense implied here is valid.
	 */
	public double getProbabilityOfSense(double commonness, double relatedness, double similarity, Context context) {

		sensesConsidered++;

		return Math.max(commonness, relatedness);
	}

	public ArticleComparer getArticleComparer() {
		return comparer;
	}

	private Context getContext(Article article, SnippetLength snippetLength, RelatednessCache rc) throws Exception {

		Vector<Label> unambigLabels = new Vector<Label>();

		String content = cleaner.getMarkupLinksOnly(article, snippetLength);
		String s = "$ " + content + " $";

		Pattern p = Pattern.compile("[\\s\\{\\}\\(\\)\"\'\\.\\,\\;\\:\\-\\_]"); // would just match all non-word chars,
																				// but we dont want to match utf chars
		Matcher m = p.matcher(s);

		Vector<Integer> matchIndexes = new Vector<Integer>();

		while (m.find())
			matchIndexes.add(m.start());

		for (int i = 0; i < matchIndexes.size(); i++) {

			int startIndex = matchIndexes.elementAt(i) + 1;

			for (int j = Math.min(i + maxLabelLength, matchIndexes.size() - 1); j > i; j--) {
				int currIndex = matchIndexes.elementAt(j);
				String ngram = s.substring(startIndex, currIndex);

				if (!(ngram.length() == 1 && s.substring(startIndex - 1, startIndex).equals("'"))
						&& !ngram.trim().equals("")) {
					Label label = new Label(wikipedia.getEnvironment(), ngram, tp);

					if (label.getLinkProbability() > minLinkProbability) {

						Label.Sense[] senses = label.getSenses();

						if (senses.length == 1 || senses[0].getPriorProbability() >= (1 - minSenseProbability))
							unambigLabels.add(label);
					}
				}
			}
		}

		if (rc == null)
			return new Context(unambigLabels, new RelatednessCache(comparer), maxContextSize);
		else
			return new Context(unambigLabels, rc, maxContextSize);
	}

	/**
	 * @return the maximum length (in words) for ngrams that will be checked against wikipedia's Label vocabulary.
	 */
	public int getMaxLabelLength() {
		return maxLabelLength;
	}

	/**
	 * @return the lowest probability (as a link in Wikipedia) for which terms will be mined from surrounding text and
	 *         used as context.
	 */
	public double getMinLinkProbability() {
		return minLinkProbability;
	}

	public void setMinLinkProbability(double val) {
		minLinkProbability = val;
	}

	/**
	 * @return the lowest probability (as a destination for the ambiguous Label term) for which senses will be
	 *         considered.
	 */
	public double getMinSenseProbability() {
		return minSenseProbability;
	}

	public void setMinSenseProbability(double val) {
		minSenseProbability = val;
	}

	/**
	 * @return the maximum number of concepts that are used as context.
	 */
	public int getMaxContextSize() {
		return maxContextSize;
	}

	/**
	 * @return the text processor used to modify terms and phrases before they are compared to Wikipedia's Label
	 *         vocabulary.
	 */
	public TextProcessor getTextProcessor() {
		return tp;
	}

	public int getSensesConsidered() {
		return sensesConsidered;
	}

}
