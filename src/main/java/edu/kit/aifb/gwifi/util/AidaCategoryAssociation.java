package edu.kit.aifb.gwifi.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

public class AidaCategoryAssociation implements CategoryAssociation {

	private static final String MONGODB_URL = "mongodb://aifb-ls3-remus.aifb.kit.edu:19010";
	private static final String DBNAME = "congDB";
	private static final String COLLECTION = "Category_Entity_EN";
	private static final String FIELD_CATEGORY = "category";
	private static final String FIELD_ENTITY = "entity";
	private static final String FIELD_DEPTH = "depth";
	private DBCollection cate_entity;

	private static final String CATEGORY_FILE = "res/categories_aida.txt";
//	private static final String CATEGORY_FILE = "/home/ls3data/users/lzh/congliu/categories_aida.txt";
	private Map<String, String> categories = new HashMap<String, String>();

	private static Logger logger = Logger.getLogger(AidaCategoryAssociation.class);

	public AidaCategoryAssociation() {
		mongoDBInitialization();
		loadAidaCateogries();
	}

	public void mongoDBInitialization() {
		try {
			MongoClient mongoClient = new MongoClient(new MongoClientURI(MONGODB_URL));
			DB db = mongoClient.getDB(DBNAME);
			cate_entity = db.getCollection(COLLECTION);
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
	 * given a topic vertex, get the category sets which this topic belongs to.
	 * 
	 * @param topicVertex
	 * @return
	 */
	public Set<String> getCategories(String topicTitle) {
		Set<String> topic2categories = new HashSet<String>();
		BasicDBObject topicQuery = new BasicDBObject();
		topicQuery.put(FIELD_ENTITY, topicTitle);
		DBCursor topicCollection = cate_entity.find(topicQuery);
		while (topicCollection.hasNext()) {
			String cateName = topicCollection.next().get(FIELD_CATEGORY).toString();
			if (categories.keySet().contains(cateName)) {
				topic2categories.add(cateName);
			}
		}
		return topic2categories;
	}

	/**
	 * given a topic, get the category(aida) sets which this topic（with depth"artDep"） belongs to.
	 * 
	 * @param topicVertex
	 * @return
	 */
	public Map<String, Double> getCategoriesWithWeights(String topicTitle) {
		Map<String, Double> topic2categories = new HashMap<String, Double>();
		BasicDBObject topicQuery = new BasicDBObject();
		topicQuery.put(FIELD_ENTITY, topicTitle);
		DBCursor topicCollection = cate_entity.find(topicQuery);
		while (topicCollection.hasNext()) {
			DBObject dbc = topicCollection.next();
			String depth = dbc.get(FIELD_DEPTH).toString();
			if (null != depth) {
//				Double association = 1.0 / Integer.parseInt(depth);
				Double association = Math.log(20.0 / Integer.parseInt(depth));
				String cateName = dbc.get(FIELD_CATEGORY).toString();
				if (categories.values().contains(cateName) && association > 0) {
					topic2categories.put(cateName, association);
				}
			} 
		}
		return topic2categories;
	}

}
