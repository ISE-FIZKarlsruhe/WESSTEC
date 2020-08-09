package edu.kit.aifb.gwifi.mongo.index;

import java.io.File;
import java.io.IOException;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Page;
import edu.kit.aifb.gwifi.model.Page.PageType;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.mongo.DBConstants;
import edu.kit.aifb.gwifi.mongo.MongoResource;
import edu.kit.aifb.gwifi.mongo.Property;
import edu.kit.aifb.gwifi.util.PageIterator;
import edu.kit.aifb.gwifi.util.nlp.Language;

/**
 * This class is responsible to build the collection named "EntityIndex_**" in MongoDB
 * 
 */
public class MongoEntityIndexer {

	private Wikipedia _wikipedia;
	private DBCollection _dbCollection;

	private long _allLinksCount = 0;

	public MongoEntityIndexer(String dbDir, String langLabel) throws Exception {
		Language lang = Language.getLanguage(langLabel);
		_dbCollection = MongoResource.INSTANCE.getDB().getCollection(DBConstants.ENTITY_COLLECTION + lang.getLabel());
		_wikipedia = new Wikipedia(new File(dbDir), false);
		_allLinksCount = calAllLinksCount();
	}

	// "configs/configuration_gwifi.properties" "configs/wikipedia-template-en.xml" "en" 
	public static void main(String[] args) {
		try {
			String configPath = args[0];
			Property.setProperties(configPath);
			MongoEntityIndexer indexer = new MongoEntityIndexer(args[1], args[2]);
			indexer.insertData();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void insertData() throws IOException {
		// the argument "PageType.article" includes disambiguation pages
//		PageIterator pageIterator = _wikipedia.getPageIterator(PageType.article);
		PageIterator pageIterator = _wikipedia.getPageIterator();
		int i = 0;
		while (pageIterator.hasNext()) {
			if(++i%100000 == 0) {
				System.out.println(i + " pages have been processed!");
			}
			Page page = pageIterator.next();
			if (page instanceof Article && page.getType().equals(PageType.article)) {
				Article article = (Article) page;
				String title = article.getTitle();
				BasicDBObject dbObject = new BasicDBObject(DBConstants.ENTITY_ID, article.getId())
						.append(DBConstants.ENTITY_NAME, title)
						.append(DBConstants.ENTITY_GENERALITY, calGeneralityOfEntity(article))
						.append(DBConstants.ENTITY_DISTINCT_LINKS_IN_COUNT, article.getDistinctLinksInCount())
						.append(DBConstants.ENTITY_DISTINCT_LINKS_OUT_COUNT, article.getDistinctLinksOutCount())
						.append(DBConstants.ENTITY_TOTAL_LINKS_IN_COUNT, article.getTotalLinksInCount())
						.append(DBConstants.ENTITY_TOTAL_LINKS_OUT_COUNT, article.getTotalLinksOutCount());
				_dbCollection.insert(dbObject);
			}
		}
		_dbCollection.createIndex(new BasicDBObject(DBConstants.ENTITY_ID, 1));
		_dbCollection.createIndex(new BasicDBObject(DBConstants.ENTITY_NAME, 1));
		pageIterator.close();
		_wikipedia.close();
	}

	/**
	 * this method is used to calculate the generality of an entity, the approach used is from the paper
	 * "Semantic Similarity Measurements for Multi-lingual Short Texts Using Wikipedia_Nakamura" , i.e., the equation
	 * (6)
	 */
	private double calGeneralityOfEntity(Article article) {
//		return (1.0 * (article.getTotalLinksInCount() + article.getTotalLinksOutCount())) / (1.0 * _allLinksCount);
		return (1.0 * article.getTotalLinksInCount()) / (1.0 * _allLinksCount);
	}

	/**
	 * en: 185364999
	 * 
	 * @return
	 */
	private long calAllLinksCount() {
		PageIterator pi = _wikipedia.getPageIterator();
		long allLinksCount = 0;
		while (pi.hasNext()) {
			Page page = pi.next();
			if (page instanceof Article) {
				Article art = (Article) page;
//				allLinksCount += (art.getTotalLinksInCount() + art.getTotalLinksOutCount());
				allLinksCount += art.getTotalLinksInCount();
			}
		}
		pi.close();
		return allLinksCount;
	}
}
