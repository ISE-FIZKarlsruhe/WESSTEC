package edu.kit.aifb.gwifi.mingyuzuo;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import edu.kit.aifb.gwifi.db.struct.DbLabel;
import edu.kit.aifb.gwifi.db.struct.DbSenseForLabel;
import edu.kit.aifb.gwifi.util.text.TextProcessor;


public class LabelInMongoDB
{
	private String text ;
	private TextProcessor textProcessor ;

	private long linkDocCount = 0 ;
	private long linkOccCount = 0 ;
	private long textDocCount = 0;
	private long textOccCount = 0;
	
	private Sense[] senses = null ;
	
	private boolean detailsSet ;
	
	public LabelInMongoDB(String text) {
		this.text = text ;
		this.textProcessor = null;
		this.detailsSet = false ;
	}
	
	public LabelInMongoDB(String text, TextProcessor tp) {
		this.text = text ;
		this.textProcessor = tp ;
		this.detailsSet = false ;
	}
	
	public String toString() {
		return "\"" + text + "\"" ; 
	}
	
	public String getText() {
		return text;
	}
	
	public boolean exists() {
		
		
		if (!detailsSet) setDetails() ;
		return (senses.length > 0) ;	
	}
	
	public long getLinkDocCount() {
		if (!detailsSet) setDetails() ;
		
		// TODO
		if(exists() && linkDocCount == 0)
			linkDocCount += 1;
		
		return linkDocCount;
	}
	
	public long getLinkOccCount() {
		if (!detailsSet) setDetails() ;
		
		// TODO
		if(exists() && linkOccCount == 0)
			linkOccCount += 1;
		
		return linkOccCount;
	}
	
	public long getDocCount() {
		if (!detailsSet) setDetails() ;
		
		// TODO
		if(exists() && textDocCount == 0)
			textDocCount += 1;
		
		return textDocCount;
	}
	
	public long getOccCount() {
		if (!detailsSet) setDetails() ;
		
		// TODO
		if(exists() && textOccCount == 0)
			textOccCount += 1;
		
		return textOccCount;
	}
	
	public double getLinkProbability() {
		if (!detailsSet) setDetails() ;
		
		// TODO
		if(exists() && linkDocCount == 0)
			linkDocCount += 1;
		if(exists() && textOccCount == 0)
			textOccCount += 1;
		
		if (textDocCount == 0)
			return 0 ;
		
		double linkProb = (double) linkDocCount/textDocCount ;
		
		if (linkProb > 1)
			linkProb = 1 ;
			
		return linkProb ;
	}
	
	public Sense[] getSenses() {
		if (!detailsSet) setDetails() ;	
		return senses ;
	}
	
public class Sense {

		
		private long sLinkDocCount ;
		private long sLinkOccCount ;


		//constructor =============================================================
		
		protected Sense(DbSenseForLabel s) {
			

			this.sLinkDocCount = s.getLinkDocCount() ;
			this.sLinkOccCount = s.getLinkOccCount() ;
			
			// TODO
			if(exists() && linkOccCount == 0)
				linkOccCount += 1;
			if(exists() && sLinkDocCount == 0)
				sLinkDocCount += 1;
			if(exists() && sLinkOccCount == 0)
				sLinkOccCount += 1;
		}
		
		public long getLinkDocCount() {
			return sLinkDocCount;
		}
		
		public long getLinkOccCount() {
			return sLinkOccCount;
		}
		
		
		public double getPriorProbability() {

			if (getSenses().length == 1)
				return 1 ;

			if (linkOccCount == 0)
				return 0 ;
			else 			
				return ((double)sLinkOccCount) / linkOccCount ;
		}
		
		public boolean isPrimary() {
			return (this == senses[0]) ;
		}
		
	}

	private void setDetails() {
	
		try {
			
			BasicDBObject query = new BasicDBObject();
			query.put("label", text);
			// TODO: do not change WEnvironment class, use mongoResource or other class to return mongoDB collection
//			DBObject lbl = env.getMongoDB().getCollection(DBConstants.LABEL_ENTITY_COLLECTION_EN_LC).findOne(query);
			DBObject lbl = null;
	
			if (lbl == null) {
				throw new Exception() ;
			} else {
				setDetails(lbl) ;
			}
		} catch (Exception e) {
			this.senses = new Sense[0] ;
			detailsSet = true ;
		}
	}

	private void setDetails(DbLabel lbl) {
	
		this.linkDocCount = lbl.getLinkDocCount() ;
		this.linkOccCount = lbl.getLinkOccCount() ;
		this.textDocCount = lbl.getTextDocCount() ;
		this.textOccCount = lbl.getTextOccCount() ;
	
		this.senses = new Sense[lbl.getSenses().size()] ;
	
		int i = 0 ;
		for (DbSenseForLabel dbs:lbl.getSenses()) {
		this.senses[i] = new Sense(dbs) ;
			i++ ;
		}
	
		this.detailsSet = true ;
	}	

	private void setDetails(DBObject lbl){
		
		this.linkDocCount = Long.parseLong(lbl.get("linkDocCount").toString());		
		this.linkOccCount = Long.parseLong(lbl.get("linkOccCount").toString());
		this.textDocCount = Long.parseLong(lbl.get("docCount").toString());
		this.textOccCount = Long.parseLong(lbl.get("occCount").toString());

		
		this.senses = new Sense[1] ;
//		this.senses[0] = new Sense(env) ;

		this.detailsSet = true ;
		
		System.out.println("RESULT : "+this.linkDocCount + this.linkOccCount + this.textDocCount + this.textOccCount);
	}

	public static LabelInMongoDB createLabel(String text, DbLabel dbLabel, TextProcessor tp) {
		LabelInMongoDB l = new LabelInMongoDB(text, tp) ;
		l.setDetails(dbLabel) ;
	
		return l ;
	}
	
	

}
