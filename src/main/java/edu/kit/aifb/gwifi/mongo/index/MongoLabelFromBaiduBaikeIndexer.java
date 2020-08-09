package edu.kit.aifb.gwifi.mongo.index;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.List;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

import edu.kit.aifb.gwifi.mongo.DBConstants;
import edu.kit.aifb.gwifi.mongo.MongoResource;
import edu.kit.aifb.gwifi.mongo.Property;
import edu.kit.aifb.gwifi.util.nlp.LabelNormalizer;
import edu.kit.aifb.gwifi.util.nlp.Language;

/**
 * This class is responsible to build the collection named "LabelIndex_**" in MongoDB
 * 
 */
public class MongoLabelFromBaiduBaikeIndexer {

	private DBCollection _dbCollection;
	private BufferedReader _reader;
	private LabelNormalizer _normalizer;

	private Language _lang;

	public MongoLabelFromBaiduBaikeIndexer(String file, String langLabel) throws Exception {
		_lang = Language.getLanguage(langLabel);

		_dbCollection = MongoResource.INSTANCE.getDB().getCollection(DBConstants.LABEL_COLLECTION + _lang.getLabel());
		_reader = new BufferedReader(new FileReader(file));
		_normalizer = new LabelNormalizer(_lang);
	}

	// "configs/configuration_gwifi.properties" "zh-en_links.dat" "zh"
	public static void main(String[] args) {
		try {
			String configPath = args[0];
			Property.setProperties(configPath);
			MongoLabelFromBaiduBaikeIndexer indexer = new MongoLabelFromBaiduBaikeIndexer(args[1], args[2]);
			indexer.insertLabel();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void insertLabel() throws Exception {
		int i = 0;
		String line;
		while ((line = _reader.readLine()) != null) {
			String[] mapping = line.split("\t");

			if (++i % 100000 == 0) {
				System.out.println(i + " lables have been processed!");
			}

			String labelText = mapping[0];
			List<String> nlabels = _normalizer.getSegments(labelText);
			for (String nlabel : nlabels) {
				BasicDBObject dbObject = new BasicDBObject(DBConstants.LABEL_TEXT, labelText)
						.append(DBConstants.LABEL_NORM_TEXT, nlabel.toLowerCase())
						.append(DBConstants.LABEL_SOURCE, DBConstants.SOURCE_BAIDU);
				_dbCollection.insert(dbObject);
			}
		}

		_dbCollection.createIndex(new BasicDBObject(DBConstants.LABEL_TEXT, 1));
		_dbCollection.createIndex(new BasicDBObject(DBConstants.LABEL_NORM_TEXT, 1));
		_dbCollection.createIndex(new BasicDBObject(DBConstants.LABEL_SOURCE, 1));
		_reader.close();
	}
}
