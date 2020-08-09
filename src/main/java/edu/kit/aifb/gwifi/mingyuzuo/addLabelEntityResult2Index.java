package edu.kit.aifb.gwifi.mingyuzuo;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import edu.kit.aifb.gwifi.mongo.MongoResource;
import edu.kit.aifb.gwifi.mongo.Property;
import edu.kit.aifb.gwifi.util.nlp.Language;

public class addLabelEntityResult2Index
{
	private DBCollection _googleForTestResultCollection;
	private DBCollection _LabelEntityIndexCollection;
	private DBCollection _googleForTestCollection;
	
	private Language _sLang;
	private Language _tLang;
	
	public addLabelEntityResult2Index(String sLangLabel, String tLangLabel, String year)
	{
		_sLang = Language.getLanguage(sLangLabel);
		_tLang = Language.getLanguage(tLangLabel);
		_googleForTestResultCollection = MongoResource.INSTANCE.getDB()
				.getCollection("googleForAllText_result_" + _sLang.getLabel() + "_" + _tLang.getLabel() + "_" + year);
		_LabelEntityIndexCollection = MongoResource.INSTANCE.getDB()
				.getCollection("LabelEntityIndex_" + _sLang.getLabel() + "_" + _tLang.getLabel());
		_googleForTestCollection = MongoResource.INSTANCE.getDB().getCollection("googleForAllText_" + _sLang.getLabel() + "_" + year);
	}
	
	//	"configs/MongoConfig_gwifi.properties"	"zh" "en" "2013"
	public static void main(String[] args)
	{
		try
		{
			String configPath = args[0];
			Property.setProperties(configPath);
			addLabelEntityResult2Index indexer = new addLabelEntityResult2Index(args[1], args[2], args[3]);
			indexer.insertLabelEntity();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}


	private void insertLabelEntity()
	{
		DBCursor curqueries = _googleForTestResultCollection.find();
		
		while (curqueries.hasNext())
		{
			DBObject curobj = curqueries.next();
			String nlabelText = curobj.get("label").toString();
			String labelText = getLabelByNLabel(nlabelText) ;
			String sid = curobj.get("s_id").toString();
			int s_id = Integer.valueOf(sid);
			String s_name = curobj.get("s_name").toString();
			String type = curobj.get("type").toString();
			String source = curobj.get("source").toString();
			
			
			String tid = null;
			String t_name = null;
			int t_id = 0;
			if(curobj.containsField("t_id"))
			{	
				t_name = curobj.get("t_name").toString();
				tid = curobj.get("t_id").toString();
				t_id = Integer.valueOf(tid);
			}	
			
			
			
			BasicDBObject dbobj = new BasicDBObject("label", labelText)
											.append("nLabel", nlabelText)
											.append("s_id", s_id)
											.append("s_name", s_name)
											.append("type", type)
											.append("source", source);
			if(curobj.containsField("t_id"))
			{
				dbobj.append("t_id", t_id)
					 .append("t_name", t_name);
			}
			_LabelEntityIndexCollection.insert(dbobj);
			
		}
	}

	private String getLabelByNLabel(String nlabelText)
	{
		BasicDBObject query = new BasicDBObject();
		query.put("nLabel", nlabelText);
		String labelText = _googleForTestCollection.findOne(query).get("label").toString();
		return labelText;
	}
}
