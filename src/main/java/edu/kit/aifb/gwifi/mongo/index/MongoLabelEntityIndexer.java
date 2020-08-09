package edu.kit.aifb.gwifi.mongo.index;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Label;
import edu.kit.aifb.gwifi.model.Label.Sense;
import edu.kit.aifb.gwifi.model.Page.PageType;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.mongo.DBConstants;
import edu.kit.aifb.gwifi.mongo.MongoResource;
import edu.kit.aifb.gwifi.mongo.Property;
import edu.kit.aifb.gwifi.mongo.search.MongoLanguageLinksSearcher;
import edu.kit.aifb.gwifi.util.LabelIterator;
import edu.kit.aifb.gwifi.util.nlp.LabelNormalizer;
import edu.kit.aifb.gwifi.util.nlp.Language;

/**
 * This class is responsible to build the collection named "LabelEntityIndex_**" in MongoDB
 * 
 */
public class MongoLabelEntityIndexer {

	private Wikipedia _wikipedia;
	private DBCollection _dbCollection;
	private MongoLanguageLinksSearcher _searcher;
	private LabelNormalizer _normalizer;

	private Language _sLang;
	private Language _tLang;

	public MongoLabelEntityIndexer(String dbDir, String sLangLabel, String tLangLabel) throws Exception {
		_sLang = Language.getLanguage(sLangLabel);
		_tLang = Language.getLanguage(tLangLabel);
		_dbCollection = MongoResource.INSTANCE.getDB()
				.getCollection(DBConstants.LABEL_ENTITY_COLLECTION + _sLang.getLabel() + "_" + _tLang.getLabel());
		if(!_sLang.equals(_tLang))
			_searcher = new MongoLanguageLinksSearcher(_sLang, _tLang);
		_wikipedia = new Wikipedia(new File(dbDir), false);
		_normalizer = new LabelNormalizer(_sLang);
	}

