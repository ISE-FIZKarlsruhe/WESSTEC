package edu.kit.aifb.gwifi.mingyuzuo;

import java.io.File;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.mongo.DBConstants;
import edu.kit.aifb.gwifi.mongo.MongoResource;
import edu.kit.aifb.gwifi.mongo.Property;

public class RedirectForKBPKB {
	private Wikipedia _wikipedia;
	private DBCollection _dbCollection;
	private DBCollection _dbCollection_redirect;
	
	public RedirectForKBPKB(String dbDir) throws Exception
	{
		_dbCollection = MongoResource.INSTANCE.getDB().getCollection("kbp_kb");
		_dbCollection_redirect = MongoResource.INSTANCE.getDB().getCollection("kbp_kb_redirect");
		_wikipedia = new Wikipedia(new File(dbDir), false);
	}

	// "configs/MongoConfig_gwifi.properties" "configs/wikipedia-template-en.xml"
	public static void main(String[] args) {
		try {
			String configPath = args[0];
			Property.setProperties(configPath);
			RedirectForKBPKB indexer = new RedirectForKBPKB(args[1]);
			indexer.redirecter();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void redirecter() {
		DBCursor cur = _dbCollection.find();
		cur.addOption(com.mongodb.Bytes.QUERYOPTION_NOTIMEOUT);
		
		int i = 0;
		while (cur.hasNext())
		{
			++i;
			if (i % 10000 == 0) {
				System.out.println(i + " pages have been processed!");
			}
			
			DBObject curobj = cur.next();
			String entity_id = curobj.get("entitiy_id").toString();
			String wiki_title = curobj.get("wiki_title").toString();
			String entity_name = curobj.get("entity_name").toString();
			String entity_type = curobj.get("entity_type").toString();
			String wiki_text = curobj.get("wiki_text").toString();
			
			Article article = _wikipedia.getArticleByTitle(entity_name);
			String entity_name_redirect = null;
			if(article == null)
				entity_name_redirect = entity_name;
			else
				entity_name_redirect = article.getTitle();
			
			BasicDBObject dbobj = new BasicDBObject("entity_id", entity_id)
					.append("wiki_title", wiki_title)
					.append("entity_name", entity_name_redirect)
					.append("entity_type", entity_type)
					.append("wiki_text", wiki_text);
			_dbCollection_redirect.insert(dbobj);
		}
		_dbCollection_redirect.createIndex(new BasicDBObject("entity_id", 1));
		_dbCollection_redirect.createIndex(new BasicDBObject("wiki_title", 1));
		_dbCollection_redirect.createIndex(new BasicDBObject("entity_name", 1));
		_dbCollection_redirect.createIndex(new BasicDBObject("entity_type", 1));
		_wikipedia.close();
	}
	
}
