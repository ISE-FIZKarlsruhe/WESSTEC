package edu.kit.aifb.gwifi.mongo;

import java.net.UnknownHostException;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

/**
 * A singleton of mongo connection.
 * 
 */
public enum MongoResource {
	INSTANCE;
	public MongoClient mongoClient;

	private MongoResource() {
		if (mongoClient == null) {
			mongoClient = getClient();
		}
	}

	private MongoClient getClient() {
		try {
			return new MongoClient(new MongoClientURI(Property.getValue("mongodb_url")));
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return null;
	}

	public DB getDB() {
		return mongoClient.getDB(Property.getValue("mongodb_name"));
	}

	public void finalizing() {
		mongoClient.close();
	}

	public static void main(String[] args) {
		DB db = MongoResource.INSTANCE.getDB();
		System.out.println(db.getName());
	}
}
