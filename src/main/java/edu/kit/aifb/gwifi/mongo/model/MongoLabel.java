package edu.kit.aifb.gwifi.mongo.model;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import edu.kit.aifb.gwifi.model.ILabel;
import edu.kit.aifb.gwifi.model.ISense;
import edu.kit.aifb.gwifi.model.Label.Sense;
import edu.kit.aifb.gwifi.model.Page.PageType;
import edu.kit.aifb.gwifi.mongo.search.MongoLabelEntitySearcher;
import edu.kit.aifb.gwifi.mongo.search.MongoLabelEntitySearcher.DBSense;
import edu.kit.aifb.gwifi.mongo.search.MongoLabelSearcher;
import edu.kit.aifb.gwifi.mongo.search.MongoLabelSearcher.DBLabel;
import edu.kit.aifb.gwifi.util.nlp.LabelNormalizer;

/**
 * A term or phrase that has been used to refer to one or more entities in Wikipedia.
 * 
 * These provide your best way of searching for articles relating to or describing a particular term.
 */
public class MongoLabel implements ILabel {

	private String text;

	private LabelNormalizer normalizer;

	private long linkDocCount = 0;
	private long linkOccCount = 0;
	private long textDocCount = 0;
	private long textOccCount = 0;

	private MongoSense[] senses = null;

	private MongoLabelSearcher labelSearcher;
	private MongoLabelEntitySearcher senseSearcher;

	private boolean detailsSet;

	/**
	 * Initialises a Label
	 * 
	 * @param text
	 *            the term or phrase of interest
	 */
	public MongoLabel(MongoLabelSearcher labelSearcher, MongoLabelEntitySearcher senseSearcher, String text,
			LabelNormalizer normalizer) {
		this.labelSearcher = labelSearcher;
		this.senseSearcher = senseSearcher;
		this.text = text;
		this.normalizer = normalizer;
		this.detailsSet = false;
	}

	@Override
	public String toString() {
		return "\"" + text + "\"";
	}

	/**
	 * @return the text used to refer to concepts.
	 */
	public String getText() {
		return text;
	}

	/**
	 * @return true if this label has ever been used to refer to an article, otherwise false
	 */
	public boolean exists() {
		if (!detailsSet)
			setDetails();
		return (senses.length > 0);
	}

	/**
	 * @return the number of articles that contain links with this label used as an anchor.
	 */
	public long getLinkDocCount() {
		if (!detailsSet)
			setDetails();

		// TODO
		if (exists() && linkDocCount == 0)
			linkDocCount += 1;

		return linkDocCount;
	}

	/**
	 * @return the number of links that use this label as an anchor.
	 */
	public long getLinkOccCount() {
		if (!detailsSet)
			setDetails();

		// TODO
		if (exists() && linkOccCount == 0)
			linkOccCount += 1;

		return linkOccCount;
	}

	/**
	 * @return the number of articles that mention this label (either as links or in plain text).
	 */
	public long getDocCount() {
		if (!detailsSet)
			setDetails();

		// TODO
		if (exists() && textDocCount == 0)
			textDocCount += 1;

		return textDocCount;
	}

	/**
	 * @return the number of times this label is mentioned in articles (either as links or in plain text).
	 */
	public long getOccCount() {
		if (!detailsSet)
			setDetails();

		// TODO
		if (exists() && textOccCount == 0)
			textOccCount += 1;

		return textOccCount;
	}

	/**
	 * @return the probability that this label is used as a link in Wikipedia ({@link #getLinkDocCount()}/
	 *         {@link #getDocCount()}.
	 */
	public double getLinkProbability() {
		if (!detailsSet)
			setDetails();

		// TODO
		if (exists() && linkDocCount == 0)
			linkDocCount += 1;
		if (exists() && textDocCount == 0)
			textDocCount += 1;

		// if (textDocCount == 0)
		// return 0 ;

		double linkProb = (double) linkDocCount / textDocCount;

		if (linkProb > 1)
			linkProb = 1;

		return linkProb;
	}

	/**
	 * @return an array of {@link Sense Senses}, sorted by {@link Sense#getPriorProbability()}, that this label refers
	 *         to.
	 */
	public ISense[] getSenses() {
		if (!detailsSet)
			setDetails();
		return senses;
	}

