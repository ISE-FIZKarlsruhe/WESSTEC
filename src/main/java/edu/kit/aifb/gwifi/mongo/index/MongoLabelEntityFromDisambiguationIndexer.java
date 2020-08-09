package edu.kit.aifb.gwifi.mongo.index;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Disambiguation;
import edu.kit.aifb.gwifi.model.Page.PageType;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.mongo.DBConstants;
import edu.kit.aifb.gwifi.mongo.MongoResource;
import edu.kit.aifb.gwifi.mongo.Property;
import edu.kit.aifb.gwifi.mongo.search.MongoLanguageLinksSearcher;
import edu.kit.aifb.gwifi.util.PageIterator;
import edu.kit.aifb.gwifi.util.nlp.LabelNormalizer;
import edu.kit.aifb.gwifi.util.nlp.Language;

public class MongoLabelEntityFromDisambiguationIndexer {

	private Wikipedia _wikipedia;
	private DBCollection _dbCollection;
	private MongoLanguageLinksSearcher _searcher;
	private LabelNormalizer _normalizer;

	private Language _sLang;
	private Language _tLang;

	public MongoLabelEntityFromDisambiguationIndexer(String dbDir, String sLangLabel, String tLangLabel)
			throws Exception {
		_sLang = Language.getLanguage(sLangLabel);
		_tLang = Language.getLanguage(tLangLabel);
		_dbCollection = MongoResource.INSTANCE.getDB()
				.getCollection(DBConstants.LABEL_ENTITY_COLLECTION + _sLang.getLabel() + "_" + _tLang.getLabel());
		_searcher = new MongoLanguageLinksSearcher(_sLang, _tLang);
		_wikipedia = new Wikipedia(new File(dbDir), false);
		_normalizer = new LabelNormalizer(_sLang);
	}

	// "configs/MongoConfig_gwifi.properties" "configs/wikipedia-template-zh.xml" "zh" "en"
	public static void main(String[] args) {
		try {
			String configPath = args[0];
			Property.setProperties(configPath);
			MongoLabelEntityFromDisambiguationIndexer indexer = new MongoLabelEntityFromDisambiguationIndexer(args[1],
					args[2], args[3]);
			indexer.insertLabelEntity();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void insertLabelEntity() throws IOException {
		PageIterator pageIterator = _wikipedia.getPageIterator(PageType.disambiguation);
		int i = 0;
		while (pageIterator.hasNext()) {
			++i;
			if (i % 100000 == 0) {
				System.out.println(i + " pages have been processed!");
			}
			Article article = (Article) pageIterator.next();

			if (article instanceof Disambiguation) {
				String labelText = article.getTitle();
				String nLabelText = _normalizer.normalize(labelText);

				String markup = article.getMarkup();
				if (markup == null)
					continue;
				List<Article> senses = new ArrayList<Article>();
				Matcher m = Pattern.compile("((?<=\\u005B\\u005B).*?(?=\\u005D\\u005D))").matcher(markup);
				while (m.find()) {
					String entity = m.group();

					if (entity.contains("|"))
						entity = entity.substring(0, entity.indexOf("|"));
					if (entity != null && !entity.equals("")) {
//							&& (nEntity.contains(nLabelText) || _sLang.equals(Language.ZH))) {
						Article sense = _wikipedia.getArticleByTitle(entity);
						if (sense != null) {
							senses.add(sense);
						}
					}
				}

				for (Article sense : senses) {
					int sId = sense.getId();
					BasicDBObject dbobj = new BasicDBObject(DBConstants.LABEL_ENTITY_LABEL_TEXT, labelText)
							.append(DBConstants.LABEL_ENTITY_NORM_LABEL_TEXT, nLabelText)
							.append(DBConstants.LABEL_ENTITY_SOURCE_ENTITY_ID, sId)
							.append(DBConstants.LABEL_ENTITY_SOURCE_ENTITY_NAME, sense.getTitle())
							.append(DBConstants.LABEL_ENTITY_ENTITY_TYPE, sense.getType().toString())
							.append(DBConstants.LABEL_ENTITY_SOURCE, DBConstants.SOURCE_WIKI_DISAMBIGUATION);
					if (!_sLang.equals(_tLang)) {
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
		_dbCollection.createIndex(new BasicDBObject(DBConstants.LABEL_ENTITY_SOURCE, 1));
		pageIterator.close();
		_wikipedia.close();
	}

}
