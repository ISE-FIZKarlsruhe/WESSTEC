package edu.kit.aifb.gwifi.mingyuzuo;

import java.util.ArrayList;
import java.util.List;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;


public class Wiki
{
	private String slang;
	private String tlang;
	private String _text;
	private List<String> sourceEntities;
	private static List<String> targetEntities;
	
	public List<String> getTargetEntities()
	{
		return targetEntities;
	}


	private DBCollection _entityCollection;
	private DBCollection _labelCollection;
	private DBCollection _labelEntityCollection;
	private DBCollection _langlinksIndexCollection;

	public Wiki(String text, String slang, String tlang)
	{
		this.slang = slang;
		this.tlang = tlang;
		this._text = text;
		
		targetEntities = new ArrayList<>();
		
		if(LanguageContains.EN.equals(slang))
		{
			_entityCollection = DBBuilder.getDB().getCollection(DBConstants.ENTITY_COLLECTION_EN);
			_labelCollection = DBBuilder.getDB().getCollection(DBConstants.LABEL_COLLECTION_EN);
			_labelEntityCollection = DBBuilder.getDB().getCollection(DBConstants.LABEL_ENTITY_COLLECTION_EN);
			_langlinksIndexCollection = DBBuilder.getDB().getCollection(DBConstants.LANGLINKS_COLLECTION);
		}else if(LanguageContains.DE.equals(slang))
		{
			_entityCollection = DBBuilder.getDB().getCollection(DBConstants.ENTITY_COLLECTION_DE);
			_labelCollection = DBBuilder.getDB().getCollection(DBConstants.LABEL_COLLECTION_DE);
			_labelEntityCollection = DBBuilder.getDB().getCollection(DBConstants.LABEL_ENTITY_COLLECTION_DE);
			_langlinksIndexCollection = DBBuilder.getDB().getCollection(DBConstants.LANGLINKS_COLLECTION);
		}else if(LanguageContains.ZH.equals(slang))
		{
			_entityCollection = DBBuilder.getDB().getCollection(DBConstants.ENTITY_COLLECTION_ZH);
			_labelCollection = DBBuilder.getDB().getCollection(DBConstants.LABEL_COLLECTION_ZH);
			_labelEntityCollection = DBBuilder.getDB().getCollection(DBConstants.LABEL_ENTITY_COLLECTION_ZH);
			_langlinksIndexCollection = DBBuilder.getDB().getCollection(DBConstants.LANGLINKS_COLLECTION);
		}
		
		exists();
	}
	
	
	public boolean exists()
	{
		BasicDBObject dbObject = new BasicDBObject(DBConstants.LABEL, _text);
		DBObject label = _labelCollection.findOne(dbObject);
		
		if (null != label)
		{
			sourceEntities = label2Entity(_text);

			for(int i = 0; i < sourceEntities.size(); i++)
			{
				String sourceEntity = sourceEntities.get(i);
				String targetEntity = entity2TargetLang(sourceEntity);
				
				if(targetEntity.contains("not found"))
					continue;
				
				targetEntities.add(targetEntity);
			}
			return true;
		}else
		{
			return false;
		}
	}


	public List<String> label2Entity(String label)
	{
		List<String> sourceEntities = new ArrayList<>();
		DBCursor cursor = _labelEntityCollection.find(new BasicDBObject(DBConstants.LABEL, label));
		
		while(cursor.hasNext())
		{
			DBObject searchEntity = cursor.next();
			sourceEntities.add(searchEntity.get("entity").toString());
		}
		return sourceEntities;
	}
	
	
	public String entity2TargetLang(String sourceEntity)
	{
		DBObject searchId = _entityCollection.findOne(new BasicDBObject(DBConstants.ENTITY, sourceEntity));
		String id = searchId.get("id").toString();
		
		BasicDBList condList = new BasicDBList();
		BasicDBObject querySourceId = new BasicDBObject();
		BasicDBObject queryTargetLang = new BasicDBObject();
		BasicDBObject query = new BasicDBObject();
		querySourceId.append("s_id", id);
		queryTargetLang.append("t_lang", tlang);
		condList.add(querySourceId);
		condList.add(queryTargetLang);
		query.put("$and", condList);
		
		DBObject searchTargetLang = _langlinksIndexCollection.findOne(query);
		
		if(null == searchTargetLang)
		{
			return "not found";
		}

		return searchTargetLang.get("t_title").toString();
	}
	
	
	//test
	public static void main(String[] args)
	{
		Wiki test = new Wiki("中国", LanguageContains.ZH, LanguageContains.EN);
		if(test.exists())
		{
			for(int i = 0; i < test.getTargetEntities().size(); i++)
			{
				System.out.println(test.getTargetEntities().get(i));
			}
		}
	}
}
