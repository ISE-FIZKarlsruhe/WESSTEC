package edu.kit.aifb.gwifi.xlime;

import java.util.Map;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

public class xLiMeNewsTest {

	public final static String ITEMS_TAG = "items";
	public final static String ITEM_TAG = "item";
	
	public final static String DB_ARTICLE = "article";
	public final static String DB_ID = "id";
	public final static String DB_TITLE = "title";
	public final static String DB_TEXT = "body-cleartext";
	public final static String DB_LANG = "lang";
	public final static String DB_URI = "uri";
	public final static String DB_DATE = "serialized-date";
	public final static String DB_HOSTNAME = "hostname";
	public final static String DB_LOCATION = "location";
	public final static String DB_LATITUDE = "latitude";
	public final static String DB_LONGITUDE = "longitude";
	public final static String DB_COUNTRY = "country";

	public static void main(String[] args) throws Exception {
		String language = args[0];
		int startPos = Integer.parseInt(args[1]);
		int endPos = Integer.parseInt(args[2]);
		int interval = Integer.parseInt(args[3]);
		xLiMeNewsTest mongoService = new xLiMeNewsTest();
		MongoClient mongoClient = mongoService.initMongoDB();
		DBCollection collection = mongoService.getCollection(mongoClient);

		int num = endPos - startPos + 1;
		int round = num / interval + 1;

		int start, end = startPos - 1;
		for (int i = 1; i <= round; i++) {
			start = end + 1;
			end = start + interval - 1;

			DBCursor cursor = mongoService.genDBCursor(collection, language, startPos, endPos);
			try {
				while (cursor.hasNext()) {
					DBObject obj = cursor.next();
					Map<String, Object> article = (Map<String, Object>) obj.get(DB_ARTICLE);
					String id = article.get(DB_ID).toString();
					System.out.println("id: " + id);
					String title = article.get(DB_TITLE).toString();
					System.out.println("title: " + title);
					String text = article.get(DB_TEXT).toString();
					System.out.println("text: " + text);
					String lang = article.get(DB_LANG).toString();
					System.out.println("lang: " + lang);
					
					Object uriObj = article.get(DB_URI);
					if (uriObj != null)
						System.out.println("uri: " + uriObj.toString());
					Object dateObj = article.get(DB_DATE);
					if (dateObj != null)
						System.out.println("date: " + dateObj.toString());
					Object hostnameObj = article.get(DB_HOSTNAME);
					if (hostnameObj != null)
						System.out.println("hostname: " + hostnameObj.toString());
					Map<String, String> location = (Map<String, String>) article.get(DB_LOCATION);
					if (location != null) {
						Object latitudeObj = location.get(DB_LATITUDE);
						if (latitudeObj != null)
							System.out.println("latitude: " + latitudeObj.toString());
						Object longitudeObj = location.get(DB_LONGITUDE);
						if (longitudeObj != null)
							System.out.println("longitude: " + longitudeObj.toString());
						Object countryObj = location.get(DB_COUNTRY);
						if (countryObj != null)
							System.out.println("country: " + countryObj.toString());
					}
					
					System.out.println();
					System.out.println();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		mongoClient.close();
	}

	// connect to db server
	public MongoClient initMongoDB() throws Exception {
		MongoClient mongoClient = new MongoClient(new MongoClientURI("mongodb://aifb-ls3-remus.aifb.kit.edu:19015"));
		return mongoClient;
	}

	public DBCollection getCollection(MongoClient mongoClient) {
		DB db = mongoClient.getDB("brexit-xlimeress");
		System.out.println("Connect to database successfully!");
		DBCollection collection = db.getCollection("newsfeed");
		System.out.println("Collection selected successfully!");
		return collection;
	}

	public DBCursor genDBCursor(DBCollection collection, String language, int start, int end) {
		BasicDBObject langQuery = new BasicDBObject(DB_ARTICLE + "." + DB_LANG, language.toString());
		BasicDBObject fields = new BasicDBObject();
		fields.put(DB_ARTICLE + "." + DB_ID, 1);
		fields.put(DB_ARTICLE + "." + DB_LANG, 2);
		fields.put(DB_ARTICLE + "." + DB_TITLE, 3);
		fields.put(DB_ARTICLE + "." + DB_TEXT, 4);
		fields.put(DB_ARTICLE + "." + DB_URI, 5);
		fields.put(DB_ARTICLE + "." + DB_DATE, 6);
		fields.put(DB_ARTICLE + "." + DB_HOSTNAME, 7);
		fields.put(DB_ARTICLE + "." + DB_LOCATION, 8);
		DBCursor cursor = collection.find(langQuery, fields).sort(new BasicDBObject(DB_ID, 1));
		if (start < 0)
			start = 0;
		cursor.skip(start);
		if (end > start)
			cursor.limit(end - start + 1);
		return cursor;
	}

}
