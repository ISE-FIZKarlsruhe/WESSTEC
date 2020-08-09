package edu.kit.aifb.gwifi.mongo.search;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

import edu.kit.aifb.gwifi.mongo.DBConstants;
import edu.kit.aifb.gwifi.mongo.MongoResource;
import edu.kit.aifb.gwifi.util.nlp.Language;

public class MongoEntitySearcher {

	private DBCollection _dbCollection;

	private Language _lang;

	public MongoEntitySearcher(Language lang) throws Exception {
		_lang = lang;
		_dbCollection = MongoResource.INSTANCE.getDB().getCollection(DBConstants.ENTITY_COLLECTION + _lang.getLabel());
	}

	public DBEntity getEntity(String name) {
		BasicDBObject query = new BasicDBObject();

		query.put(DBConstants.ENTITY_NAME, name);
		DBObject obj = _dbCollection.findOne(query);

		if(obj != null)
			return getEntity(obj);
		else 
			return null;
	}

	public DBEntity getEntity(int id) {
		BasicDBObject query = new BasicDBObject();

		query.put(DBConstants.ENTITY_ID, id);
		DBObject obj = _dbCollection.findOne(query);

		if(obj != null)
			return getEntity(obj);
		else 
			return null;
	}

	public DBEntity getEntity(DBObject obj) {
		DBEntity dbe = new DBEntity();
		dbe.id = (Integer) obj.get(DBConstants.ENTITY_ID);
		dbe.name = (String) obj.get(DBConstants.ENTITY_NAME);
		dbe.generality = (Double) obj.get(DBConstants.ENTITY_GENERALITY);
		dbe.totalLinksIn = (Integer) obj.get(DBConstants.ENTITY_TOTAL_LINKS_IN_COUNT);
		dbe.distinctLinksIn = (Integer) obj.get(DBConstants.ENTITY_DISTINCT_LINKS_IN_COUNT);
		dbe.totalLinksOut = (Integer) obj.get(DBConstants.ENTITY_TOTAL_LINKS_OUT_COUNT);
		dbe.distinctLinksOut = (Integer) obj.get(DBConstants.ENTITY_DISTINCT_LINKS_OUT_COUNT);

		return dbe;
	}

	public class DBEntity {

		private int id;
		private String name;

		private double generality;

		private int totalLinksIn;
		private int distinctLinksIn;
		private int totalLinksOut;
		private int distinctLinksOut;

		public int getId() {
			return id;
		}

		public String getName() {
			return name;
		}
		
		public double getGenerality() {
			return generality;
		}

		public long getTotalLinksIn() {
			return totalLinksIn;
		}

		public long getDistinctLinksIn() {
			return distinctLinksIn;
		}

		public long getTotalLinksOut() {
			return totalLinksOut;
		}

		public long getDistinctLinksOut() {
			return distinctLinksOut;
		}

	}

}