	// "configs/configuration_gwifi.properties" "configs/wikipedia-template-zh.xml" "zh" "en"
	public static void main(String[] args) {
		try {
			String configPath = args[0];
			Property.setProperties(configPath);
			MongoLabelEntityIndexer indexer = new MongoLabelEntityIndexer(args[1], args[2], args[3]);
			indexer.insertLabelEntity();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void insertLabelEntity() throws IOException {
		LabelIterator labelIterator = _wikipedia.getLabelIterator(null);
		int i = 0;
		while (labelIterator.hasNext()) {
			if (++i % 100000 == 0) {
				System.out.println(i + " lables have been processed!");
			}
			Label label = labelIterator.next();
			Sense[] senses = _wikipedia.getLabel(label.getText()).getSenses();

			for (Sense sense : senses) {
				if (sense.getType().equals(PageType.article)) {
					long sLinkOccCount = sense.getLinkOccCount();
					long sLinkDocCount = sense.getLinkDocCount();
					double associationStrength = sense.getPriorProbability();

					int sId = sense.getId();
					String labelText = label.getText();
					String nLabelText = _normalizer.normalize(labelText);
					BasicDBObject dbobj = new BasicDBObject(DBConstants.LABEL_ENTITY_LABEL_TEXT, labelText)
							.append(DBConstants.LABEL_ENTITY_NORM_LABEL_TEXT, nLabelText)
							.append(DBConstants.LABEL_ENTITY_SOURCE_ENTITY_ID, sId)
							.append(DBConstants.LABEL_ENTITY_SOURCE_ENTITY_NAME, sense.getTitle())
							.append(DBConstants.LABEL_ENTITY_ENTITY_TYPE, sense.getType().toString())
							.append(DBConstants.LABEL_ENTITY_SENSE_LINK_OCC_COUNT, sLinkOccCount)
							.append(DBConstants.LABEL_ENTITY_SENSE_LINK_DOC_COUNT, sLinkDocCount)
							.append(DBConstants.LABEL_ENTITY_ASSOCIATION_STRENGTH, associationStrength)
							.append(DBConstants.LABEL_ENTITY_SOURCE, DBConstants.SOURCE_WIKI_LABEL);
					if(!_sLang.equals(_tLang)) {
						String[] tIdAndEntity = _searcher.getCrossLingualEntity(sId).split(":");
						if (tIdAndEntity.length == 2)
							dbobj.append(DBConstants.LABEL_ENTITY_TARGET_ENTITY_ID, Integer.valueOf(tIdAndEntity[0]))
									.append(DBConstants.LABEL_ENTITY_TARGET_ENTITY_NAME, tIdAndEntity[1]);
					} else {
						dbobj.append(DBConstants.LABEL_ENTITY_TARGET_ENTITY_ID, sId)
						.append(DBConstants.LABEL_ENTITY_TARGET_ENTITY_NAME, sense.getTitle());
					}
					_dbCollection.insert(dbobj);
				}
			}
		}
		_dbCollection.createIndex(new BasicDBObject(DBConstants.LABEL_ENTITY_SOURCE_ENTITY_ID, 1));
		_dbCollection.createIndex(new BasicDBObject(DBConstants.LABEL_ENTITY_SOURCE_ENTITY_NAME, 1));
		_dbCollection.createIndex(new BasicDBObject(DBConstants.LABEL_ENTITY_TARGET_ENTITY_ID, 1));
		_dbCollection.createIndex(new BasicDBObject(DBConstants.LABEL_ENTITY_TARGET_ENTITY_NAME, 1));
		_dbCollection.createIndex(new BasicDBObject(DBConstants.LABEL_ENTITY_ENTITY_TYPE, 1));
		_dbCollection.createIndex(new BasicDBObject(DBConstants.LABEL_ENTITY_LABEL_TEXT, 1));
		_dbCollection.createIndex(new BasicDBObject(DBConstants.LABEL_ENTITY_NORM_LABEL_TEXT, 1));
		_dbCollection.createIndex(new BasicDBObject(DBConstants.LABEL_ENTITY_ASSOCIATION_STRENGTH, -1));
		_dbCollection.createIndex(new BasicDBObject(DBConstants.LABEL_ENTITY_SENSE_LINK_OCC_COUNT, -1));
		_dbCollection.createIndex(new BasicDBObject(DBConstants.LABEL_ENTITY_SENSE_LINK_DOC_COUNT, -1));
		_dbCollection.createIndex(new BasicDBObject(DBConstants.LABEL_ENTITY_SOURCE, 1));
		labelIterator.close();
		_wikipedia.close();
	}

	/**
	 * the probability that an entity is related to a label, the approach here used is from the paper
	 * "Semantic Similarity Measurements for Multi-lingual Short Texts Using Wikipedia_Nakamura" , i.e., the equation
	 * (5)
	 * 
	 * @param article
	 * @param label
	 * @return
	 */
	@SuppressWarnings("unused")
	private double calProbEntityRelatedToLabel(Article article, Label label) {
		double prob = 0.0;
		Sense[] senses = _wikipedia.getLabel(label.getText()).getSenses();
		for (Sense sense : senses) {
			if (sense instanceof Article) {
				Article art = (Article) sense;
				prob += calProbEntityRelatedToEntity(article, art) * calProbLabelLinkedToEntity(art, label);
			}
		}
		return round(prob, 10);
	}

	/**
	 * The number of links between two entities
	 * 
	 * @param art1
	 *            the first entity
	 * @param art2
	 *            the second entity
	 * @return
	 */
	private int getNumOfLinks(Article art1, Article art2) {
		int numOfLinks = 0;
		for (Article art : art1.getLinksIn()) {
			if (art.getTitle().equals(art2.getTitle())) {
				numOfLinks++;
			}
		}
		for (Article art : art1.getLinksOut()) {
			if (art.getTitle().equals(art2.getTitle())) {
				numOfLinks++;
			}
		}
		return numOfLinks;
	}

	/**
	 * the probability that one entity is related to the other entity, the approach used is from the paper
	 * "Semantic Similarity Measurements for Multi-lingual Short Texts Using Wikipedia_Nakamura" , i.e., equation (4)
	 * 
	 * @param oneArticle
	 *            one entity
	 * @param theOtherArticle
	 *            the other entity
	 * @return
	 */
	private double calProbEntityRelatedToEntity(Article oneArticle, Article theOtherArticle) {
		return (1.0 * getNumOfLinks(oneArticle, theOtherArticle))
				/ (1.0 * (oneArticle.getTotalLinksInCount() + oneArticle.getTotalLinksOutCount()));
	}

	/**
	 * the probability that a label is linked to an entity, the approach here used is from the paper
	 * "Semantic Similarity Measurements for Multi-lingual Short Texts Using Wikipedia_Nakamura" , i.e., the equation
	 * (3)
	 * 
	 * @param article
	 * @param label
	 * @return
	 */
	private double calProbLabelLinkedToEntity(Article article, Label label) {
		double prob = 0.0;
		Article.Label[] labels = article.getLabels();
		if (Arrays.asList(labels).contains(label)) {
			long totalLinkOccCount = 0;
			Sense[] senses = _wikipedia.getLabel(label.getText()).getSenses();
			for (Sense sense : senses) {
				totalLinkOccCount += sense.getLinkOccCount();
			}
			if (totalLinkOccCount != 0) {
				prob = (1.0 * label.getLinkOccCount()) / (1.0 * totalLinkOccCount);
			}
		}
		return prob;
	}

	public static double round(double score, int scale) {
		if (Double.isInfinite(score) || Double.isNaN(score)) {
			score = 0.0;
		} else {
			BigDecimal bdScore = new BigDecimal(score);
			bdScore = bdScore.setScale(scale, BigDecimal.ROUND_HALF_UP);
			score = Double.parseDouble(bdScore.toString());
		}
		return score;
	}
}