	private void setDetails() {

		try {
			boolean normalized = false;
			String labelText = text;
			if (normalizer != null) {
				normalized = true;
				labelText = normalizer.normalize(text);
			}
			DBLabel dbl = labelSearcher.getOneLabel(labelText, null, normalized);
			List<DBSense> dbss = senseSearcher.getSenses(labelText, null, normalized);

			if (dbl == null || !dbl.exists()) {
				throw new Exception();
			} else {
				setDetails(dbl, dbss);
			}
		} catch (Exception e) {
			this.senses = new MongoSense[0];
			detailsSet = true;
		}
	}

	private void setDetails(DBLabel dbl, List<DBSense> dbss) {

		this.linkDocCount = dbl.getLinkDocCount();
		this.linkOccCount = dbl.getLinkOccCount();
		this.textDocCount = dbl.getTextDocCount();
		this.textOccCount = dbl.getTextOccCount();

		this.senses = new MongoSense[dbss.size()];

		int i = 0;
		for (DBSense dbs : dbss) {
			this.senses[i] = new MongoSense(dbs);
			i++;
		}

		this.detailsSet = true;
	}

	/**
	 * A possible sense for a label
	 */
	public class MongoSense implements ISense {

		private int id;
		private String title;

		private PageType type;

		private long sLinkDocCount;
		private long sLinkOccCount;

		private String sTitle;

		protected MongoSense(DBSense s) {

			this.id = s.getId();
			this.title = s.getName();

			if (s.getType().equals(PageType.article.toString()))
				this.type = PageType.article;
			else if (s.getType().equals(PageType.disambiguation.toString()))
				this.type = PageType.disambiguation;
			else if (s.getType().equals(PageType.redirect.toString()))
				this.type = PageType.redirect;
			else if (s.getType().equals(PageType.category.toString()))
				this.type = PageType.category;
			else if (s.getType().equals(PageType.template.toString()))
				this.type = PageType.template;
			else
				this.type = PageType.invalid;

			this.sLinkDocCount = s.getLinkDocCount();
			this.sLinkOccCount = s.getLinkOccCount();

			this.sTitle = s.getSourceName();

			// TODO
//			if (sLinkDocCount == 0)
//				sLinkDocCount += 1;
//			if (sLinkOccCount == 0)
//				sLinkOccCount += 1;
//			if (linkDocCount == 0)
//				linkDocCount = sLinkDocCount;
//			if (linkOccCount == 0)
//				linkOccCount = sLinkOccCount;
		}

		/**
		 * @return the unique identifier
		 */
		public int getId() {
			if (!detailsSet)
				setDetails();

			return id;
		}

		/**
		 * @return the title
		 */
		public String getTitle() {
			if (!detailsSet)
				setDetails();

			return title;
		}

		/**
		 * @return the type of the page
		 */
		public PageType getType() {
			if (!detailsSet)
				setDetails();

			return type;
		}

		/**
		 * Returns the number of documents that contain links that use the surrounding label as anchor text, and point
		 * to this sense as the destination.
		 * 
		 * @return the number of documents that contain links that use the surrounding label as anchor text, and point
		 *         to this sense as the destination.
		 */
		public long getLinkDocCount() {
			return sLinkDocCount;
		}

		/**
		 * Returns the number of links that use the surrounding label as anchor text, and point to this sense as the
		 * destination.
		 * 
		 * @return the number of links that use the surrounding label as anchor text, and point to this sense as the
		 *         destination.
		 */
		public long getLinkOccCount() {
			return sLinkOccCount;
		}

		/**
		 * Returns the probability that the surrounding label goes to this destination
		 * 
		 * @return the probability that the surrounding label goes to this destination
		 */
		public double getPriorProbability() {

			if (getSenses().length == 1)
				return 1;

			double sim = getMatchSimilarity();
			
			if (linkOccCount == 0) {
				return Math.max(0.005, sim);
			} else {
				double pro = ((double) sLinkOccCount) / linkOccCount;
				return Math.max(pro, sim);
			}
		}

		public double getMatchSimilarity() {
			if (sTitle != null) {
				String s1 = normalizer.normalize(text);
				String s2 = normalizer.normalize(sTitle);
				int dis = StringUtils.getLevenshteinDistance(s1, s2);
				double sim = 1 - ((double) dis) / (Math.max(s1.length(), s2.length()));
				return sim;
			} else {
				return 0;
			}
		}

		public String toString() {
			return title;
		}

	}

}
