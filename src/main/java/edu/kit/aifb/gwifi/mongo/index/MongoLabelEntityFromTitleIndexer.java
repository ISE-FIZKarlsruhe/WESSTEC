package edu.kit.aifb.gwifi.mongo.index;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Page;
import edu.kit.aifb.gwifi.model.Page.PageType;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.mongo.DBConstants;
import edu.kit.aifb.gwifi.mongo.MongoResource;
import edu.kit.aifb.gwifi.mongo.Property;
import edu.kit.aifb.gwifi.mongo.search.MongoLanguageLinksSearcher;
import edu.kit.aifb.gwifi.util.PageIterator;
import edu.kit.aifb.gwifi.util.nlp.LabelNormalizer;
import edu.kit.aifb.gwifi.util.nlp.Language;

/**
 * This class is responsible to build the collection named "LabelEntityIndex_**" in MongoDB
 * 
 */
public class MongoLabelEntityFromTitleIndexer {

	private DBCollection _dbCollection;
	private Wikipedia _wikipedia;
	private LabelNormalizer _normalizer;

	private MongoLanguageLinksSearcher _searcher;

	private Language _sLang;
	private Language _tLang;

	public MongoLabelEntityFromTitleIndexer(String dbDir, String sLangLabel, String tLangLabel) throws Exception {
		_sLang = Language.getLanguage(sLangLabel);
		_tLang = Language.getLanguage(tLangLabel);
		_dbCollection = MongoResource.INSTANCE.getDB()
				.getCollection(DBConstants.LABEL_ENTITY_COLLECTION + _sLang.getLabel() + "_" + _tLang.getLabel());
		_searcher = new MongoLanguageLinksSearcher(_sLang, _tLang);
		_wikipedia = new Wikipedia(new File(dbDir), false);
		_normalizer = new LabelNormalizer(_sLang);
	}

	// "configs/configuration_gwifi.properties" "configs/wikipedia-template-zh.xml" "zh" "en"
	public static void main(String[] args) {
		try {
			String configPath = args[0];
			Property.setProperties(configPath);
			MongoLabelEntityFromTitleIndexer indexer = new MongoLabelEntityFromTitleIndexer(args[1], args[2], args[3]);
			indexer.insertData();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void insertData() throws IOException {
		PageIterator pageIterator = _wikipedia.getPageIterator();
		int i = 0;
		while (pageIterator.hasNext()) {
			if (++i % 100000 == 0) {
				System.out.println(i + " pages have been processed!");
			}
			Page page = pageIterator.next();
			if (page.getType().equals(PageType.article)) {
				Article article = (Article) page;
				int sId = article.getId();
				String title = article.getTitle();
				List<String> nlabels = _normalizer.getSegments(title);
				for (String nlabel : nlabels) {
					BasicDBObject dbobj = new BasicDBObject(DBConstants.LABEL_ENTITY_LABEL_TEXT, title)
							.append(DBConstants.LABEL_ENTITY_NORM_LABEL_TEXT, nlabel)
							.append(DBConstants.LABEL_ENTITY_SOURCE_ENTITY_ID, sId)
							.append(DBConstants.LABEL_ENTITY_SOURCE_ENTITY_NAME, title)
							.append(DBConstants.LABEL_ENTITY_ENTITY_TYPE, page.getType().toString())
							.append(DBConstants.LABEL_ENTITY_SOURCE, DBConstants.SOURCE_WIKI_TITLE);
					if(!_sLang.equals(_tLang)) {
						String[] tIdAndEntity = _searcher.getCrossLingualEntity(sId).split(":");
						if (tIdAndEntity.length == 2)
							dbobj.append(DBConstants.LABEL_ENTITY_TARGET_ENTITY_ID, Integer.valueOf(tIdAndEntity[0]))
									.append(DBConstants.LABEL_ENTITY_TARGET_ENTITY_NAME, tIdAndEntity[1]);
					} else {
						dbobj.append(DBConstants.LABEL_ENTITY_TARGET_ENTITY_ID, sId)
						.append(DBConstants.LABEL_ENTITY_TARGET_ENTITY_NAME, title);
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
		_dbCollection.createIndex(new BasicDBObject(DBConstants.LABEL_ENTITY_SOURCE, 1));
		pageIterator.close();
		_wikipedia.close();
	}

}
