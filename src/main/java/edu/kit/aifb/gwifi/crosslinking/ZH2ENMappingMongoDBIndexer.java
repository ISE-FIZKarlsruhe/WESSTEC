package edu.kit.aifb.gwifi.crosslinking;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;


/**
 * 
 */
public class ZH2ENMappingMongoDBIndexer {
	
	public static final String EN = "en";
	public static final String ZH = "zh";

	private MongoClient _mongo; 
	private DB _db;
	private DBCollection _mappingsCollection;
	private BufferedReader _reader;

	// mongodb://aifb-ls3-maia.aifb.kit.edu:19005
	// ABIRS
	// Mappings_ZH_EN
	// /home/aifb-ls3-maia/lzh/zh-en_links.dat
	public static void main(String[] args) {
		try {
			ZH2ENMappingMongoDBIndexer indexer = new ZH2ENMappingMongoDBIndexer(args[0], args[1], args[2], args[3]);
			indexer.insertData();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public ZH2ENMappingMongoDBIndexer(String url, String dbName, String colName, String filePath)
			throws UnknownHostException, FileNotFoundException, UnsupportedEncodingException {
		_mongo = new MongoClient(new MongoClientURI(url));
		_db = _mongo.getDB(dbName);
		_mappingsCollection = _db.getCollection(colName);
		_reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "UTF-8"));
	}
	
	private void insertData() throws IOException {
		try {
			int i = 0;
			String line;
			while ((line = _reader.readLine()) != null) {
				String[] data = line.split("\t");
				if(data.length != 2)
					continue;
				System.out.println(++i + ":" + data[0] + ":" + data[1]);
				BasicDBObject dbObject = new BasicDBObject();
				dbObject.append(ZH, data[0]);
				dbObject.append(EN, data[1]);
				_mappingsCollection.insert(dbObject);
			}
			_mappingsCollection.createIndex(new BasicDBObject(ZH, 1));
			_mappingsCollection.createIndex(new BasicDBObject(EN, 1));
			
			_reader.close();
			_mongo.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
