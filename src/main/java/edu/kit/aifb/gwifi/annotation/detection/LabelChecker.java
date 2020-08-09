package edu.kit.aifb.gwifi.annotation.detection;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import edu.kit.aifb.gwifi.annotation.DBConstants;
import edu.kit.aifb.gwifi.annotation.LanguageConstants;
import edu.kit.aifb.gwifi.mongo.MongoResource;
import edu.kit.aifb.gwifi.mongo.Property;

/**
 * Parse query to get labels combination and the corresponding entities.
 * 
 */
public class LabelChecker {

	private static final Logger _logger = Logger.getLogger(LabelChecker.class);

	private static final int MAX_ENTITY_SIZE = 5;
	private static final int MAX_LABEL_SIZE = 5;
	private static final double LABEL_PROBABILITY_THRESHOLD = 0.000;
	private static final double LABEL_ENTITY_SCORE_THRESHOLD = 0.00;

	private String[] _sLangs;

	private Map<String, DBCollection> _lang2labelCollection;
	private Map<String, DBCollection> _lang2labelEntityCollection;
	private DBCollection _langlinksCollection;

	public static void main(String[] args) {
		String configPath = "configs/MongoDBConfig.properties";
		Property.setProperties(configPath);

		LabelChecker checker = new LabelChecker("en,de,zh");	
		
		Map<String, Double> label2Score = checker.findLabelWithProbabilityByLCMention("Apple", "en");
		for(String label : label2Score.keySet()) {
			System.out.println(label + " : " + label2Score.get(label));
		}
		
//		Map<String, Double> entity2Score = checker.findEntityWithScoreByLCLabel("Online investment", "en", "en");
//		for(String entity : entity2Score.keySet()) {
//			System.out.println(entity + " : " + entity2Score.get(entity));
//		}
	}

	public LabelChecker(String s_lang) {
		_sLangs = s_lang.split(",");
		_lang2labelCollection = new HashMap<String, DBCollection>();
		_lang2labelEntityCollection = new HashMap<String, DBCollection>();
		_langlinksCollection = MongoResource.INSTANCE.getDB().getCollection(DBConstants.LANGLINKS_COLLECTION_DEZH2EN);
		for (String lang : _sLangs) {
			if (LanguageConstants.EN.equals(lang)) {
				DBCollection labelCollection = MongoResource.INSTANCE.getDB().getCollection(
						DBConstants.LABEL_COLLECTION_EN);
				DBCollection labelEntityCollection = MongoResource.INSTANCE.getDB().getCollection(
						DBConstants.LABEL_ENTITY_COLLECTION_EN);
				_lang2labelCollection.put(lang, labelCollection);
				_lang2labelEntityCollection.put(lang, labelEntityCollection);
			} else if (LanguageConstants.DE.equals(lang)) {
				DBCollection labelCollection = MongoResource.INSTANCE.getDB().getCollection(
						DBConstants.LABEL_COLLECTION_DE);
				DBCollection labelEntityCollection = MongoResource.INSTANCE.getDB().getCollection(
						DBConstants.LABEL_ENTITY_COLLECTION_DE);
				_lang2labelCollection.put(lang, labelCollection);
				_lang2labelEntityCollection.put(lang, labelEntityCollection);
			} else if (LanguageConstants.ZH.equals(lang)) {
				DBCollection labelCollection = MongoResource.INSTANCE.getDB().getCollection(
						DBConstants.LABEL_COLLECTION_ZH);
				DBCollection labelEntityCollection = MongoResource.INSTANCE.getDB().getCollection(
						DBConstants.LABEL_ENTITY_COLLECTION_ZH);
				_lang2labelCollection.put(lang, labelCollection);
				_lang2labelEntityCollection.put(lang, labelEntityCollection);
			}
		}
	}

	/**
	 * Using keyword (lower case) to find the label and its probability that the label appears in an article as an
	 * anchor text
	 * 
	 * @param mention
	 *            inputed by user, which could be a word or a phrase
	 * @param label2prob
	 *            a label and probability map where the original label and its probability will be inserted
	 * @return maxProb
	 */
	private Map<String, Double> findLabelWithProbabilityByLCMention(String mention, String lang) {
		Map<String, Double> label2prob = new HashMap<String, Double>();
		DBCollection _labelCollection = _lang2labelCollection.get(lang);
		if (_labelCollection != null) {
			// querying using lower case of the mention
			DBCursor cursor = _labelCollection.find(new BasicDBObject(DBConstants.LC_LABEL, mention.toLowerCase()))
					.sort(new BasicDBObject(DBConstants.PROBABILITY, -1)).limit(MAX_LABEL_SIZE);
			while (cursor.hasNext()) {
				DBObject next = cursor.next();
				String nextLabel = (String) next.get(DBConstants.LABEL);
				Double nextProb = (Double) next.get(DBConstants.PROBABILITY);
				Double prob = label2prob.get(nextLabel);
				if (prob == null || prob < nextProb) {
					label2prob.put(nextLabel, nextProb);
				}
			}
		}
		return label2prob;
	}

