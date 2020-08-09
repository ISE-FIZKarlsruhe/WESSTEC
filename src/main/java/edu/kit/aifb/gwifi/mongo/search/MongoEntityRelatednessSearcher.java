package edu.kit.aifb.gwifi.mongo.search;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

import edu.kit.aifb.gwifi.mongo.DBConstants;
import edu.kit.aifb.gwifi.mongo.MongoResource;
import edu.kit.aifb.gwifi.util.nlp.Language;

public class MongoEntityRelatednessSearcher {

	private DBCollection _dbCollection;

	private Language _lang;

	public MongoEntityRelatednessSearcher(Language lang) throws Exception {
		_lang = lang;
		_dbCollection = MongoResource.INSTANCE.getDB()
				.getCollection(DBConstants.RESOURCERELATEDNESS_COLLECTION + _lang.getLabel());
	}

	public double getRelatedness(String sName, String tName) {
		BasicDBObject query = new BasicDBObject();
		query.put(DBConstants.RESOURCERELATEDNESS_SOURCE_ENTITY_NAME, sName);
		query.put(DBConstants.RESOURCERELATEDNESS_TARGET_ENTITY_NAME, tName);
		DBObject obj = _dbCollection.findOne(query);

		double score = 0;
		if(obj != null) {
			score = (Double) obj.get(DBConstants.ENTITY_GENERALITY);
		} else {
			query.put(DBConstants.RESOURCERELATEDNESS_SOURCE_ENTITY_NAME, tName);
			query.put(DBConstants.RESOURCERELATEDNESS_TARGET_ENTITY_NAME, sName);
			obj = _dbCollection.findOne(query);
			if(obj != null) {
				score = (Double) obj.get(DBConstants.ENTITY_GENERALITY);
			}
		}	
		
		return score;
	}

}
