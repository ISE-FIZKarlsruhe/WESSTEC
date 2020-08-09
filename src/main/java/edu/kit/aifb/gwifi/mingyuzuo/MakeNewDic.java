package edu.kit.aifb.gwifi.mingyuzuo;

import java.net.UnknownHostException;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;

//using zhlabel - enlabel and enlabel - enentity make zhlabel - enentity
// zhlabel - zhentity   需要langlinks 找到enentity

public class MakeNewDic
{
	private Mongo mongomaia;
	private Mongo mongoremus;
	private DB dbKBP;
	private DB dbABIRS;
	private DBCollection zhlabel_enlabel;
	private DBCollection labelEntityIndex_EN_LC;
	private DBCollection labelEntityIndex_ZH_EN;

	public MakeNewDic()
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

		zhlabel_enlabel = dbKBP.getCollection("DictionaryFrom_cedict_ts");
		labelEntityIndex_EN_LC = dbABIRS.getCollection("LabelEntityIndex_EN_LC");
		labelEntityIndex_ZH_EN = dbKBP.getCollection("LabelEntityIndex_ZH_EN");
		labelEntityIndex_ZH_EN.drop();
	}

	private void makeNewDic()
	{
		DBCursor cur = zhlabel_enlabel.find();
		cur.addOption(com.mongodb.Bytes.QUERYOPTION_NOTIMEOUT);

		while (cur.hasNext())
		{
			DBObject curobj = cur.next();
			String zhLabel = curobj.get("zhlabel").toString();
//			String enLabel = curobj.get("enlabel").toString();
			String lcenLabel = curobj.get("lcenlabel").toString();

			String enEntity = getEnEntityByEnLabel(lcenLabel);
			
			if(lcenLabel.equals(""))
				continue;
			
			if(enEntity.equals(""))
				continue;
			
			this.saveLabelEntityIndex_ZH_EN2Mongodb(zhLabel, enEntity);
		}

	}

	private String getEnEntityByEnLabel(String lcenLabel)
	{
		BasicDBObject query = new BasicDBObject();
		query.put("lcLabel", lcenLabel);
		DBObject entity = null;
		if (null != labelEntityIndex_EN_LC.findOne(query))
		{
			entity = labelEntityIndex_EN_LC.findOne(query);
		}
		else
		{
			return "";
		}
		String enEntity = entity.get("entity").toString();

		return enEntity;
	}

	private void saveLabelEntityIndex_ZH_EN2Mongodb(String zhLabel, String enEntity)
	{
		DBObject insertData = new BasicDBObject();
		insertData.put("label", zhLabel);
		insertData.put("entity", enEntity);
		labelEntityIndex_ZH_EN.insert(insertData);
	}

	
	public static void main(String[] args)
	{
		MakeNewDic newDic = new MakeNewDic();
		newDic.makeNewDic();
		System.out.println("done!!!!");
	}
}
