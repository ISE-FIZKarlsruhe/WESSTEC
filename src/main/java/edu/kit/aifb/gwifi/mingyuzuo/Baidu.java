package edu.kit.aifb.gwifi.mingyuzuo;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class Baidu
{
	private String _text;
	private String entity;
	private DBCollection _baidu;
	
	public Baidu(String text)
	{
		this._text = text;
		_baidu = DBBuilder.getDB("KBP").getCollection(DBConstants.BAIDU_COLLECTION);
		label2Entity(_text);
	}
	
	public String label2Entity(String text)
	{
		DBObject searchEntity = _baidu.findOne(new BasicDBObject(DBConstants.LABEL, text));
		entity = searchEntity.get(DBConstants.ENTITY).toString();
		return entity;
	}

	public String getEntity()
	{
		return entity;
	}
	
	public static void main(String[] args)
	{
		Baidu test = new Baidu("2010年夏季奧林匹克青年運動會女子籃球比賽");
		//result: Basketball at the 2010 Summer Youth Olympics – Girls' tournament
		System.out.println(test.getEntity());
	}
	
}
