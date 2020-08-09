package edu.kit.aifb.gwifi.mongo.index;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

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
import edu.kit.aifb.gwifi.util.nlp.LabelNormalizer;
import edu.kit.aifb.gwifi.util.nlp.Language;

/**
 * This class is responsible to build the collection named "LabelEntityIndex_**" in MongoDB
 * 
 */
public class MongoLabelEntityFromBaiduBaikeIndexer {

	private Wikipedia _wikipedia;
	private DBCollection _dbCollection;
	private LabelNormalizer _normalizer;
	
	private BufferedReader _reader;

	private Language _sLang;
	private Language _tLang;

	public MongoLabelEntityFromBaiduBaikeIndexer(String dbDir, String file, String sLangLabel, String tLangLabel)
			throws Exception {
		_sLang = Language.getLanguage(sLangLabel);
		_tLang = Language.getLanguage(tLangLabel);
		_dbCollection = MongoResource.INSTANCE.getDB()
				.getCollection(DBConstants.LABEL_ENTITY_COLLECTION + _sLang.getLabel() + "_" + _tLang.getLabel());
		_wikipedia = new Wikipedia(new File(dbDir), false);
		_reader = new BufferedReader(new FileReader(file));
		_normalizer = new LabelNormalizer(_sLang);
	}

	// "configs/configuration_gwifi.properties" "configs/wikipedia-template-zh.xml" "zh-en_links.dat" "zh" "en"
	public static void main(String[] args) {
		try {
			String configPath = args[0];
			Property.setProperties(configPath);
			MongoLabelEntityFromBaiduBaikeIndexer indexer = new MongoLabelEntityFromBaiduBaikeIndexer(args[1], args[2], args[3],
					args[4]);
			indexer.insertLabelEntity();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void insertLabelEntity() throws IOException {
		int i = 0;
		String line;
		while ((line = _reader.readLine()) != null) {
			String[] mapping = line.split("\t");

			if (++i % 100000 == 0) {
				System.out.println(i + " lables have been processed!");
			}

			String title = mapping[1];
			Article article = _wikipedia.getArticleByTitle(title);
			if (article != null && article.getType().equals(PageType.article)) {
				String labelText = mapping[0];
				List<String> nlabels = _normalizer.getSegments(labelText);
				for (String nlabel : nlabels) {
					BasicDBObject dbobj =	new BasicDBObject(DBConstants.LABEL_ENTITY_LABEL_TEXT, labelText)
					.append(DBConstants.LABEL_ENTITY_NORM_LABEL_TEXT, nlabel)
					.append(DBConstants.LABEL_ENTITY_TARGET_ENTITY_ID, Integer.valueOf(article.getId()))
					.append(DBConstants.LABEL_ENTITY_TARGET_ENTITY_NAME, title)
					.append(DBConstants.LABEL_ENTITY_ENTITY_TYPE, article.getType().toString())
					.append(DBConstants.LABEL_ENTITY_SOURCE, DBConstants.SOURCE_BAIDU);
				_dbCollection.insert(dbobj);
				}
			}
		}
		_dbCollection.createIndex(new BasicDBObject(DBConstants.LABEL_ENTITY_TARGET_ENTITY_ID, 1));
		_dbCollection.createIndex(new BasicDBObject(DBConstants.LABEL_ENTITY_TARGET_ENTITY_NAME, 1));
		_dbCollection.createIndex(new BasicDBObject(DBConstants.LABEL_ENTITY_ENTITY_TYPE, 1));
		_dbCollection.createIndex(new BasicDBObject(DBConstants.LABEL_ENTITY_LABEL_TEXT, 1));
		_dbCollection.createIndex(new BasicDBObject(DBConstants.LABEL_ENTITY_NORM_LABEL_TEXT, 1));
		_dbCollection.createIndex(new BasicDBObject(DBConstants.LABEL_ENTITY_SOURCE, 1));
		_reader.close();
		_wikipedia.close();
	}

}
