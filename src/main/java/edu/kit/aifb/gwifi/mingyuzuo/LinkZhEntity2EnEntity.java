package edu.kit.aifb.gwifi.mingyuzuo;

import java.net.UnknownHostException;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;

public class LinkZhEntity2EnEntity
{
	private Mongo mongomaia;
	private Mongo mongoremus;
	private DB dbKBP;
	private DB dbABIRS;
	private DBCollection labelEntityIndex_ZH;
//	private DBCollection labelEntityIndex_ZH_LC;
	private DBCollection langlinksIndex_DE_ZH_EN;
	private DBCollection labelEntityIndex_ZH_EN2;


	public LinkZhEntity2EnEntity()
	{
		mongoInit();
	}

	@SuppressWarnings("deprecation")
	private void mongoInit()
	{
		try
		{
			mongomaia = new Mongo("aifb-ls3-maia.aifb.kit.edu", 19005);
			mongoremus = new Mongo("aifb-ls3-remus.aifb.kit.edu", 19005);
		}
		catch (UnknownHostException e)
		{
			e.printStackTrace();
		}
		dbKBP = mongomaia.getDB("KBP");
		dbABIRS = mongoremus.getDB("ABIRS");

		labelEntityIndex_ZH = dbABIRS.getCollection("LabelEntityIndex_ZH");
		langlinksIndex_DE_ZH_EN = dbABIRS.getCollection("LanglinksIndex_DE_ZH_EN");
		labelEntityIndex_ZH_EN2 = dbKBP.getCollection("LabelEntityIndex_ZH_EN2");
		
		labelEntityIndex_ZH_EN2.drop();
	}

	private void linkZhEntity2EnEntity()
	{
		DBCursor cur = labelEntityIndex_ZH.find();
		cur.addOption(com.mongodb.Bytes.QUERYOPTION_NOTIMEOUT);

		while (cur.hasNext())
		{
			DBObject curobj = cur.next();
			String label = curobj.get("label").toString();
			String zhEntity = curobj.get("entity").toString();
			
			String lclabel = label.toLowerCase();

			String enEntity = getEnEntityByZhEntity(zhEntity);

			if (zhEntity.equals(""))
				continue;

			if (enEntity.equals(""))
				continue;

			this.saveLabelEntityIndex_ZH_EN2ToMongodb(label, lclabel, enEntity);
		}
	}

	private String getEnEntityByZhEntity(String zhEntity)
	{
		BasicDBObject query = new BasicDBObject();
		query.put("s_title", zhEntity);
		DBObject title = null;
		if (null != langlinksIndex_DE_ZH_EN.findOne(query))
		{
			title = langlinksIndex_DE_ZH_EN.findOne(query);
		}
		else
		{
			return "";
		}
		String enEntity = title.get("t_title").toString();

		return enEntity;
	}
	
	private void saveLabelEntityIndex_ZH_EN2ToMongodb(String label, String lclabel, String enEntity)
	{
		DBObject insertData = new BasicDBObject();
		insertData.put("label", label);
		insertData.put("lclabel", lclabel);
		insertData.put("entity", enEntity);
		labelEntityIndex_ZH_EN2.insert(insertData);		
		
	}
	
	public static void main(String[] args)
	{
		LinkZhEntity2EnEntity link = new LinkZhEntity2EnEntity();
		link.linkZhEntity2EnEntity();
		System.out.println("done!!");
	}

}
