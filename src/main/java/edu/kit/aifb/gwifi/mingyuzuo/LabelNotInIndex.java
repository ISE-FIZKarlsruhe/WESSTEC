package edu.kit.aifb.gwifi.mingyuzuo;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import edu.kit.aifb.gwifi.mongo.DBConstants;
import edu.kit.aifb.gwifi.mongo.MongoResource;
import edu.kit.aifb.gwifi.mongo.Property;
import edu.kit.aifb.gwifi.nlp.preprocessing.StanfordNLPPreprocessor;
//import edu.kit.aifb.gwifi.service.Service.EntityType;
import edu.kit.aifb.gwifi.util.Position;
import edu.kit.aifb.gwifi.util.nlp.LabelNormalizer;
import edu.kit.aifb.gwifi.util.nlp.Language;

public class LabelNotInIndex
{
	private DBCollection _sourceCollection;
	private DBCollection _zhdbCollection;
	private DBCollection _endbCollection;
	private DBCollection _googleForAllTextCollection_en;
	private DBCollection _googleForAllTextCollection_zh;
	private LabelNormalizer _zhnormalizer;
	private LabelNormalizer _ennormalizer;
	private Set<String> _zhLabelSet;
	private Set<String> _enLabelSet;

	public LabelNotInIndex(String year)throws Exception{
		_sourceCollection = MongoResource.INSTANCE.getDB().getCollection("eval_source_2013");
		_zhdbCollection = MongoResource.INSTANCE.getDB().getCollection(DBConstants.LABEL_COLLECTION + "zh");
		_endbCollection = MongoResource.INSTANCE.getDB().getCollection(DBConstants.LABEL_COLLECTION + "en");
		_googleForAllTextCollection_en = MongoResource.INSTANCE.getDB().getCollection("googleForAllText_en" + "_" + year);
		_googleForAllTextCollection_zh = MongoResource.INSTANCE.getDB().getCollection("googleForAllText_zh" + "_" + year);
		_zhnormalizer = new LabelNormalizer("zh");
		_ennormalizer = new LabelNormalizer("en");
		_zhLabelSet = new LinkedHashSet<>();
		_enLabelSet = new LinkedHashSet<>();
	}

	private void extractLabel()
	{
		DBCursor cur = _sourceCollection.find();
		cur.addOption(com.mongodb.Bytes.QUERYOPTION_NOTIMEOUT);
		StanfordNLPPreprocessor zhpre = new StanfordNLPPreprocessor("configs/NLPConfig.properties", Language.ZH);
		StanfordNLPPreprocessor enpre = new StanfordNLPPreprocessor("configs/NLPConfig.properties", Language.EN);
		int i = 1;
		while (cur.hasNext())
		{
//			if(i == 5){
//				 break;
//				 }

			DBObject curobj = cur.next();
			String doc_id = curobj.get("doc_id").toString();
//			System.out.println(doc_id);
			String doc_text = curobj.get("doc_text").toString();
			
			System.out.println(i++);
			
			if(doc_text == null || doc_text.length() == 0)
				continue;
			
			if (doc_text.startsWith("exit")) 
			{
				continue;
			}
			
			if (!isChinese(doc_id))
			{
				_enLabelSet.addAll(extractLabelForOneText(doc_text, enpre));
			}else
			{
				_zhLabelSet.addAll(extractLabelForOneText(doc_text, zhpre));
			}
		}
		
	}
	
	private Set<String> extractLabelForOneText(String doc_text, StanfordNLPPreprocessor pre)
	{
		Set<String> labelSet = new LinkedHashSet<>();
		doc_text = pre.segmentation(doc_text);
//		HashMap<Position, EntityType> labelmap = pre.NERPositionAndType(doc_text);
//		for(Position pos : labelmap.keySet())
//		{
//			if(labelmap.get(pos).equals(EntityType.MISC))
//				continue;			
//			labelSet.add(doc_text.substring(pos.getStart(), pos.getEnd()));
//			System.out.println(doc_text.substring(pos.getStart(), pos.getEnd()));
//		}
		return labelSet;
	}
	
	
	public static boolean isChinese(String docid)
	{
		if (docid.contains("cmn"))
		{
			return true;
		}
		else if (docid.contains("CMN"))
		{
			return true;
		}
		else
			return false;
	}
	
	public void insertLabel() throws Exception {
		for(String labelText: _zhLabelSet)
		{
			String nLabelText = _zhnormalizer.normalize(labelText);
			BasicDBObject query = new BasicDBObject();
			query.put(DBConstants.LABEL_NORM_TEXT, nLabelText);
			DBObject curobjLabel = _zhdbCollection.findOne(query);
			if(curobjLabel == null) {
				BasicDBObject dbObject = new BasicDBObject(DBConstants.LABEL_TEXT, labelText)
						.append(DBConstants.LABEL_NORM_TEXT, nLabelText)
						.append(DBConstants.LABEL_SOURCE, DBConstants.SOURCE_GOOGLE_SEARCH);
				_googleForAllTextCollection_zh.insert(dbObject);
			}
		}	
		
		for(String labelText: _enLabelSet)
		{
			String nLabelText = _ennormalizer.normalize(labelText);
			BasicDBObject query = new BasicDBObject();
			query.put(DBConstants.LABEL_NORM_TEXT, nLabelText);
			DBObject curobjLabel = _endbCollection.findOne(query);
			if(curobjLabel == null) {
				BasicDBObject dbObject = new BasicDBObject(DBConstants.LABEL_TEXT, labelText)
						.append(DBConstants.LABEL_NORM_TEXT, nLabelText)
						.append(DBConstants.LABEL_SOURCE, DBConstants.SOURCE_GOOGLE_SEARCH);
				_googleForAllTextCollection_en.insert(dbObject);
			}
		}
		
		
	}
		
	public static void main(String[] args)
	{
		try {
			String configPath = args[0];
			Property.setProperties(configPath);
			LabelNotInIndex indexer = new LabelNotInIndex(args[1]);
			indexer.extractLabel();
			indexer.insertLabel();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
