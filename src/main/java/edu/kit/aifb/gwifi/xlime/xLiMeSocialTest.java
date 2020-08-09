package edu.kit.aifb.gwifi.xlime;

import java.io.File;
import java.io.PrintWriter;
import java.util.Map;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

import edu.kit.aifb.gwifi.util.nlp.Language;

public class xLiMeSocialTest {

	public final static String ITEMS_TAG = "items";
	public final static String ITEM_TAG = "item";
	public final static String DB_ID = "_id";
	public final static String DB_LANG = "lang";
	public final static String DB_CONTENT = "content";
	public final static String DB_CONTENT_FULL = "full";
	public final static String DB_CREATED = "created";
	public final static String DB_CREATED_FORMATTED = "formatted";
	public final static String DB_PUBLISHER = "publisher";
	public final static String DB_PUBLISHER_URL = "url";
	public final static String DB_SOURCE = "source";
	public final static String DB_SOURCETYPE = "sourceType";
	public final static String DB_CREATOR = "creator";
	public final static String DB_CREATOR_URL = "url";

	public static void main(String[] args) throws Exception {
		Language language = Language.getLanguage(args[0]);
		int startPos = Integer.parseInt(args[1]);
		int endPos = Integer.parseInt(args[2]);
		int interval = Integer.parseInt(args[3]);
		xLiMeSocialTest mongoService = new xLiMeSocialTest();
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
					String id = obj.get(DB_ID).toString();
					System.out.println("id: " + id);
					String content = ((Map<String, String>) obj.get(DB_CONTENT)).get(DB_CONTENT_FULL);
					System.out.println("content: " + content);
					String created = ((Map<String, String>) obj.get(DB_CREATED)).get(DB_CREATED_FORMATTED);
					System.out.println("created: " + created);
					String publisher = ((Map<String, String>) obj.get(DB_PUBLISHER)).get(DB_PUBLISHER_URL);
					System.out.println("publisher: " + publisher);
					String source = obj.get(DB_SOURCE).toString();
					System.out.println("source: " + source);
					String sourceType = obj.get(DB_SOURCETYPE).toString();
					System.out.println("sourceType: " + sourceType);
					String creator = ((Map<String, String>) obj.get(DB_CREATOR)).get(DB_CREATOR_URL);
					System.out.println("creator: " + creator);
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
		DBCollection collection = db.getCollection("MicroPostBean");
		System.out.println("Collection selected successfully!");
		return collection;
	}

	public DBCursor genDBCursor(DBCollection collection, Language language, int start, int end) {
		BasicDBObject langQuery = new BasicDBObject(DB_LANG, language.toString());
		BasicDBObject fields = new BasicDBObject();
		fields.put(DB_LANG, 1);
		fields.put(DB_CONTENT, 2);
		fields.put(DB_CREATED, 3);
		fields.put(DB_PUBLISHER, 4);
		fields.put(DB_SOURCE, 5);
		fields.put(DB_SOURCETYPE, 6);
		fields.put(DB_CREATOR, 7);
		DBCursor cursor = collection.find(langQuery, fields);
		if (start < 0)
			start = 0;
		cursor.skip(start);
		if (end > start)
			cursor.limit(end - start + 1);
		return cursor;
	}

}
