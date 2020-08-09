package edu.kit.aifb.gwifi.mongo.index;

import java.io.File;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

import edu.kit.aifb.gwifi.model.Label;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.mongo.DBConstants;
import edu.kit.aifb.gwifi.mongo.MongoResource;
import edu.kit.aifb.gwifi.mongo.Property;
import edu.kit.aifb.gwifi.util.LabelIterator;
import edu.kit.aifb.gwifi.util.nlp.LabelNormalizer;
import edu.kit.aifb.gwifi.util.nlp.Language;

/**
 * This class is responsible to build the collection named "LabelIndex_**" in MongoDB
 * 
 */
public class MongoLabelIndexer {

	private DBCollection _dbCollection;
	private Wikipedia _wikipedia;
	private LabelNormalizer _normalizer;
	
	private Language _lang;

	public MongoLabelIndexer(String dbDir, String langLabel) throws Exception {
		_lang = Language.getLanguage(langLabel);
		_dbCollection = MongoResource.INSTANCE.getDB().getCollection(DBConstants.LABEL_COLLECTION + _lang.getLabel());
		_wikipedia = new Wikipedia(new File(dbDir), false);
		_normalizer = new LabelNormalizer(_lang);
	}

	// "configs/configuration_gwifi.properties" "configs/wikipedia-template-en.xml" "en"
	public static void main(String[] args) {
		try {
			String configPath = args[0];
			Property.setProperties(configPath);
			MongoLabelIndexer indexer = new MongoLabelIndexer(args[1], args[2]);
			indexer.insertLabel();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void insertLabel() throws Exception {
		LabelIterator labelIterator = _wikipedia.getLabelIterator(null);
		int i = 0;
		while (labelIterator.hasNext()) {
			if (++i % 100000 == 0) {
				System.out.println(i + " lables have been processed!");
			}
			Label label = labelIterator.next();
			long docCount = label.getDocCount();
			long linkDocCount = label.getLinkDocCount();
			
			String labelText = label.getText();
			String nLabelText = _normalizer.normalize(labelText);
			
			BasicDBObject dbObject = new BasicDBObject(DBConstants.LABEL_TEXT, labelText)
					.append(DBConstants.LABEL_NORM_TEXT, nLabelText)
					.append(DBConstants.LABEL_PROBABILITY, label.getLinkProbability()).append(DBConstants.LABEL_DOC_COUNT, docCount)
					.append(DBConstants.LABEL_OCC_COUNT, label.getOccCount()).append(DBConstants.LABEL_LINK_DOC_COUNT, linkDocCount)
					.append(DBConstants.LABEL_LINK_OCC_COUNT, label.getLinkOccCount())
					.append(DBConstants.LABEL_SOURCE, DBConstants.SOURCE_WIKI_LABEL);
			_dbCollection.insert(dbObject);
		}
		_dbCollection.createIndex(new BasicDBObject(DBConstants.LABEL_TEXT, 1));
		_dbCollection.createIndex(new BasicDBObject(DBConstants.LABEL_NORM_TEXT, 1));
		_dbCollection.createIndex(new BasicDBObject(DBConstants.LABEL_PROBABILITY, -1));
		_dbCollection.createIndex(new BasicDBObject(DBConstants.LABEL_LINK_DOC_COUNT, -1));
		_dbCollection.createIndex(new BasicDBObject(DBConstants.LABEL_LINK_OCC_COUNT, -1));
		_dbCollection.createIndex(new BasicDBObject(DBConstants.LABEL_DOC_COUNT, -1));
		_dbCollection.createIndex(new BasicDBObject(DBConstants.LABEL_OCC_COUNT, -1));
		_dbCollection.createIndex(new BasicDBObject(DBConstants.LABEL_SOURCE, 1));
		labelIterator.close();
		_wikipedia.close();
	}
}
