package edu.kit.aifb.gwifi.annotation.weighting.graph.categorybased;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;

import edu.kit.aifb.gwifi.annotation.detection.DisambiguationUtil;
import edu.kit.aifb.gwifi.annotation.detection.Topic;
import edu.kit.aifb.gwifi.annotation.detection.TopicReference;
import edu.kit.aifb.gwifi.annotation.weighting.TopicWeighter;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.textcategorization.MyFilteredClassifier;
import edu.kit.aifb.gwifi.textcategorization.Wiki;
import edu.kit.aifb.gwifi.util.CategoryAssociation;
import edu.kit.aifb.gwifi.util.RelatednessCache;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import weka.core.FastVector;

public abstract class GraphBasedTopicWeighter extends TopicWeighter {

	private static float DEFAULT_MIN_WEIGHT = 0.1f;

	private Wikipedia wikipedia;
	private DisambiguationUtil disambiguationUtil;
	private List<String> cateVertices;// wiki category list
	private static String baseURL = "en.wikipedia.org";
	private Wiki wiki;
	// private List<String> categories;
	private Map<String, String> categories = new HashMap<String, String>();
	private FastVector fvNominalVal;
	private MyFilteredClassifier mf = new MyFilteredClassifier();
	private String arffFile = "/Users/aifb-ls3/MasterThesis/NewsDataSet/ReutersDataset_8.arff";
	private String modelFile = "/Users/aifb-ls3/MasterThesis/ReutersDataset_8.model";
	private String catenamefile="/home/ls3data/users/lzh/congliu/categories_aida.txt";
//	private String catenamefile = "res/categories_aida.txt";
	private static String text2Classify;
	private static String aidatextitle;
	private Mongo mongo;
	private DB db;

	// public DBCollection engMapping;//2 layer category
	// public DBCollection engMapping_cateName;
	private static final String DBNAME = "congDB";
	private static final String COLLECTION = "Category_Entity_EN";
	private static final String FIELD_CATEGORY = "category";
	private static final String FIELD_ENTITY = "entity";
	private static final String FIELD_DEPTH = "depth";
	private DBCollection cate_entity;

	private static final String COLLECTION_AIDA = "aidaConelltestb";
	private static final String FIELD_AIDA_TEXT = "text";
	private static final String FIELD_AIDA_CATEGORY = "category";
	private DBCollection aida_collection;
	private BasicDBObject topicQuery;
	// public BasicDBObject cateIDQuery;
	private Map<Topic, Map<String, Double>> topic_cate_value = new HashMap<Topic, Map<String, Double>>();
	private Map<String, Map<Topic, Double>> cate_topic_value = new HashMap<String, Map<Topic, Double>>();

	private static Logger logger = Logger.getLogger(GraphBasedTopicWeighter.class);

