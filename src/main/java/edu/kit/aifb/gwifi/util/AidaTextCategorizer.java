package edu.kit.aifb.gwifi.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

public class AidaTextCategorizer implements TextCategorizer {

	private static final String MONGODB_URL = "mongodb://aifb-ls3-remus.aifb.kit.edu:19010";
	private static final String DBNAME = "congDB";
	private static final String COLLECTION_AIDA = "aidaConelltestb";
	private static final String FIELD_AIDA_TITLE = "title";
	private static final String FIELD_AIDA_CATEGORY = "category";
	private DBCollection aida_collection;
	
	private static final String CATEGORY_FILE = "res/categories_aida.txt";
//	private static final String CATEGORY_FILE = "/home/ls3data/users/lzh/congliu/categories_aida.txt";
	private Map<String, String> categories = new HashMap<String, String>();

	private static Logger logger = Logger.getLogger(AidaTextCategorizer.class);

	public AidaTextCategorizer() {
		mongoDBInitialization();
		loadAidaCateogries();
	}

	public void mongoDBInitialization() {
		try {
			MongoClient mongoClient = new MongoClient(new MongoClientURI(MONGODB_URL));
			DB db = mongoClient.getDB(DBNAME);
			aida_collection = db.getCollection(COLLECTION_AIDA);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	
	public void loadAidaCateogries() {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(CATEGORY_FILE));
			String line;
			while ((line = reader.readLine()) != null) {
				if (line != null && !line.isEmpty()) {
					String[] linearray = line.split("\t");
					categories.put(linearray[0], linearray[1]);
				}
			}
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}


	/**
	 * query the text in aida mongodb, each category weighted 
	 * 
	 */
	public Map<String, Double> getCategoryWithProbability(String text) {
		Map<String, Double> catepro = new HashMap<String, Double>();
		String title = text.substring(0, text.indexOf("\n"));
		String cate = getCategoriesByTitle(title);
		if(cate == null)
			return catepro;
		String cates[] = cate.split(",");
		for (int i = 0; i < cates.length; i++) {
			if (categories.keySet().contains(cates[i])) {
				catepro.put(categories.get(cates[i]), 1.0 / cates.length);
			}
		}
		return catepro;
	}

	/*
	 * fuzzy search the title in the mongodb aidaconelltestb in order to get the category of every text.
	 */
	public String getCategoriesByTitle(String s) {
		Pattern pattern = Pattern.compile("^.*" + s + ".*$");
		BasicDBObject q = new BasicDBObject();
		q.put(FIELD_AIDA_TITLE, pattern);
		DBObject cursor = aida_collection.findOne(q);
		if (cursor == null) {
			int indexOfMinus = s.indexOf("-");
			int indexOfslash = s.indexOf("/");
			if (indexOfslash == -1 && indexOfMinus < 20) {
				s = s.substring(indexOfMinus + 2);
			} else if (indexOfslash == -1 && indexOfMinus >= 20) {
				s = s.substring(0, indexOfMinus - 1);
			} else if (indexOfMinus == -1) {
				s = s.substring(indexOfslash + 2);
			} else if (indexOfMinus < indexOfslash) {
				s = s.substring(indexOfMinus + 2, indexOfslash - 1);
			} else {
				s = s.substring(indexOfslash + 2, indexOfMinus - 1);
			}
			pattern = Pattern.compile("^.*" + s + ".*$");
			q.put(FIELD_AIDA_TITLE, pattern);
			cursor = aida_collection.findOne(q);
		}
		
		if(cursor != null) {
			Object obj =  cursor.get(FIELD_AIDA_CATEGORY);
			if(obj != null)
				return obj.toString();
			else 
				return null;
		}
		else 
			return null;
	}

}
