package edu.kit.aifb.gwifi.mingyuzuo;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import edu.kit.aifb.gwifi.mongo.MongoResource;
import edu.kit.aifb.gwifi.mongo.Property;
import edu.kit.aifb.gwifi.util.nlp.Language;

public class addLabelResult2Index
{
	private DBCollection _googleForTestCollection;
	private DBCollection _LabelIndexCollection;
	
	private Language _sLang;
	
	public addLabelResult2Index(String sLangLabel, String year)
	{
		_sLang = Language.getLanguage(sLangLabel);
		_googleForTestCollection = MongoResource.INSTANCE.getDB().getCollection("googleForTest_allQuery_" + _sLang.getLabel() + "_" + year);
		_LabelIndexCollection = MongoResource.INSTANCE.getDB().getCollection("LabelIndex_" + _sLang.getLabel());
	}
	
	//	"configs/MongoConfig_gwifi.properties" "zh" "2013"
	public static void main(String[] args)
	{
		try
		{
			String configPath = args[0];
			Property.setProperties(configPath);
			addLabelResult2Index indexer = new addLabelResult2Index(args[1],args[2]);
			indexer.insertLabel();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}


	private void insertLabel()
	{
		DBCursor curqueries = _googleForTestCollection.find();
		
		while (curqueries.hasNext())
		{
			DBObject curobj = curqueries.next();
			String labelText = curobj.get("label").toString();
			String nlabelText = curobj.get("nLabel").toString();
			String source = curobj.get("source").toString();
			
			BasicDBObject dbobj = new BasicDBObject("label", labelText)
											.append("nLabel", nlabelText)
											.append("source", source);
			_LabelIndexCollection.insert(dbobj);
			
		}
	}
}
