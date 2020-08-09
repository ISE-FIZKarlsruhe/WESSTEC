package edu.kit.aifb.gwifi.util;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import weka.core.FastVector;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

import edu.kit.aifb.gwifi.annotation.weighting.graph.categorybased.GraphBasedTopicWeighter;
import edu.kit.aifb.gwifi.annotation.weighting.graph.categorybased.GraphBasedTopicWeighter_salient;
import edu.kit.aifb.gwifi.textcategorization.MyFilteredClassifier;

public abstract class R128SCategoryAssociation implements CategoryAssociation {

	private static final String MONGODB_URL = "mongodb://aifb-ls3-remus.aifb.kit.edu:19010";
	private static final String DBNAME = "congDB";
	private static final String COLLECTION = "engMapping";
	private static final String COLLECTION_CATENAME= "engMapping_cateName";
	private static final String FIELD_CATEID = "cateID";
	private static final String FIELD_CATENAME="CateName";
	private static final String FIELD_ENTITY = "artTitle";
	private static final String FIELD_DEPTH = "depth";
	private DBCollection engMapping;
	private DBCollection engMapping_cateName;
	private BasicDBObject topicQuery;
	private BasicDBObject cateIDQuery;
    
	
	private List<String> categories;
	private static final String arffFile = "/home/ls3data/users/lzh/congliu/ReutersDataset_8.arff";
	private static Logger logger = Logger.getLogger(R128SCategoryAssociation.class);
	
	public R128SCategoryAssociation() throws Exception {
		mongoDBInitialization();
		categories = MyFilteredClassifier.getClassName(arffFile);//load r128s dataset categories
		}

	public void mongoDBInitialization() {
		try {
			MongoClient mongoClient = new MongoClient(new MongoClientURI(MONGODB_URL));
			DB db = mongoClient.getDB(DBNAME);
			engMapping = db.getCollection(COLLECTION);
			engMapping_cateName = db.getCollection(COLLECTION_CATENAME);
			topicQuery = new BasicDBObject();
			cateIDQuery = new BasicDBObject();
		} catch (UnknownHostException e) {
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
		topicQuery.put(FIELD_ENTITY, topicTitle);
		DBCursor topicCollection = engMapping.find(topicQuery);
		while (topicCollection.hasNext()) {
			String cateID = topicCollection.next().get(FIELD_CATEID).toString();
			cateIDQuery.put(FIELD_CATEID, cateID);
			String cateName = engMapping_cateName.findOne(cateIDQuery).get(FIELD_CATENAME).toString();
			if (categories.contains(cateName)) {
				topic2categories.add(cateName);
			}
			cateIDQuery.clear();
		}
		topicQuery.clear();
		return topic2categories;
	}

	/**
	 * given a topic vertex, get the category(aida) sets which this topic（with depth"artDep"） belongs to.
	 * 
	 * @param topicVertex
	 * @return
	 */
	public Map<String, Double> getCategoriesWithWeights(String topicTitle) {
		Map<String, Double> topic2categories = new HashMap<String, Double>();
		//String topicTitle = topicVertex.getTopic().getTitle();
		topicQuery.put(FIELD_ENTITY, topicTitle);
		DBCursor topicCollection = engMapping.find(topicQuery);
		while (topicCollection.hasNext()) {
			Double dep;
			// String cateID = topicCollection.next().get("cateID").toString();
			DBObject dbc = topicCollection.next();
			String cateID = dbc.get(FIELD_CATEID).toString();
			String depth = dbc.get(FIELD_DEPTH).toString();
			if (null != depth) {
				dep = Math.log(20.0 / Integer.parseInt(depth));
			} else {
				dep = 0.00001;
			}
			cateIDQuery.put(FIELD_CATEID, cateID);
			String cateName = engMapping_cateName.findOne(cateIDQuery).get(FIELD_CATENAME).toString();
			if (categories.contains(cateName)) {
				// if(cateName.equals("Business")){
				topic2categories.put(cateName, dep);
			}
			cateIDQuery.clear();
		}
		topicQuery.clear();
		return topic2categories;
	}

}

