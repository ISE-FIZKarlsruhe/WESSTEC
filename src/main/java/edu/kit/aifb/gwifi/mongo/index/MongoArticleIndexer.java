package edu.kit.aifb.gwifi.mongo.index;

import java.io.File;
import java.io.IOException;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Page;
import edu.kit.aifb.gwifi.model.Page.PageType;
import edu.kit.aifb.gwifi.mongo.DBConstants;
import edu.kit.aifb.gwifi.mongo.MongoResource;
import edu.kit.aifb.gwifi.mongo.Property;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.util.PageIterator;
import edu.kit.aifb.gwifi.util.nlp.Language;

/**
 * This class is responsible to build the collection named "articles_**" in MongoDB
 * 
 */
public class MongoArticleIndexer {

	private Wikipedia _wikipedia;
	private DBCollection _dbCollection;

	public MongoArticleIndexer(String wikiDir, String langLabel) throws Exception {
		Language lang = Language.getLanguage(langLabel);
		_dbCollection = MongoResource.INSTANCE.getDB().getCollection(DBConstants.ARTICLES_COLLECTION + lang.getLabel());
		_wikipedia = new Wikipedia(new File(wikiDir), false);
	}

	// "configs/configuration_esa.properties" "configs/wikipedia-template-en.xml" "en"
	public static void main(String[] args) {
		try {
			String mongoConfigPath = args[0];
			Property.setProperties(mongoConfigPath);

			MongoArticleIndexer indexer = new MongoArticleIndexer(args[1], args[2]);
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
			if (page instanceof Article && page.getType().equals(PageType.article)) {
				Article article = (Article) page;
				String title = article.getTitle();
				String text = article.getPlainText();
				if (title == null || text == null)
					continue;
				BasicDBObject dbObject = new BasicDBObject(DBConstants.ARTICLES_ID, article.getId())
						.append(DBConstants.ARTICLES_TITLE, title).append(DBConstants.ARTICLES_TEXT, text)
						.append(DBConstants.ARTICLES_DISTINCT_LINKS_IN_COUNT, article.getDistinctLinksInCount())
						.append(DBConstants.ARTICLES_DISTINCT_LINKS_OUT_COUNT, article.getDistinctLinksOutCount())
						.append(DBConstants.ARTICLES_TOTAL_LINKS_IN_COUNT, article.getTotalLinksInCount())
						.append(DBConstants.ARTICLES_TOTAL_LINKS_OUT_COUNT, article.getTotalLinksOutCount());
				_dbCollection.insert(dbObject);
			}
		}
		_dbCollection.createIndex(new BasicDBObject(DBConstants.ARTICLES_ID, 1));
		_dbCollection.createIndex(new BasicDBObject(DBConstants.ARTICLES_TITLE, 1));
		pageIterator.close();
		_wikipedia.close();
	}

}
