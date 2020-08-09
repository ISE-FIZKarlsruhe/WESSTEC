package edu.kit.aifb.gwifi.mongo.index;

import java.io.File;
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
import edu.kit.aifb.gwifi.util.PageIterator;
import edu.kit.aifb.gwifi.util.nlp.LabelNormalizer;
import edu.kit.aifb.gwifi.util.nlp.Language;

/**
 * This class is responsible to build the collection named "LabelIndex_**" in MongoDB
 * 
 */
public class MongoLabelFromTitleIndexer {

	private DBCollection _dbCollection;
	private Wikipedia _wikipedia;
	private LabelNormalizer _normalizer;

	private Language _lang;

	public MongoLabelFromTitleIndexer(String dbDir, String langLabel) throws Exception {
		_lang = Language.getLanguage(langLabel);
		_dbCollection = MongoResource.INSTANCE.getDB().getCollection(DBConstants.LABEL_COLLECTION + _lang.getLabel());
		_wikipedia = new Wikipedia(new File(dbDir), false);
		_normalizer = new LabelNormalizer(_lang);
	}

	// "configs/configuration_gwifi.properties" "configs/wikipedia-template-zh.xml" "zh"
	public static void main(String[] args) {
		double start = System.currentTimeMillis() / 1000 / 60;
		try {
			String configPath = args[0];
			Property.setProperties(configPath);
			MongoLabelFromTitleIndexer indexer = new MongoLabelFromTitleIndexer(args[1], args[2]);
			indexer.insertLabel();
		} catch (Exception e) {
			e.printStackTrace();
		}
		double end = System.currentTimeMillis() / 1000 / 60;
		System.out.println("The total time in min: " + (end - start));
	}

	public void insertLabel() throws Exception {
		PageIterator pageIterator = _wikipedia.getPageIterator();
		int i = 0;
		while (pageIterator.hasNext()) {
			if (++i % 100000 == 0) {
				System.out.println(i + " pages have been processed!");
			}
			Page page = pageIterator.next();
			if (page.getType().equals(PageType.article)) {
				Article article = (Article) page;
				String title = article.getTitle();
				List<String> nlabels = _normalizer.getSegments(title);
				for (String nlabel : nlabels) {
					BasicDBObject dbObject = new BasicDBObject(DBConstants.LABEL_TEXT, title)
							.append(DBConstants.LABEL_NORM_TEXT, nlabel.toLowerCase())
							.append(DBConstants.LABEL_SOURCE, DBConstants.SOURCE_WIKI_TITLE);
					_dbCollection.insert(dbObject);
				}
			}
		}
		_dbCollection.createIndex(new BasicDBObject(DBConstants.LABEL_TEXT, 1));
		_dbCollection.createIndex(new BasicDBObject(DBConstants.LABEL_NORM_TEXT, 1));
		_dbCollection.createIndex(new BasicDBObject(DBConstants.LABEL_SOURCE, 1));
		pageIterator.close();
		_wikipedia.close();
	}

}
