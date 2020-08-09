package edu.kit.aifb.gwifi.mongo.search;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

import edu.kit.aifb.gwifi.mongo.DBConstants;
import edu.kit.aifb.gwifi.mongo.MongoResource;
import edu.kit.aifb.gwifi.util.nlp.Language;

public class MongoLanguageLinksSearcher {

	private DBCollection _dbLanglinksCollection;
	
	private Language _sLang;
	private Language _tLang;
	
	public MongoLanguageLinksSearcher(Language sLang, Language tLang) throws Exception {
		_sLang = sLang;
		_tLang = tLang;
		_dbLanglinksCollection = MongoResource.INSTANCE.getDB().getCollection(DBConstants.LANGLINKS_COLLECTION);
	}

	public String getCrossLingualEntity(int sId) {
		BasicDBObject query = new BasicDBObject();
		query.put(DBConstants.LANGLINKS_SOURCE_ID, sId);
		query.put(DBConstants.LANGLINKS_SOURCE_LANGUAGE, _sLang.getLabel());
		query.put(DBConstants.LANGLINKS_TARGET_LANGUAGE, _tLang.getLabel());
		DBObject obj = _dbLanglinksCollection.findOne(query);

		if (obj != null) {
			String tId = obj.get(DBConstants.LANGLINKS_TARGET_ID).toString();
			String tEntity = obj.get(DBConstants.LANGLINKS_TARGET_ENTITY).toString();
			return tId + ":" + tEntity;
		} else {
			return ":";
		}
	}
	
	public static void main(String[] args) throws Exception {
	}

}
