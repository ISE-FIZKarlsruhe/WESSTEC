package edu.kit.aifb.gwifi.mongo.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import edu.kit.aifb.gwifi.mongo.DBConstants;
import edu.kit.aifb.gwifi.mongo.MongoResource;
import edu.kit.aifb.gwifi.util.nlp.Language;

public class MongoLabelEntitySearcher {

	private DBCollection _dbCollection;

	private Language _sLang;
	private Language _tLang;

	public MongoLabelEntitySearcher(Language sLang, Language tLang) throws Exception {
		_sLang = sLang;
		_tLang = tLang;
		_dbCollection = MongoResource.INSTANCE.getDB()
				.getCollection(DBConstants.LABEL_ENTITY_COLLECTION + _sLang.getLabel() + "_" + _tLang.getLabel());
	}

	public List<DBSense> getSenses(String text, String source, boolean normalized) {
		BasicDBObject query = new BasicDBObject();
		if (normalized == true)
			query.put(DBConstants.LABEL_NORM_TEXT, text);
		else
			query.put(DBConstants.LABEL_TEXT, text);
		if (source != null)
			query.put(DBConstants.LABEL_SOURCE, source);
		DBCursor cur = _dbCollection.find(query);

		List<DBSense> dbss = new ArrayList<DBSense>();
		Map<Integer, DBSense> id2sense = new HashMap<Integer, DBSense>();
		while (cur.hasNext()) {
			DBObject obj = cur.next();
			if (obj != null) {
				Object idObj = obj.get(DBConstants.LABEL_ENTITY_TARGET_ENTITY_ID);
				if (idObj == null)
					continue;
				int id = (Integer) idObj;
				DBSense dbs;
				if(id2sense.containsKey(id)) {
					dbs = id2sense.get(id);
				} else {
					dbs = new DBSense();
					dbs.id = id;
					dbs.name = (String) obj.get(DBConstants.LABEL_ENTITY_TARGET_ENTITY_NAME);
					dbs.type = (String) obj.get(DBConstants.LABEL_ENTITY_ENTITY_TYPE);
					
					Object snObj = obj.get(DBConstants.LABEL_ENTITY_SOURCE_ENTITY_NAME);
					if(snObj != null)
						dbs.sName = (String) snObj;
					
					id2sense.put(id, dbs);
				}
				
				Object slinkDocCountObj = obj.get(DBConstants.LABEL_ENTITY_SENSE_LINK_DOC_COUNT);
				if (slinkDocCountObj != null) {
					long slinkDcCount = (Long) slinkDocCountObj;
					if (dbs.sLinkDocCount < slinkDcCount)
						dbs.sLinkDocCount = (Long) slinkDcCount;
				}
				Object slinkOccCountObj = obj.get(DBConstants.LABEL_ENTITY_SENSE_LINK_OCC_COUNT);
				if (slinkOccCountObj != null) {
					long slinkOccCount = (Long) slinkOccCountObj;
					if (dbs.sLinkOccCount < slinkOccCount)
						dbs.sLinkOccCount = (Long) slinkOccCount;
				}
				dbss.add(dbs);
			}
		}

		return dbss;
	}

	public Language getSourceLanguage() {
		return _sLang;
	}

	public Language getTargetLanguage() {
		return _tLang;
	}

	public class DBSense {

		private int id;
		private String name;

		private String type;

		private long sLinkDocCount = 0;
		private long sLinkOccCount = 0;

		private String sName;
		
		public int getId() {
			return id;
		}

		public String getName() {
			return name;
		}
		
		public String getSourceName() {
			return sName;
		}

		public String getType() {
			return type;
		}

		public long getLinkDocCount() {
			return sLinkDocCount;
		}

		public long getLinkOccCount() {
			return sLinkOccCount;
		}

		public String toString() {
			return name;
		}

	}

}
