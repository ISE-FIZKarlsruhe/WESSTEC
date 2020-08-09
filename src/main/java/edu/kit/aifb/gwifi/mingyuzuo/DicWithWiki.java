package edu.kit.aifb.gwifi.mingyuzuo;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class DicWithWiki
{
	private String _test;
	private String _tlang;
	private DBCollection _dic;
	private Set<String> setEnLabel;
	private List<String> listEntity;
	private Wiki wikiEnLable;
	
	public DicWithWiki(String test, String tlang)
	{
		this._test = test;
		this._tlang = tlang;
		setEnLabel = new LinkedHashSet<>();
		listEntity = new ArrayList<>();
		_dic = DBBuilder.getDB("KBP").getCollection(DBConstants.DIC_COLLECTION);
		
//		System.out.println("_dic: "+_dic);
		
		zhLabel2EnLable();
		changeLang(_tlang);
	}
	
	public Set<String> zhLabel2EnLable()
	{
		DBCursor cursor = _dic.find(new BasicDBObject(DBConstants.ZH_LABEL, _test));
		
		while(cursor.hasNext())
		{
			DBObject zh2en = cursor.next();
			String enlabel = zh2en.get(DBConstants.EN_LABEL).toString();
			
//			System.out.println("zh2en: "+zh2en);
//			System.out.println("enlabel: "+enlabel);
			
			if(null != enlabel)
			{
				setEnLabel.add(enlabel);
			}else 
				continue;
			
		}
//		System.out.println("setEnLabel: "+setEnLabel);
		return setEnLabel;
	}
	
	public List<String> changeLang(String tlang)
	{
		if(setEnLabel.isEmpty() == false)
		{
			for(String enLabel : setEnLabel)
			{
//				System.out.println("enLabel: "+enLabel);
				wikiEnLable = new Wiki(enLabel, LanguageContains.EN, tlang);
				wikiEnLable.exists();
			}
		}
		listEntity = wikiEnLable.getTargetEntities();
		return listEntity;
	}
	
	public List<String> getListEntity()
	{
		return listEntity;
	}
	
	public static void main(String[] args)
	{
		DicWithWiki test = new DicWithWiki("阿布扎比市", LanguageContains.DE);
		
		System.out.println("test:");

		for(int i = 0; i < test.getListEntity().size(); i++)
		{
			System.out.println(test.getListEntity().get(i));
		}
	}

	
	//zhlabel - enlabel(list)(if null continue) - wiki(test, en, ?)
}
