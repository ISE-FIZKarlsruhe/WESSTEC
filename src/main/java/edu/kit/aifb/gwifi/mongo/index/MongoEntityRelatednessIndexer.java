package edu.kit.aifb.gwifi.mongo.index;

import java.io.File;

import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DBCollection;

import edu.kit.aifb.gwifi.comparison.ArticleComparer;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Page;
import edu.kit.aifb.gwifi.model.Page.PageType;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.mongo.DBConstants;
import edu.kit.aifb.gwifi.mongo.MongoResource;
import edu.kit.aifb.gwifi.mongo.Property;
import edu.kit.aifb.gwifi.util.PageIterator;
import edu.kit.aifb.gwifi.util.nlp.Language;

public class MongoEntityRelatednessIndexer {

	private static DBCollection _dbCollection;
	private static BulkWriteOperation builder;
	private static Wikipedia wikipedia;
	private static ArticleComparer comparer;
	private static PageIterator iter;

	public MongoEntityRelatednessIndexer(String wikiDir, String langLabel) throws Exception {
		File databaseDirectory = new File(wikiDir);
		wikipedia = new Wikipedia(databaseDirectory, false);
		System.out.println("The Wikipedia environment has been initialized.");
		comparer = new ArticleComparer(wikipedia);
		iter = wikipedia.getPageIterator(PageType.article);

		Language lang = Language.getLanguage(langLabel);
		_dbCollection = MongoResource.INSTANCE.getDB()
				.getCollection(DBConstants.RESOURCERELATEDNESS_COLLECTION + lang.getLabel());
	}

	// "configs/configuration_gwifi.properties" "configs/wikipedia-template-en.xml" "en"
	public static void main(String[] args) {
		try {
			String configPath = args[0];
			Property.setProperties(configPath);
			MongoEntityRelatednessIndexer indexer = new MongoEntityRelatednessIndexer(args[1], args[2]);
			indexer.insertData();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void insertData() {
		int i = 0;
		while (iter.hasNext()) {
			builder = _dbCollection.initializeUnorderedBulkOperation();
			i++;
			if (i % 1000 == 0) {
				System.out.println(i + " articles have been processed!");
			}
			Page page = iter.next();
			if (!page.getType().equals(PageType.article)) {
				continue;
			}
			String title = page.getTitle();
			if (title == null || title.equals("")) {
				continue;
			}
			Article article = wikipedia.getArticleByTitle(title);
			if (article == null) {
				System.out.println("Could not find exact match. Searching through anchors instead");
				continue;
			} else {
				for (Article tOUT : article.getLinksOut()) {
					try {
						double relatedness = comparer.getRelatedness(article, tOUT);
						createDocumentByBulk(article, tOUT, relatedness, true);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				for (Article tIN : article.getLinksIn()) {
					try {
						double relatedness = comparer.getRelatedness(article, tIN);
						createDocumentByBulk(article, tIN, relatedness, false);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			try {
				builder.execute();
			} catch (Exception e) {
				continue;
			}
		}
		createIndex();
		iter.close();
	}

	public static void createDocumentByBulk(Article sourceArticle, Article targetArticle, double relatedness,
			boolean linkOut) {
		BasicDBObject doc = new BasicDBObject(DBConstants.RESOURCERELATEDNESS_SOURCE_ID, sourceArticle.getId())
				.append(DBConstants.RESOURCERELATEDNESS_TARGET_ID, targetArticle.getId())
				.append(DBConstants.RESOURCERELATEDNESS_SOURCE_ENTITY_NAME, sourceArticle.getTitle())
				.append(DBConstants.RESOURCERELATEDNESS_TARGET_ENTITY_NAME, targetArticle.getTitle())
				.append(DBConstants.RESOURCERELATEDNESS_SCORE, relatedness)
				.append(DBConstants.RESOURCERELATEDNESS_LINK_OUT, linkOut);
		builder.insert(doc);
	}

	public static void createIndex() {
		_dbCollection.createIndex(new BasicDBObject(DBConstants.RESOURCERELATEDNESS_SOURCE_ID, 1));
		_dbCollection.createIndex(new BasicDBObject(DBConstants.RESOURCERELATEDNESS_TARGET_ID, 1));
		_dbCollection.createIndex(new BasicDBObject(DBConstants.RESOURCERELATEDNESS_SOURCE_ENTITY_NAME, 1));
		_dbCollection.createIndex(new BasicDBObject(DBConstants.RESOURCERELATEDNESS_TARGET_ENTITY_NAME, 1));
		_dbCollection.createIndex(new BasicDBObject(DBConstants.RESOURCERELATEDNESS_SCORE, -1));
		_dbCollection.createIndex(new BasicDBObject(DBConstants.RESOURCERELATEDNESS_LINK_OUT, 1));
	}

}
