package edu.kit.aifb.gwifi.mingyuzuo;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import edu.kit.aifb.gwifi.evaluation.kbp.Constants;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Page.PageType;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.mongo.DBConstants;
import edu.kit.aifb.gwifi.mongo.MongoResource;
import edu.kit.aifb.gwifi.mongo.Property;
import edu.kit.aifb.gwifi.mongo.search.MongoLanguageLinksSearcher;
import edu.kit.aifb.gwifi.util.GoogleCustomSearch;
import edu.kit.aifb.gwifi.util.nlp.LabelNormalizer;
import edu.kit.aifb.gwifi.util.nlp.Language;

public class MongoLabelEntityFromGoogleSearchIndexer
{

	private Wikipedia _wikipedia;
	private DBCollection _dbCollection;
	private MongoLanguageLinksSearcher _searcher;
	private LabelNormalizer _normalizer;

	private DBCollection _queryCollection;
	private DBCollection _googleForAllTextCollection;
	private DBCollection _resultOfGoogleSearchCollection;
	
	private DBCollection _googleForTestCollection;
	private DBCollection _resultOfGoogleForTestCollection;
	
	private PrintWriter prLinks;
	private PrintWriter prArticle;
	
	private GoogleCustomSearch _googleSearch;

	private Language _sLang;
	private Language _tLang;

	public MongoLabelEntityFromGoogleSearchIndexer(String dbDir, String sLangLabel, String tLangLabel, String APIKey,
			String engineId, String year) throws Exception
	{
		_sLang = Language.getLanguage(sLangLabel);
		_tLang = Language.getLanguage(tLangLabel);
		_dbCollection = MongoResource.INSTANCE.getDB()
				.getCollection(DBConstants.LABEL_ENTITY_COLLECTION + _sLang.getLabel() + "_" + _tLang.getLabel());
		_queryCollection = MongoResource.INSTANCE.getDB().getCollection(Constants.EVAL_QUERIES_2013);
		_googleForAllTextCollection = MongoResource.INSTANCE.getDB().getCollection("googleForAllText" + "_" + _sLang.getLabel() + "_" + year);
		_resultOfGoogleSearchCollection = MongoResource.INSTANCE.getDB().getCollection("googleForAllText_result" + "_" +_sLang.getLabel() + "_" + _tLang.getLabel() + "_" + year);
		
		_googleForTestCollection = MongoResource.INSTANCE.getDB().getCollection("googleForTest_allQuery" + "_" + _sLang.getLabel() + "_" + year);
		_resultOfGoogleForTestCollection = MongoResource.INSTANCE.getDB().getCollection("googleForTest_allQuery_result" + "_" +_sLang.getLabel() + "_" + _tLang.getLabel() + "_" + year);

		
		_searcher = new MongoLanguageLinksSearcher(_sLang, _tLang);
		_wikipedia = new Wikipedia(new File(dbDir), false);

		_googleSearch = new GoogleCustomSearch(APIKey, engineId);

		_normalizer = new LabelNormalizer(_sLang);
		
		String links = "zmy/lins.txt";
		File prLinksFile = new File(links);
		if (!prLinksFile.exists())
			prLinksFile.getParentFile().mkdirs();
		prLinks = new PrintWriter(prLinksFile);
		
		String article = "zmy/article.txt";
		File prArticleFile = new File(article);
		if (!prArticleFile.exists())
			prArticleFile.getParentFile().mkdirs();
		prArticle = new PrintWriter(prArticleFile);
		
	}

