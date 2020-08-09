package edu.kit.aifb.gwifi.mongo.search;

import java.util.ArrayList;
import java.util.List;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import edu.kit.aifb.gwifi.mongo.DBConstants;
import edu.kit.aifb.gwifi.mongo.MongoResource;
import edu.kit.aifb.gwifi.util.nlp.Language;

public class MongoLabelSearcher {

	private DBCollection _dbCollection;

	private Language _lang;

	public MongoLabelSearcher(Language lang) throws Exception {
		_lang = lang;
		_dbCollection = MongoResource.INSTANCE.getDB().getCollection(DBConstants.LABEL_COLLECTION + _lang.getLabel());
	}

	public List<DBLabel> getLabel(String text, String source, boolean normalized) {
		BasicDBObject query = new BasicDBObject();
		if (normalized == true)
			query.put(DBConstants.LABEL_NORM_TEXT, text);
		else
			query.put(DBConstants.LABEL_TEXT, text);
		if (source != null)
			query.put(DBConstants.LABEL_SOURCE, source);
		DBCursor cur = _dbCollection.find(query);

		List<DBLabel> dbls = new ArrayList<DBLabel>();
		while (cur.hasNext()) {
			DBObject obj = cur.next();
			if (obj != null) {
				DBLabel dbl = new DBLabel();
				dbl.exist = true;
				dbl.text = text;
				Object docCountObj = obj.get(DBConstants.LABEL_DOC_COUNT);
				if (docCountObj != null) {
					dbl.textDocCount = (Long) docCountObj;
				}
				Object occCountObj = obj.get(DBConstants.LABEL_OCC_COUNT);
				if (occCountObj != null) {
					dbl.textOccCount = (Long) occCountObj;
				}
				Object linkDocCountObj = obj.get(DBConstants.LABEL_LINK_DOC_COUNT);
				if (linkDocCountObj != null) {
					dbl.linkDocCount = (Long) linkDocCountObj;
				}
				Object linkOccCountObj = obj.get(DBConstants.LABEL_LINK_OCC_COUNT);
				if (linkOccCountObj != null) {
					dbl.linkOccCount = (Long) linkOccCountObj;
				}
				dbls.add(dbl);
			}
		}

		return dbls;
	}

	public DBLabel getOneLabel(String text, String source, boolean normalized) {
		BasicDBObject query = new BasicDBObject();
		if (normalized == true)
			query.put(DBConstants.LABEL_NORM_TEXT, text);
		else
			query.put(DBConstants.LABEL_TEXT, text);
		if (source != null)
			query.put(DBConstants.LABEL_SOURCE, source);
		DBCursor cur = _dbCollection.find(query);

		DBLabel dbl = new DBLabel();
		while (cur.hasNext()) {
			DBObject obj = cur.next();
			if (obj != null) {
				dbl.exist = true;
				dbl.text = text;
				Object docCountObj = obj.get(DBConstants.LABEL_DOC_COUNT);
				if (docCountObj != null) {
					long docCount = (Long) docCountObj;
					if (dbl.textDocCount < docCount)
						dbl.textDocCount = docCount;
				}
				Object occCountObj = obj.get(DBConstants.LABEL_OCC_COUNT);
				if (occCountObj != null) {
					long occCount = (Long) occCountObj;
					if (dbl.textOccCount < occCount)
						dbl.textOccCount = occCount;
				}
				Object linkDocCountObj = obj.get(DBConstants.LABEL_LINK_DOC_COUNT);
				if (linkDocCountObj != null) {
					long linkDocCount = (Long) linkDocCountObj;
					if (dbl.linkDocCount < linkDocCount)
						dbl.linkDocCount = linkDocCount;
				}
				Object linkOccCountObj = obj.get(DBConstants.LABEL_LINK_OCC_COUNT);
				if (linkOccCountObj != null) {
					long linkOccCount = (Long) linkOccCountObj;
					if (dbl.linkOccCount < linkOccCount)
						dbl.linkOccCount = linkOccCount;
				}
			}
		}

		return dbl;
	}

	public class DBLabel {

		private boolean exist = false;
		private String text;
		private long linkDocCount = 0;
		private long linkOccCount = 0;
		private long textDocCount = 0;
		private long textOccCount = 0;

		public boolean exists() {
			return exist;
		}

		public long getLinkDocCount() {
			return linkDocCount;
		}

		public long getLinkOccCount() {
			return linkOccCount;
		}

		public long getTextDocCount() {
			return textDocCount;
		}

		public long getTextOccCount() {
			return textOccCount;
		}

		public String toString() {
			return text;
		}

	}

}