	public void getAidaCateogries(String catenamefile) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(catenamefile));
			String line;
			while ((line = reader.readLine()) != null) {
				// line = line.substring(line.indexOf("/")+1, line.indexOf("n"));
				if (line != null && !line.isEmpty()) {
					System.out.print(line + "\n");
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

	public void mongodbInitialization() throws Exception {
		// File f = new File("./configs/wikipedia-template-zh.xml");
		// wiki = new Wikipedia(f, false);
		try {
			mongo = new Mongo("aifb-ls3-remus.aifb.kit.edu", 19010);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		db = mongo.getDB(DBNAME);
		// cateID_topicName_mapping_en = db.getCollection("cateID_topicName_mapping_en");
		// cateID_cateName = db.getCollection("cateID_cateName");
		cate_entity = db.getCollection(COLLECTION);
		aida_collection = db.getCollection(COLLECTION_AIDA);
		// engMapping_cateName = db.getCollection("engMapping_cateName");

		topicQuery = new BasicDBObject();
		// cateIDQuery = new BasicDBObject();
		// System.out.print(wiki.getArticleByTitle("电影").getDepth()+" "+wiki.getCategoryByTitle("电影").getDepth());
	}

	public void fileIni() throws IOException {
		// not for web//
		Properties properties = new Properties();
		try {
			// FileInputStream inputFile = new FileInputStream(textCategorization);
			InputStream in = new FileInputStream("");
			properties.load(in);
			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// arffFile = properties.getProperty("arffFile");
		// modelFile = properties.getProperty("modelFile");

	}
	// public void classifierInitialization() {
	// // add the class label
	// try {
	// categories = MyFilteredClassifier.getClassName(arffFile);
	// } catch (Exception e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// fvNominalVal = new FastVector(2);
	// for (String s : categories) {
	// fvNominalVal.addElement(s);
	// }
	// // load the classification model
	// mf.loadModel(modelFile);
	// }

	// public void getTopicCategoryRelation(Collection<Topic> topics){
	// for (Topic topic : topics){
	// String topicTitle = topic.getTitle();
	// Map<String,Double> cate_value = new HashMap<String,Double>();
	// Map<Topic,Double> topic_value = new HashMap<Topic,Double>();
	// topic_value.put(topic,0.0);
	// for (String s : categories) {
	// cate_value.put(s, 0.0);
	// }
	//
	// topic_cate_value.put(topic, cate_value);
	// topicQuery.put("artTitle", topicTitle);
	// DBCursor topicCollection = cateID_topicName_mapping_en.find(topicQuery);
	// while (topicCollection.hasNext()) {
	// String cateID = topicCollection.next().get("cateID").toString();
	// cateIDQuery.put("cateID",cateID);
	// String cateName = cateID_cateName.findOne(cateIDQuery).get("CateName").toString();
	// if (categories.contains(cateName)) {
	// cate_value.put(cateName, 1.0);
	// topic_value.put(topic, 1.0);
	// }
	// topic_cate_value.put(topic, cate_value);
	// cate_topic_value.put(cateName, topic_value);
	// }
	// topicQuery.clear();
	// }

	public void graphTest(DirectedSparseGraph<Vertex, Edge> graph) throws IOException {
		PrintWriter pw = new PrintWriter(new FileWriter("/Users/aifb-ls3/MasterThesis/Graph1.txt", true));
		Collection<Edge> edges = graph.getEdges();
		for (Edge edge : edges) {
			pw.println("source:" + edge.getSource() + "target:" + edge.getTarget() + "edge weight:" + edge.getWeight()
					+ "\n");
		}
		// Collection<Vertex> vertexs = graph.getVertices();
		// for(Vertex vertex:vertexs){
		// pw.println(vertex+"\n");
		// }
		// Collection<Edge> edges = graph.getEdges();
		// int i=0;
		// System.err.print("how many edges are there:"+ edges.size());
		// for(Vertex vertex:vertexs){
		// for(Edge edge:edges){
		// if(edge.getSource().equals(vertex)){
		// pw.println("source:"+edge.getSource()+"target:"+edge.getTarget()+"edge weight:"+edge.getWeight()+"\n");
		// i++;
		// }
		// }
		// }
		// System.out.print("how many edges are printed:"+i);

	}

	/**
	 * @return the mappings of the topic index and its weight
	 */
	public abstract HashMap<Integer, Double> getTopicWeights(DirectedSparseGraph<Vertex, Edge> graph);

	/**
	 * Weights and sorts the given topics according to some criteria.
	 * 
	 * @param topics
	 *            the topics to be weighted and sorted.
	 * @param rc
	 *            a cache in which relatedness measures will be saved so they aren't repeatedly calculated. This may be
	 *            null.
	 * @return the weighted topics.
	 * @throws Exception
	 *             depends on the implementing class
	 */
	public ArrayList<Topic> getWeightedTopics(Collection<Topic> topics, RelatednessCache rc) {
		// this.getTopicCategoryRelation(topics);
		// System.err.print("begin to set the rc");
		if (rc == null)
			rc = new RelatednessCache(disambiguationUtil.getArticleComparer());

		long start = System.currentTimeMillis();
		// System.err.print("begin to set the graph");
		// DirectedSparseGraph<Vertex, Edge> graph = buildGraph(topics, rc);
		// DirectedSparseGraph<Vertex, Edge> graph = buildGraphwithCategory(topics, rc);
		DirectedSparseGraph<Vertex, Edge> graph = buildGraphTopicSensitive(topics, rc);
		// this.graphTest(graph);
		long end = System.currentTimeMillis();
		logger.debug("Time for building disambiguation graph: " + (end - start) + " ms");

		long newStart = System.currentTimeMillis();
		HashMap<Integer, Double> topicWeights = getTopicWeights(graph);
		end = System.currentTimeMillis();
		logger.debug("Time for performing graph algorithm: " + (end - newStart) + " ms");

		ArrayList<Topic> weightedTopics = setTopicWeights(topicWeights, topics);

		end = System.currentTimeMillis();
		logger.debug("Total time for topic weighting: " + (end - start) + " ms\n");

		return weightedTopics;
	}
	
	public ArrayList<Topic> getWeightedTopics(Collection<Topic> topics, RelatednessCache rc,
			Map<String, Double> categories, CategoryAssociation ca, double alpha) {
		return null;
	}

	/**
	 * get a relationship between a topic vertex to all the category verteies(the categories we choose for example 3rd
	 * wiki layer category)
	 * 
	 * @param topicVertex:
	 *            a topic vertex
	 * @return key: all the categories value: 1.0 (if topic belongs to the category)
	 * 
	 */
	// public Map<String, Double> getTopic2CategoryEdgeWeight(
	// TopicVertex topicVertex) {
	// Map<String,Double> cate_value = new HashMap<String,Double>();
	// String topicTitle = topicVertex.getTopic().getTitle();
	// topicQuery.put("artTitle", topicTitle);
	// DBCursor topicCollection = cateID_topicName_mapping_en.find(topicQuery);
	// while (topicCollection.hasNext()) {
	// String cateID = topicCollection.next().get("cateID").toString();
	// cateIDQuery.put("cateID",cateID);
	// String cateName = cateID_cateName.findOne(cateIDQuery).get("CateName").toString();
	// if (categories.contains(cateName)) {
	// cate_value.put(cateName, 1.0);
	// }
	// cateIDQuery.clear();
	// }
	// topicQuery.clear();
	// return cate_value;
	// }
	/**
	 * given a topic vertex, get the category sets which this topic belongs to.
	 * 
	 * @param topicVertex
	 * @return
	 */
	public Set<String> getTopic2CategoryEdgeWeight(TopicVertex topicVertex) {
		Set<String> topic2categories = new HashSet<String>();
		String topicTitle = topicVertex.getTopic().getTitle();
		topicQuery.put(FIELD_ENTITY, topicTitle);
		DBCursor topicCollection = cate_entity.find(topicQuery);
		while (topicCollection.hasNext()) {
			String cateName = topicCollection.next().get(FIELD_CATEGORY).toString();
			if (categories.keySet().contains(cateName)) {
				topic2categories.add(cateName);
			}
		}
		topicQuery.clear();
		return topic2categories;
	}

	// /**
	// * given a topic vertex, get the category sets which this topic（with depth"artDep"） belongs to.
	// * @param topicVertex
	// * @return
	// */
	// public Map<String,Double> getTopic2CategoryEdgeWeightWithDepth(
	// TopicVertex topicVertex) {
	// Map<String,Double> topic2categories = new HashMap<String,Double>();
	// String topicTitle = topicVertex.getTopic().getTitle();
	// topicQuery.put("artTitle", topicTitle);
	// DBCursor topicCollection = engMapping.find(topicQuery);
	// while (topicCollection.hasNext()) {
	// Double dep;
	// //String cateID = topicCollection.next().get("cateID").toString();
	// DBObject dbc = topicCollection.next();
	// String cateID = dbc.get("cateID").toString();
	// String depth = dbc.get("artDep").toString();
	// if(null!=depth){
	// dep = 1.0/Integer.parseInt(depth);
	// }
	// else{
	// dep = 0.00001;
	// }
	// cateIDQuery.put("cateID",cateID);
	// String cateName = engMapping_cateName.findOne(cateIDQuery).get("CateName").toString();
	// if (categories.contains(cateName)) {
	// //if(cateName.equals("Business")){
	// topic2categories.put(cateName,dep);
	// }
	// cateIDQuery.clear();
	// }
	// topicQuery.clear();
	// return topic2categories;
	// }

	/**
	 * given a topic vertex, get the category(aida) sets which this topic（with depth"artDep"） belongs to.
	 * 
	 * @param topicVertex
	 * @return
	 */
	public Map<String, Double> getAidaTopic2CategoryEdgeWeightWithDepth(TopicVertex topicVertex) {
		Map<String, Double> topic2categories = new HashMap<String, Double>();
		String topicTitle = topicVertex.getTopic().getTitle();
		topicQuery.put(FIELD_ENTITY, topicTitle);
		DBCursor topicCollection = cate_entity.find(topicQuery);
		while (topicCollection.hasNext()) {
			Double dep;
			DBObject dbc = topicCollection.next();
			String depth = dbc.get(FIELD_DEPTH).toString();
			if (null != depth) {
				dep = 1.0 / Integer.parseInt(depth);
			} else {
				dep = 0.00001;
			}
			String cateName = dbc.get(FIELD_CATEGORY).toString();
			if (categories.values().contains(cateName)) {
				topic2categories.put(cateName, dep);
			}
		}
		topicQuery.clear();
		return topic2categories;
	}

	// /**
	// *
	// * @param cate: a category Name
	// * @return the number of articles in this category
	// */
	//
	// public int getNumberOfArtsinCate(String cate){
	// int artCount = 0;
	// cateIDQuery.put("cateName",cate);
	// String cateID = cateID_cateName.findOne(cateIDQuery).get("cateID").toString();
	// topicQuery.put("cateID",cateID);
	// DBCursor topicCollection = cateID_topicName_mapping_en.find(topicQuery);
	// artCount = topicCollection.count();
	// return artCount;
	// }

	// /**
	// * get the classification probability of a given text to all the category we choose
	// * for example 3rd layer category in wiki.
	// * @return key: category name
	// * value: the classification probability the a text is classified to this category
	// * @throws IOException
	// */
	// public Map<String, Double> getClassificationProbability()
	// throws IOException {
	//
	// text2Classify = GraphBasedTopicWeighter.getText2Classify();
	// mf.setText(text2Classify);
	// mf.setCateName(categories);
	// mf.makeInstance(fvNominalVal);
	// mf.classify();
	// Map<String, Double> catepro = mf.getCate_prop();
	// return catepro;
	//
	// }

	// /**
	// * get the classification probability of a given text to all the category we choose
	// * for example 3rd layer category in wiki.
	// * @param <ValueComparator>
	// * @return key: category name
	// * value: the classification probability the a text is classified to this category
	// * @throws IOException
	// */
	// public Map<String, Double> getBignCatePro(int n)
	// throws IOException {
	//
	// text2Classify = GraphBasedTopicWeighter.getText2Classify();
	// //System.out.print("hahahhahah"+text2Classify);
	// mf.setText(text2Classify);
	// mf.setCateName(categories);
	// mf.makeInstance(fvNominalVal);
	// mf.classify();
	// Map<String, Double> catepro = mf.getCate_prop();
	//
	// // Convert Map to List
	// List<Map.Entry<String, Double>> list =
	// new LinkedList<Map.Entry<String, Double>>(catepro.entrySet());
	//
	// // Sort list with comparator, to compare the Map values
	// Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
	// public int compare(Map.Entry<String, Double> o1,
	// Map.Entry<String, Double> o2) {
	// return (o2.getValue()).compareTo(o1.getValue());
	// }
	// });
	//
	// // Convert sorted map back to a Map
	// int i=1;
	// Map<String, Double> sortedMap = new LinkedHashMap<String, Double>();
	// for (Iterator<Map.Entry<String, Double>> it = list.iterator(); it.hasNext();) {
	// Map.Entry<String, Double> entry = it.next();
	// sortedMap.put(entry.getKey(), entry.getValue());
	// i++;
	// if(i>n){
	// break;
	// }
	// }
	// return sortedMap;
	// }

	/**
	 * query the text in aida mongodb, , each category weighted m
	 * 
	 * @param <ValueComparator>
	 * @return key: category code name value: 0.5
	 * @throws IOException
	 */
	public Map<String, Double> getAidaCatePro(double m) {
		this.getAidaCateogries(this.catenamefile);
		Map<String, Double> catepro = new HashMap<String, Double>();
		// text2Classify = GraphBasedTopicWeighter.getText2Classify();
		// aidatextitle = GraphBasedTopicWeighter.getText2Classify()
		BasicDBObject query = new BasicDBObject();
		// query.put(FIELD_AIDA_TEXT,text2Classify);
		// query.put("title", aidatextitle);
		// String cate = aida_collection.findOne(query).get(FIELD_AIDA_CATEGORY).toString();
		String cate = getCategoriesByTitle(GraphBasedTopicWeighter.getTitleOfaidatext());
		if (cate == null) {
			catepro.put("News", 0.5);
		} else {
			String cates[] = cate.split(",");
			for (int i = 0; i < cates.length; i++) {
				if (categories.keySet().contains(cates[i])) {
					catepro.put(categories.get(cates[i]), m);
				}
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
		q.put("title", pattern);
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
			q.put("title", pattern);
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
		// System.out.println(cursor.get("title").toString());
	}

	public static String getText2Classify() {
		return text2Classify;
	}

	public static void setText2Classify(String text2Classify) {
		GraphBasedTopicWeighter.text2Classify = text2Classify;
	}

	public static void setTitleaidatext(String aidatext) {
		String aidatexttitle = aidatext.substring(0, aidatext.indexOf("\n"));
		GraphBasedTopicWeighter.aidatextitle = aidatexttitle;
	}

	public static String getTitleOfaidatext() {
		return aidatextitle;
	}

	public DirectedSparseGraph<Vertex, Edge> buildGraph(Collection<Topic> topics, RelatednessCache rc)
			throws Exception {

		long start = System.currentTimeMillis();

		DirectedSparseGraph<Vertex, Edge> graph = new DirectedSparseGraph<Vertex, Edge>();

		Set<TopicReferenceVertex> refVertices = new HashSet<TopicReferenceVertex>();
		Map<Integer, TopicVertex> index2topicVertices = new HashMap<Integer, TopicVertex>();
		Map<Integer, Set<TopicVertex>> pageId2topicVertices = new HashMap<Integer, Set<TopicVertex>>();
		Set<Edge> refTopicEdges = new HashSet<Edge>();
		Set<Edge> topicTopicEdges = new HashSet<Edge>();

		Map<TopicReference, Set<Topic>> ref2topics = new HashMap<TopicReference, Set<Topic>>();
		Map<Topic, Set<TopicReference>> topic2references = new HashMap<Topic, Set<TopicReference>>();

		for (Topic topic : topics) {
			TopicVertex topicVertex = new TopicVertex(topic);
			topicVertex.setWeight(0.0);
			index2topicVertices.put(topic.getIndex(), topicVertex);

			// System.err.print("pageID2TopicVertices:"+pageId2topicVertices+"\n");

			Set<TopicVertex> topicVertices = pageId2topicVertices.get(topic.getId());
			// System.err.print("TopVertices"+topicVertices+"\n");
			if (topicVertices == null) {
				topicVertices = new HashSet<TopicVertex>();
				pageId2topicVertices.put(topic.getId(), topicVertices);
			}
			topicVertices.add(topicVertex);

			Set<TopicReference> topicReferences = topic2references.get(topic);
			if (topicReferences == null) {
				topicReferences = new HashSet<TopicReference>();
				topic2references.put(topic, topicReferences);
			}

			for (TopicReference ref : topic.getReferences()) {
				topicReferences.add(ref);

				Set<Topic> referredTopics = ref2topics.get(ref);
				if (referredTopics == null) {
					referredTopics = new HashSet<Topic>();
					ref2topics.put(ref, referredTopics);
				}
				referredTopics.add(topic);
			}
		}

		long end = System.currentTimeMillis();
		logger.debug("Time for graph preprocessing: " + (end - start) + " ms");

		start = System.currentTimeMillis();

		for (TopicReference reference : ref2topics.keySet()) {
			TopicReferenceVertex refVertex = new TopicReferenceVertex(reference);
			double vertexWeight = reference.getLabel().getLinkProbability();
			refVertex.setWeight(vertexWeight);
			refVertices.add(refVertex);
			Set<Topic> referredTopics = ref2topics.get(reference);
			for (Topic topic : referredTopics) {
				Vertex topicVertex = index2topicVertices.get(topic.getIndex());
				Edge edge = new Edge(refVertex, topicVertex);
				double edgeWeight = topic.getCommenness();
				edge.setWeight(edgeWeight);
				refVertex.addEdge(edge);
				refTopicEdges.add(edge);
			}
		}

		end = System.currentTimeMillis();
		logger.debug("Time for creating reference-topic edges: " + (end - start) + " ms");

		start = System.currentTimeMillis();

		// considering the pairs of topic vertices that are directly connected
		// in the data graph using pageLinksIn
		for (int index : index2topicVertices.keySet()) {
			TopicVertex target = index2topicVertices.get(index);
			Article[] linksIn = target.getTopic().getLinksIn();
			for (Article article : linksIn) {
				int pageId = article.getId();
				Set<TopicVertex> sources = pageId2topicVertices.get(pageId);
				if (sources != null) {
					// System.out.print("sources:"+sources+"\n");
				}
				if (sources == null)
					continue;
				double relatedness = rc.getRelatedness(target.getTopic(), article);
				if (relatedness == 0)
					continue;
				for (TopicVertex source : sources) {
					Set<TopicReference> sourceReferenceSet = topic2references.get(target.getTopic());
					Set<TopicReference> targetReferenceSet = topic2references.get(source.getTopic());
					Set<TopicReference> intersection = new HashSet<TopicReference>(sourceReferenceSet);
					intersection.retainAll(targetReferenceSet);
					if (intersection.size() != 0)
						continue;
					Edge edge = new Edge(target, source);
					edge.setWeight(relatedness);
					target.addEdge(edge);
					topicTopicEdges.add(edge);

					edge = new Edge(source, target);
					edge.setWeight(relatedness);
					source.addEdge(edge);
					topicTopicEdges.add(edge);
				}
			}
		}

		for (Vertex vertex : index2topicVertices.values()) {
			boolean added = graph.addVertex(vertex);

		}

		for (Edge edge : refTopicEdges) {
			boolean added = graph.addEdge(edge, edge.getSource(), edge.getTarget());

		}

		for (Edge edge : topicTopicEdges) {
			boolean added = graph.addEdge(edge, edge.getSource(), edge.getTarget());

		}

		end = System.currentTimeMillis();
		logger.debug("Time for adding vertices and edges into the graph: " + (end - start) + " ms");

		return graph;
	}

	// public DirectedSparseGraph<Vertex, Edge> buildGraphwithCategory(
	// Collection<Topic> topics, RelatednessCache rc) throws Exception {
	//
	// long start = System.currentTimeMillis();
	// DirectedSparseGraph<Vertex, Edge> graph = new DirectedSparseGraph<Vertex, Edge>();
	// Set<TopicReferenceVertex> refVertices = new HashSet<TopicReferenceVertex>();
	// Set<CategoryVertex> cateVertexes = new HashSet<CategoryVertex>();
	// Map<Integer, TopicVertex> index2topicVertices = new HashMap<Integer, TopicVertex>();
	// Map<Integer, Set<TopicVertex>> pageId2topicVertices = new HashMap<Integer, Set<TopicVertex>>();
	// Set<Edge> refTopicEdges = new HashSet<Edge>();
	// Set<Edge> topicTopicEdges = new HashSet<Edge>();
	// Set<Edge> cateTopicEdges = new HashSet<Edge>();
	// Map<TopicReference, Set<Topic>> ref2topics = new HashMap<TopicReference, Set<Topic>>();
	// Map<Topic, Set<TopicReference>> topic2references = new HashMap<Topic, Set<TopicReference>>();
	//
	// Map<TopicVertex, Set<CategoryVertex>> topic2cates = new HashMap<TopicVertex, Set<CategoryVertex>>();
	//
	//
	// Map<String, Double> classificationProbability = this
	// .getClassificationProbability(); // get the probability
	//
	// for (String s : categories) {
	// CategoryVertex cateVertex = new CategoryVertex(s);
	// cateVertex.setWeight(classificationProbability.get(s));
	// cateVertexes.add(cateVertex);
	// }
	//
	// for (Topic topic : topics) {
	// TopicVertex topicVertex = new TopicVertex(topic);
	// topicVertex.setWeight(0.0);
	// index2topicVertices.put(topic.getIndex(), topicVertex);
	// Set<TopicVertex> topicVertices = pageId2topicVertices.get(topic
	// .getId());
	//
	// if (topicVertices == null) {
	// topicVertices = new HashSet<TopicVertex>();
	// pageId2topicVertices.put(topic.getId(), topicVertices);
	// }
	// topicVertices.add(topicVertex);
	//
	// // topic category relation
	// topic2cates.put(topicVertex, cateVertexes);
	//
	// Set<TopicReference> topicReferences = topic2references.get(topic);
	// if (topicReferences == null) {
	// topicReferences = new HashSet<TopicReference>();
	// topic2references.put(topic, topicReferences);
	// }
	//
	// for (TopicReference ref : topic.getReferences()) {
	// topicReferences.add(ref);
	//
	// Set<Topic> referredTopics = ref2topics.get(ref);
	// if (referredTopics == null) {
	// referredTopics = new HashSet<Topic>();
	// ref2topics.put(ref, referredTopics);
	// }
	// referredTopics.add(topic);
	// }
	// }
	//
	// long end = System.currentTimeMillis();
	// logger.debug("Time for graph preprocessing: " + (end - start) + " ms");
	//
	// start = System.currentTimeMillis();
	//
	//
	// //start = System.currentTimeMillis();
	// //Map<String,Integer> topicReleatedCategories = new HashMap<String,Integer>();
	// for (TopicVertex topicVertex : topic2cates.keySet()) {
	// Set<CategoryVertex> referredCategoryVertex=topic2cates.get(topicVertex);
	// start = System.currentTimeMillis();
	// Set<String> topic2Categories = getTopic2CategoryEdgeWeight(topicVertex);
	// //end = System.currentTimeMillis();
	// logger.debug("Time for visit mongodb once: " + (end - start) + " ms");
	//// for(String ca:topic2Categories){
	//// if(!topicReleatedCategories.keySet().contains(ca))
	//// topicReleatedCategories.put(ca,1);
	//// else{
	//// Integer a = topicReleatedCategories.get(ca);
	//// a=a+1;
	//// topicReleatedCategories.put(ca,a);
	//// }
	//// }
	//
	// for (CategoryVertex categoryVertex : referredCategoryVertex) {
	// if(topic2Categories.contains(categoryVertex.getCategory())){
	// Edge edget2c = new Edge(topicVertex, categoryVertex);
	// //edget2c.setWeight((double)1.0/topic2Categories.size());
	// edget2c.setWeight(1.0);
	// topicVertex.addEdge(edget2c);
	// cateTopicEdges.add(edget2c);
	// edget2c = new Edge(categoryVertex, topicVertex);
	// //edget2c.setWeight(0.0);
	// edget2c.setWeight(1.0);
	// categoryVertex.addEdge(edget2c);
	// cateTopicEdges.add(edget2c);
	// }
	// }
	// for(Edge e:cateTopicEdges){
	// e.setWeight(e.getWeight()/cateTopicEdges.size());
	// }
	// }
	//// // category to topic edges nomalization
	//// for(Edge e:cateTopicEdges){
	//// if(e.getWeight()==0){
	//// CategoryVertex cate = (CategoryVertex) e.getSource();
	//// e.setWeight(1.0/topicReleatedCategories.get(cate.getCategory()));
	//// }
	//// }
	//
	//
	// end = System.currentTimeMillis();
	// logger.debug("Time for topic category graph: " + (end - start) + " ms");
	//
	//
	//
	//
	//
	//
	// for (TopicReference reference : ref2topics.keySet()) {
	// TopicReferenceVertex refVertex = new TopicReferenceVertex(reference);
	// double vertexWeight = reference.getLabel().getLinkProbability();
	// refVertex.setWeight(vertexWeight);
	// refVertices.add(refVertex);
	// Set<Topic> referredTopics = ref2topics.get(reference);
	// for (Topic topic : referredTopics) {
	// Vertex topicVertex = index2topicVertices.get(topic.getIndex());
	// Edge edge = new Edge(refVertex, topicVertex);
	// double edgeWeight = topic.getCommenness();
	// edge.setWeight(edgeWeight);
	// refVertex.addEdge(edge);
	// refTopicEdges.add(edge);
	// }
	// }
	//
	// end = System.currentTimeMillis();
	// logger.debug("Time for creating reference-topic edges: "
	// + (end - start) + " ms");
	//
	// start = System.currentTimeMillis();
	//
	// // considering the pairs of topic vertices that are directly connected
	// // in the data graph using pageLinksIn
	// for (int index : index2topicVertices.keySet()) {
	// TopicVertex target = index2topicVertices.get(index);
	// Article[] linksIn = target.getTopic().getLinksIn();
	// for (Article article : linksIn) {
	// int pageId = article.getId();
	// Set<TopicVertex> sources = pageId2topicVertices.get(pageId);
	// if (sources != null) {
	// // System.out.print("sources:"+sources+"\n");
	// }
	// if (sources == null)
	// continue;
	// double relatedness = rc.getRelatedness(target.getTopic(),
	// article);
	// if (relatedness == 0)
	// continue;
	// for (TopicVertex source : sources) {
	// Set<TopicReference> sourceReferenceSet = topic2references
	// .get(target.getTopic());
	// Set<TopicReference> targetReferenceSet = topic2references
	// .get(source.getTopic());
	// Set<TopicReference> intersection = new HashSet<TopicReference>(
	// sourceReferenceSet);
	// intersection.retainAll(targetReferenceSet);
	// if (intersection.size() != 0)
	// continue;
	// Edge edge = new Edge(target, source);
	// edge.setWeight(relatedness);
	// target.addEdge(edge);
	// topicTopicEdges.add(edge);
	//
	// edge = new Edge(source, target);
	// edge.setWeight(relatedness);
	// source.addEdge(edge);
	// topicTopicEdges.add(edge);
	// }
	// }
	// }
	//
	// end = System.currentTimeMillis();
	// logger.debug("Time for creating topic-topic edges: " + (end - start)
	// + " ms");
	//
	// start = System.currentTimeMillis();
	//
	// // category vertex
	// for (Vertex vertex : cateVertexes) {
	// boolean added = graph.addVertex(vertex);
	//
	// }
	//
	// for (Vertex vertex : refVertices) {
	// boolean added = graph.addVertex(vertex);
	//
	// }
	//
	// for (Vertex vertex : index2topicVertices.values()) {
	// boolean added = graph.addVertex(vertex);
	//
	// }
	//
	// for (Edge edge : cateTopicEdges) {
	// boolean added = graph.addEdge(edge, edge.getSource(),
	// edge.getTarget());
	//
	// }
	//
	// for (Edge edge : refTopicEdges) {
	// boolean added = graph.addEdge(edge, edge.getSource(),
	// edge.getTarget());
	//
	// }
	//
	// for (Edge edge : topicTopicEdges) {
	// boolean added = graph.addEdge(edge, edge.getSource(),
	// edge.getTarget());
	//
	// }
	//
	// end = System.currentTimeMillis();
	// logger.debug("Time for adding vertices and edges into the graph: "
	// + (end - start) + " ms");
	//
	// return graph;
	// }

	public DirectedSparseGraph<Vertex, Edge> buildGraphTopicSensitive(Collection<Topic> topics, RelatednessCache rc) {

		// long start = System.currentTimeMillis();

		DirectedSparseGraph<Vertex, Edge> graph = new DirectedSparseGraph<Vertex, Edge>();

		Set<TopicReferenceVertex> refVertices = new HashSet<TopicReferenceVertex>();
		Map<Integer, TopicVertex> index2topicVertices = new HashMap<Integer, TopicVertex>();
		Map<Integer, Set<TopicVertex>> pageId2topicVertices = new HashMap<Integer, Set<TopicVertex>>();
		Set<Edge> refTopicEdges = new HashSet<Edge>();
		Set<Edge> topicTopicEdges = new HashSet<Edge>();
		Map<TopicReference, Set<Topic>> ref2topics = new HashMap<TopicReference, Set<Topic>>();
		Map<Topic, Set<TopicReference>> topic2references = new HashMap<Topic, Set<TopicReference>>();
		Map<String, Double> bigNcatePro = this.getAidaCatePro(0.5);// aida categories from mongodb

		long b = System.currentTimeMillis();
		for (Topic topic : topics) {
			double i = 0.0;
			double vertex_prior = 0.0;
			TopicVertex topicVertex = new TopicVertex(topic);
			long beforeDB = System.currentTimeMillis();
			Map<String, Double> topic_cates = getAidaTopic2CategoryEdgeWeightWithDepth(topicVertex);
			long endDB = System.currentTimeMillis();
			for (String topic_cate : topic_cates.keySet()) {
				if (bigNcatePro.keySet().contains(topic_cate)) {
					i = topic_cates.get(topic_cate);
					vertex_prior = vertex_prior + i;
				}
			}
			topicVertex.setWeight(vertex_prior / bigNcatePro.size());
			index2topicVertices.put(topic.getIndex(), topicVertex);
			Set<TopicVertex> topicVertices = pageId2topicVertices.get(topic.getId());
			if (topicVertices == null) {
				topicVertices = new HashSet<TopicVertex>();
				pageId2topicVertices.put(topic.getId(), topicVertices);
			}
			topicVertices.add(topicVertex);

			Set<TopicReference> topicReferences = topic2references.get(topic);
			if (topicReferences == null) {
				topicReferences = new HashSet<TopicReference>();
				topic2references.put(topic, topicReferences);
			}

			for (TopicReference ref : topic.getReferences()) {
				topicReferences.add(ref);

				Set<Topic> referredTopics = ref2topics.get(ref);
				if (referredTopics == null) {
					referredTopics = new HashSet<Topic>();
					ref2topics.put(ref, referredTopics);
				}
				referredTopics.add(topic);
			}
		}
		long e = System.currentTimeMillis();
		long b1 = System.currentTimeMillis();
		for (TopicReference reference : ref2topics.keySet()) {
			TopicReferenceVertex refVertex = new TopicReferenceVertex(reference);
			double vertexWeight = reference.getLabel().getLinkProbability();
			refVertex.setWeight(vertexWeight);
			refVertices.add(refVertex);
			Set<Topic> referredTopics = ref2topics.get(reference);
			// int sizeOfRefferedTopics = referredTopics.size();
			for (Topic topic : referredTopics) {
				Vertex topicVertex = index2topicVertices.get(topic.getIndex());
				Edge edge = new Edge(refVertex, topicVertex);
				double edgeWeight = topic.getCommenness();
				// double edgeWeight = 1/sizeOfRefferedTopics;
				edge.setWeight(edgeWeight);
				refVertex.addEdge(edge);
				refTopicEdges.add(edge);
			}
		}

		// end = System.currentTimeMillis();
		// logger.debug("Time for creating reference-topic edges: "
		// + (end - start) + " ms");

		// considering the pairs of topic vertices that are directly connected
		// in the data graph using pageLinksIn
		for (int index : index2topicVertices.keySet()) {
			TopicVertex target = index2topicVertices.get(index);
			Article[] linksIn = target.getTopic().getLinksIn();
			for (Article article : linksIn) {
				int pageId = article.getId();
				Set<TopicVertex> sources = pageId2topicVertices.get(pageId);
				if (sources != null) {
					// System.out.print("sources:"+sources+"\n");
				}
				if (sources == null)
					continue;
				double relatedness = rc.getRelatedness(target.getTopic(), article);
				if (relatedness == 0)
					continue;
				for (TopicVertex source : sources) {
					Set<TopicReference> sourceReferenceSet = topic2references.get(target.getTopic());
					Set<TopicReference> targetReferenceSet = topic2references.get(source.getTopic());
					Set<TopicReference> intersection = new HashSet<TopicReference>(sourceReferenceSet);
					intersection.retainAll(targetReferenceSet);
					if (intersection.size() != 0)
						continue;
					Edge edge = new Edge(target, source);
					edge.setWeight(relatedness);
					target.addEdge(edge);
					topicTopicEdges.add(edge);

					edge = new Edge(source, target);
					edge.setWeight(relatedness);
					source.addEdge(edge);
					topicTopicEdges.add(edge);
				}
			}
		}
		long e1 = System.currentTimeMillis();
		logger.debug("Time for create edges: " + (e1 - b1) + " ms");

		for (Vertex vertex : index2topicVertices.values()) {
			boolean added = graph.addVertex(vertex);

		}

		for (Edge edge : refTopicEdges) {
			boolean added = graph.addEdge(edge, edge.getSource(), edge.getTarget());

		}

		for (Edge edge : topicTopicEdges) {
			boolean added = graph.addEdge(edge, edge.getSource(), edge.getTarget());

		}

		return graph;
	}

}