	// "configs/MongoConfig_gwifi.properties" "configs/wikipedia-template-zh.xml" "zh" "en" "" "" "2013"
	public static void main(String[] args)
	{
		try
		{
			String configPath = args[0];
			Property.setProperties(configPath);
			MongoLabelEntityFromGoogleSearchIndexer indexer = new MongoLabelEntityFromGoogleSearchIndexer(args[1],
					args[2], args[3], args[4], args[5], args[6]);
			indexer.insertLabelEntityFromGoogle();
//			indexer.get_resultOfGoogleSearchCollection().insert(new BasicDBObject("test", "right"));
//			List<String> links = new ArrayList<String>();
//			links.addAll(c)
//			indexer.test("台湾选民", links);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void insertLabelEntity() throws IOException
	{
		DBCursor curqueries = _queryCollection.find();
		while (curqueries.hasNext())
		{
			DBObject curobj = curqueries.next();
			String labelText = curobj.get(Constants.QUERIES_NAME).toString();
			String nLabelText = _normalizer.normalize(labelText);

			BasicDBObject query = new BasicDBObject();
			query.put(DBConstants.LABEL_ENTITY_NORM_LABEL_TEXT, nLabelText);
			DBObject curobjLabel = _dbCollection.findOne(query);
			if (curobjLabel == null)
			{
				List<String> links = _googleSearch.getLinksAutoChangeKeys(labelText);
				for (String link : links)
				{
					String title = link.substring(link.lastIndexOf('/'), link.length());
					Article article = _wikipedia.getArticleByTitle(title);
					if (article != null && article.getType().equals(PageType.article))
					{
						int sId = article.getId();
						BasicDBObject dbobj = new BasicDBObject(DBConstants.LABEL_ENTITY_LABEL_TEXT, labelText)
								.append(DBConstants.LABEL_ENTITY_NORM_LABEL_TEXT, nLabelText)
								.append(DBConstants.LABEL_ENTITY_SOURCE_ENTITY_ID, sId)
								.append(DBConstants.LABEL_ENTITY_SOURCE_ENTITY_NAME, article.getTitle())
								.append(DBConstants.LABEL_ENTITY_ENTITY_TYPE, article.getType().toString())
								.append(DBConstants.LABEL_ENTITY_SOURCE, DBConstants.SOURCE_GOOGLE_SEARCH);
						if (!_sLang.equals(_tLang))
						{
							String[] tIdAndEntity = _searcher.getCrossLingualEntity(sId).split(":");
							if (tIdAndEntity.length == 2)
								dbobj.append(DBConstants.LABEL_ENTITY_TARGET_ENTITY_ID,
										Integer.valueOf(tIdAndEntity[0]))
										.append(DBConstants.LABEL_ENTITY_TARGET_ENTITY_NAME, tIdAndEntity[1]);
						}
						else
						{
							dbobj.append(DBConstants.LABEL_ENTITY_TARGET_ENTITY_ID, sId)
									.append(DBConstants.LABEL_ENTITY_TARGET_ENTITY_NAME, article.getTitle());
						}
						_dbCollection.insert(dbobj);
					}
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
		_wikipedia.close();
	}

	public void insertLabelEntityFromGoogle() throws IOException
	{
//		DBCursor curqueries = _googleForAllTextCollection.find();
//		System.out.println(_googleForAllTextCollection);
		
		DBCursor curqueries = _googleForTestCollection.find();
		System.out.println(_googleForTestCollection);
		int startIndex = 0;
		int index = 0;
		
		while (curqueries.hasNext())
		{
			index++;
			if(index < startIndex)
			{
				curqueries.next();
				continue;
			}
			
			DBObject curobj = curqueries.next();
			String nlabelText = curobj.get("nLabel").toString();
			String labelText = null;
			if (nlabelText.contains(" "))
			{
				labelText = nlabelText.replace(" ", "");
			}else
			{
				labelText = nlabelText;
			}

			List<String> links = _googleSearch.getLinksAutoChangeKeys(labelText);
			if(links == null)
			{
				System.out.println("done!");
				break;
			}
			prLinks.println(labelText);
			prArticle.println(labelText);
			test(labelText, links, nlabelText);
		}

//		_resultOfGoogleSearchCollection.createIndex(new BasicDBObject(DBConstants.LABEL_ENTITY_SOURCE_ENTITY_ID, 1));
//		_resultOfGoogleSearchCollection.createIndex(new BasicDBObject(DBConstants.LABEL_ENTITY_SOURCE_ENTITY_NAME, 1));
//		_resultOfGoogleSearchCollection.createIndex(new BasicDBObject(DBConstants.LABEL_ENTITY_TARGET_ENTITY_ID, 1));
//		_resultOfGoogleSearchCollection.createIndex(new BasicDBObject(DBConstants.LABEL_ENTITY_TARGET_ENTITY_NAME, 1));
//		_resultOfGoogleSearchCollection.createIndex(new BasicDBObject(DBConstants.LABEL_ENTITY_ENTITY_TYPE, 1));
//		_resultOfGoogleSearchCollection.createIndex(new BasicDBObject(DBConstants.LABEL_ENTITY_LABEL_TEXT, 1));
//		_resultOfGoogleSearchCollection.createIndex(new BasicDBObject(DBConstants.LABEL_ENTITY_NORM_LABEL_TEXT, 1));
//		_resultOfGoogleSearchCollection.createIndex(new BasicDBObject(DBConstants.LABEL_ENTITY_SOURCE, 1));
		_wikipedia.close();
		prLinks.close();
		prArticle.close();
	}
	
	public void test(String labelText, List<String> links, String nlabelText)
	{
		
		for (String link : links)
		{
			prLinks.println(link);
			if(link == null || link.equals(""))
				continue;
			String title = link.substring(link.lastIndexOf('/')+1, link.length());
			if(title.contains("_"))
			{
				title = title.replace("_", " ");
			}
			Article article = _wikipedia.getArticleByTitle(title);
			if (article != null && article.getType().equals(PageType.article))
			{
				int sId = article.getId();
				BasicDBObject dbobj = new BasicDBObject(DBConstants.LABEL_ENTITY_LABEL_TEXT, nlabelText)
						.append(DBConstants.LABEL_ENTITY_SOURCE_ENTITY_ID, sId)
						.append(DBConstants.LABEL_ENTITY_SOURCE_ENTITY_NAME, article.getTitle())
						.append(DBConstants.LABEL_ENTITY_ENTITY_TYPE, article.getType().toString())
						.append(DBConstants.LABEL_ENTITY_SOURCE, DBConstants.SOURCE_GOOGLE_SEARCH);
				
				
				
				if (!_sLang.equals(_tLang))
				{
					//在此修改
					if(link.contains("https://zh."))
					{
						String[] tIdAndEntity = _searcher.getCrossLingualEntity(sId).split(":");
						if (tIdAndEntity.length == 2)
							dbobj.append(DBConstants.LABEL_ENTITY_TARGET_ENTITY_ID, Integer.valueOf(tIdAndEntity[0]))
									.append(DBConstants.LABEL_ENTITY_TARGET_ENTITY_NAME, tIdAndEntity[1]);
					}else
					{
						dbobj.append(DBConstants.LABEL_ENTITY_TARGET_ENTITY_ID, sId)
							.append(DBConstants.LABEL_ENTITY_TARGET_ENTITY_NAME, article.getTitle());
					}
					
					
				}
				else
				{
					dbobj.append(DBConstants.LABEL_ENTITY_TARGET_ENTITY_ID, sId)
							.append(DBConstants.LABEL_ENTITY_TARGET_ENTITY_NAME, article.getTitle());
				}
//				_resultOfGoogleSearchCollection.insert(dbobj);
//				System.out.println(_resultOfGoogleSearchCollection);
				_resultOfGoogleForTestCollection.insert(dbobj);
				System.out.println(_resultOfGoogleForTestCollection);
				prArticle.println(article.getTitle());
			}
		}
		
	}
	
	

}
