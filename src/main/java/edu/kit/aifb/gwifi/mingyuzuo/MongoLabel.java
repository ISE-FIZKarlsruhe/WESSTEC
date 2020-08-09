package edu.kit.aifb.gwifi.mingyuzuo;

import java.util.ArrayList;
import java.util.List;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import edu.kit.aifb.gwifi.annotation.DBConstants;
import edu.kit.aifb.gwifi.model.ISense;
import edu.kit.aifb.gwifi.model.Page.PageType;

public class MongoLabel {
	private String text;
	private DB mongoDB;

	private long linkDocCount = 0;
	private long linkOccCount = 0;
	private long textDocCount = 0;
	private long textOccCount = 0;

	private MongoSense[] senses = null;

	private boolean detailsSet;

	public MongoLabel(String text) {
		this.text = text;
		this.detailsSet = false;
		mongoDB = DBBuilder.getDB();
	}

	public String toString() {
		return "\"" + text + "\"";
	}

	public String getText() {
		return text;
	}

	public boolean exists() {
		if (!detailsSet)
			setDetails();
		return (senses.length > 0);
	}

	public long getLinkDocCount() {
		if (!detailsSet)
			setDetails();

		// TODO
		if (exists() && linkDocCount == 0)
			linkDocCount += 1;

		return linkDocCount;
	}

	public long getLinkOccCount() {
		if (!detailsSet)
			setDetails();

		// TODO
		if (exists() && linkOccCount == 0)
			linkOccCount += 1;

		return linkOccCount;
	}

	public long getDocCount() {
		if (!detailsSet)
			setDetails();

		// TODO
		if (exists() && textDocCount == 0)
			textDocCount += 1;

		return textDocCount;
	}

	public long getOccCount() {
		if (!detailsSet)
			setDetails();

		// TODO
		if (exists() && textOccCount == 0)
			textOccCount += 1;

		return textOccCount;
	}

	public double getLinkProbability() {
		if (!detailsSet)
			setDetails();

		// TODO
		if (exists() && linkDocCount == 0)
			linkDocCount += 1;
		if (exists() && textDocCount == 0)
			textDocCount += 1;

		double linkProb = (double) linkDocCount / textDocCount;

		if (linkProb > 1)
			linkProb = 1;

		return linkProb;
	}

	public MongoSense[] getSenses() {
		if (!detailsSet)
			setDetails();
		return senses;
	}

	public class MongoSense implements ISense {

		private long sLinkDocCount;
		private long sLinkOccCount;
		private int id;

		// constructor =============================================================

		public MongoSense(long sLinkDocCount, long sLinkOccCount, int id) {
			this.sLinkDocCount = sLinkDocCount;
			this.sLinkOccCount = sLinkOccCount;
			this.id = id;

			if (exists() && linkOccCount == 0)
				linkOccCount += 1;
			if (exists() && this.sLinkDocCount == 0)
				this.sLinkDocCount += 1;
			if (exists() && this.sLinkOccCount == 0)
				this.sLinkOccCount += 1;
		}

		public int getId() {
			return id;
		}

		public long getLinkDocCount() {
			return sLinkDocCount;
		}

		public long getLinkOccCount() {
			return sLinkOccCount;
		}

		public double getPriorProbability() {

			if (getSenses().length == 1)
				return 1;

			if (linkOccCount == 0)
				return 0;
			else
				return ((double) sLinkOccCount) / linkOccCount;
		}

		public boolean isPrimary() {
			return (this == senses[0]);
		}

		@Override
		public String getTitle() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public PageType getType() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public double getMatchSimilarity() {
			// TODO Auto-generated method stub
			return 0;
		}

	}

	private void setDetails() {
		try {
			BasicDBObject query = new BasicDBObject();
			query.put("label", text);
			DBObject lbl = mongoDB.getCollection(DBConstants.LABEL_COLLECTION_ZH).findOne(query);
			DBCursor cursor = mongoDB.getCollection(DBConstants.LABEL_ENTITY_COLLECTION_ZH).find(query);

			// System.out.println("lbl: "+lbl);
			// System.out.println("cursor: "+cursor);

			List<DBObject> senses = new ArrayList<>();

			// System.out.println("cursor.hasNext())" + cursor.hasNext());
			while (cursor.hasNext()) {
				DBObject next = cursor.next();
				// System.out.println("next: " + next);
				senses.add(next);
			}

			if (lbl == null) {
				throw new Exception();
			} else {
				setDetails(lbl, senses);
			}
		} catch (Exception e) {
			this.senses = new MongoSense[0];
			detailsSet = true;
		}
	}

	private void setDetails(DBObject lbl, List<DBObject> senses) {
		this.linkDocCount = Long.parseLong(lbl.get("linkDocCount").toString());
		this.linkOccCount = Long.parseLong(lbl.get("linkOccCount").toString());
		this.textDocCount = Long.parseLong(lbl.get("docCount").toString());
		this.textOccCount = Long.parseLong(lbl.get("occCount").toString());

		if (senses != null) {
			this.senses = new MongoSense[senses.size()];
			for (int i = 0; i < senses.size(); i++) {
				MongoSense sense = new MongoSense(Long.parseLong(senses.get(i).get("slinkDocCount").toString()),
						Long.parseLong(senses.get(i).get("slinkOccCount").toString()),
						Integer.parseInt(senses.get(i).get("id").toString()));
				this.senses[i] = sense;
			}
		}
		this.detailsSet = true;
	}

	public static MongoLabel createLabel(String text, DBObject lbl) {
		MongoLabel l = new MongoLabel(text);
		l.setDetails(lbl, null);
		return l;
	}

}
