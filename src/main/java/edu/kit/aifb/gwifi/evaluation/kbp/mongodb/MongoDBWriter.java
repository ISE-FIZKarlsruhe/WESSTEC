package edu.kit.aifb.gwifi.evaluation.kbp.mongodb;

import java.net.UnknownHostException;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;


public class MongoDBWriter {
	private MongoClient mg;
	private DB db;
	private DBCollection coll;

	public MongoDBWriter(String url, String dbName, String collName) {
		try {
			this.mg = new MongoClient(new MongoClientURI(url));
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		this.db = this.mg.getDB(dbName);
		this.coll = db.getCollection(collName);
	}

	public void write(BasicDBObject data) {
		this.coll.insert(data);
	}
	
	public void index(String field) {
		this.coll.createIndex(new BasicDBObject(field, 1));
	}
	
	public void close() {
		mg.close();
	}
	
}
