package edu.kit.aifb.gwifi.mt.tools;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;

public class MongoDBTranslateTool implements TranslateTool {
	private Mongo mg;
	private DB db;
	private List <DBCollection> directories;
	private static final double THRESHOLD=0.1;
	
//	public MongoDBTranslateTool(String configFile){
//		Properties properties = new Properties();
//		try {
//			FileInputStream inputFile = new FileInputStream(configFile);
//			properties.load(inputFile);
//			this.mg=new Mongo(properties.getProperty("host"),Integer.valueOf(properties.getProperty("port")));
//			this.db=this.mg.getDB(properties.getProperty("db"));
//			String tables[] =properties.getProperty("table").split(";");
//			this.directories=new ArrayList<DBCollection>();
//			for(String table :tables){
//				this.directories.add(this.db.getCollection(table));
//			}
//			
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		
//	}
	public MongoDBTranslateTool(String configFile,String languagePair){
		Properties properties = new Properties();
		try {
			FileInputStream inputFile = new FileInputStream(configFile);
			properties.load(inputFile);
			this.mg=new Mongo(properties.getProperty("host"),Integer.valueOf(properties.getProperty("port")));
			this.db=this.mg.getDB(properties.getProperty("db"));
			String tables[] =properties.getProperty(languagePair).split(";");
			this.directories=new ArrayList<DBCollection>();
			for(String table :tables){
				this.directories.add(this.db.getCollection(table));
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	public Set<String> getBestTranslate(String source) {
		
		Set<String> result=new HashSet<String>();
		BasicDBObject condition = new BasicDBObject();
		
		condition.append("source", source);
		condition.append("p", new BasicDBObject("$gte",THRESHOLD));
		for(DBCollection directory : this.directories){
		for (DBObject object:directory.find(condition).toArray()){
			result.add((String) object.get("target"));
		}
		}
		return result;
	}

	@Override
	public double getTranslateProbability(String source, String target) {
		DBObject result=null;
		BasicDBObject condition =new BasicDBObject();
		condition.put("source", source);
		condition.put("target", target);
		Double resultValue=0.0;
		for(DBCollection directory : this.directories){
			if(directory.find(condition).sort(new BasicDBObject("p",-1)).hasNext()){
				result= directory.find(condition).sort(new BasicDBObject("p",-1)).next();
				if ((Double) result.get("p")>resultValue){
					resultValue=	(Double) result.get("p");
					}
					
			}
			
			
		}
			return resultValue;
		
		
	}

}