	private double findMaximalLabelProbabilityByLCMention(String mention, String lang) {
		double maxProb = 0;
		DBCollection _labelCollection = _lang2labelCollection.get(lang);
		if (_labelCollection != null) {
			// querying using lower case of the mention
			DBCursor cursor = _labelCollection.find(new BasicDBObject(DBConstants.LC_LABEL, mention.toLowerCase()))
					.sort(new BasicDBObject(DBConstants.PROBABILITY, -1)).limit(1);
			if (cursor.hasNext()) {
				double prob = (Double) cursor.next().get(DBConstants.PROBABILITY);
				if (maxProb < prob) {
					maxProb = prob;
				}
			}
		}
		return maxProb;
	}

	/**
	 * Using label to find top k entities in Label_Entity_Index
	 * 
	 * @param label
	 *            the label
	 * @return topK entities
	 */
	private Map<String, Double> findEntityWithScoreByLCLabel(String label, String sLang, String kbLang) {
		Map<String, Double> entity2score = new LinkedHashMap<String, Double>();
			DBCollection _labelEntityCollection = _lang2labelEntityCollection.get(sLang);
			if (_labelEntityCollection == null)
				return entity2score;
			DBCursor cursor = _labelEntityCollection.find(new BasicDBObject(DBConstants.LC_LABEL, label.toLowerCase()))
					.sort(new BasicDBObject(DBConstants.ASSOCIATION_STRENGTH, -1)).limit(MAX_ENTITY_SIZE);
			if (sLang.equals(kbLang)) {
				while (cursor.hasNext()) {
					DBObject next = cursor.next();
					double score = (Double) next.get(DBConstants.ASSOCIATION_STRENGTH);
					if (score < LABEL_ENTITY_SCORE_THRESHOLD)
						break;
					String entity = (String) next.get(DBConstants.ENTITY);
					Double lastScore = entity2score.get(entity);
					if (lastScore == null || lastScore < score) {
						entity2score.put(entity, score);
					}
				}
			} else {
				while (cursor.hasNext()) {
					DBObject next = cursor.next();
					double score = (Double) next.get(DBConstants.ASSOCIATION_STRENGTH);
					if (score < LABEL_ENTITY_SCORE_THRESHOLD)
						break;
					String entity = (String) next.get(DBConstants.ENTITY);
					if (LanguageConstants.EN.equals(sLang)) {
						DBObject match = _langlinksCollection.findOne(new BasicDBObject(DBConstants.TARGET_TITLE,
								entity).append(DBConstants.SOURCE_LANGUAGE, kbLang));
						entity = match == null ? "" : (String) match.get(DBConstants.SOURCE_TITLE);
					} else if (LanguageConstants.EN.equals(kbLang)) {
						DBObject match = _langlinksCollection.findOne(new BasicDBObject(DBConstants.SOURCE_TITLE,
								entity).append(DBConstants.SOURCE_LANGUAGE, sLang));
						entity = match == null ? "" : (String) match.get(DBConstants.TARGET_TITLE);
					} else {
						DBObject match = _langlinksCollection.findOne(new BasicDBObject(DBConstants.SOURCE_TITLE,
								entity));
						if (match != null) {
							String enEntity = (String) match.get(DBConstants.TARGET_TITLE);
							DBObject findOne = _langlinksCollection.findOne(new BasicDBObject(DBConstants.TARGET_TITLE,
									enEntity).append(DBConstants.SOURCE_LANGUAGE, kbLang));
							entity = findOne == null ? "" : (String) findOne.get(DBConstants.SOURCE_TITLE);
						}
					}
					if ("" != entity) {
						Double lastScore = entity2score.get(entity);
						if (lastScore == null || lastScore < score) {
							entity2score.put(entity, score);
						}
					}
				}
			}
		return entity2score;
	}

}
