package edu.kit.aifb.gwifi.xlime;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Element;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

import edu.kit.aifb.gwifi.service.Service.DisambiguationModel;
import edu.kit.aifb.gwifi.service.Service.KB;
import edu.kit.aifb.gwifi.service.Service.MentionMode;
import edu.kit.aifb.gwifi.service.Service.NLPModel;
import edu.kit.aifb.gwifi.service.Service.RepeatMode;
import edu.kit.aifb.gwifi.service.Service.ResponseMode;
import edu.kit.aifb.gwifi.util.nlp.Language;

public class xLiMeSocialEntityCategoryAnnotation {

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

	private static int interval;
	private static int start;
	private static int end;
	private static NLPEntityCategoryAnnotationService annoService;
	private static PrintWriter statisticsPW;

	public class AnnoThread implements Runnable {
		private String id;
		private String content;
		private Element xmlDoc;
		private Map<String, String> attributes;
		private NLPEntityCategoryAnnotationService service;

		public AnnoThread(String id, String content, Map<String, String> attributes, Element xmlDoc,
				NLPEntityCategoryAnnotationService service) {
			this.id = id;
			this.content = content;
			this.attributes = attributes;
			this.xmlDoc = xmlDoc;
			this.service = service;
		}

		@Override
		public void run() {
			try {
				System.out.println(id + "    start!");
				Element ele = service.annotateContent(id, content.replace("#", " ").replace("brexit", "Brexit"),
						attributes);
				synchronized (xmlDoc) {
					if (ele != null)
						xmlDoc.appendChild(ele);
				}
				System.out.println(id + "    end!");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public xLiMeSocialEntityCategoryAnnotation(NLPModel nlpModel, Language language) throws Exception {
		annoService = new NLPEntityCategoryAnnotationService("configs/hub-template.xml",
				"configs/wikipedia-template-" + language.getLabel() + ".xml", "configs/NLPConfig.properties", language,
				Language.EN, KB.WIKIPEDIA, nlpModel, DisambiguationModel.PAGERANK_HITSHUB, MentionMode.NON_OVERLAPPED,
				ResponseMode.BEST, RepeatMode.ALL);
		annoService.setWikiDocumentPreprocessor();
		annoService.annotateContent("", "initial", null);
	}

	// results/social/withCate en pos 100 10000 1 1000000 
	public static void main(String[] args) throws Exception {
		String path = args[0];
		Language language = Language.getLanguage(args[1]); // en
		NLPModel nlpModel = NLPModel.valueOf(args[2].toUpperCase()); // pos
		int maxThreadNum = Integer.parseInt(args[3]); // 100
		interval = Integer.parseInt(args[4]); // 10000
		if (args.length >= 6)
			start = Integer.parseInt(args[5]); // 1
		else
			start = -1;
		if (args.length == 7)
			end = Integer.parseInt(args[6]); // 1000000
		else
			end = -1;
		xLiMeSocialEntityCategoryAnnotation mongoService = new xLiMeSocialEntityCategoryAnnotation(nlpModel, language);
		MongoClient mongoClient = mongoService.initMongoDB();
		DBCollection collection = mongoService.getCollection(mongoClient);

		statisticsPW = new PrintWriter(new File(path + "/statistics_social.txt"));

		DBCursor cursor = mongoService.genDBCursor(collection, language);
		cursor.addOption(com.mongodb.Bytes.QUERYOPTION_NOTIMEOUT);
		int i = 0, j = 1;
		if (start > 0)
			j = start;
		List<DBObject> objects = new ArrayList<DBObject>();
		
		long startTime = System.currentTimeMillis();
		while (cursor.hasNext()) {
			DBObject obj = cursor.next();

			i++;
			if (start > i)
				continue;

			Language tempLang = Language.getLanguage(obj.get(DB_LANG).toString());
			if (!tempLang.equals(language))
				continue;
			if (i % interval == 0) {
				objects.add(obj);
				mongoService.process(path, maxThreadNum, objects, language, j, i);
				objects.clear();
				j = i + 1;
			} else {
				objects.add(obj);
			}

			if (end > 0 && i >= end)
				break;
		}

		if (objects.size() != 0) {
			mongoService.process(path, maxThreadNum, objects, language, j, i);
		}

		long endTime = System.currentTimeMillis();
		statisticsPW.println("Total time: " + (endTime - startTime) + " ms");
		
		statisticsPW.close();
		mongoClient.close();
	}

	public void process(String path, int maxThreadNum, List<DBObject> objects, Language language, int j, int i) {
		try {
			ExecutorService executorService = Executors.newFixedThreadPool(maxThreadNum);
			Element xmlDoc = genXMLDoc(objects, executorService);
			executorService.shutdown();
			while (!executorService.isTerminated()) {
				// waiting for the terminate of the thread pool service
			}
			String filename = path + "/xlime_social_" + language.getLabel() + "_" + j + "-" + i + ".xml";
			printXML(filename, xmlDoc);
		} catch (Exception e) {
			e.printStackTrace();
		}
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

	public DBCursor genDBCursor(DBCollection collection, Language language) {
		BasicDBObject langQuery = new BasicDBObject(DB_LANG, language.toString());

		BasicDBObject fields = new BasicDBObject();
		fields.put(DB_LANG, 1);
		fields.put(DB_CONTENT, 2);
		fields.put(DB_CREATED, 3);
		fields.put(DB_PUBLISHER, 4);
		fields.put(DB_SOURCE, 5);
		fields.put(DB_SOURCETYPE, 6);
		fields.put(DB_CREATOR, 7);
		DBCursor cursor = collection.find(langQuery, fields).sort(new BasicDBObject(DB_ID, 1));

		return cursor;
	}

	@SuppressWarnings("unchecked")
	public Element genXMLDoc(List<DBObject> objects, ExecutorService executorService) throws Exception {
		Element xmlDoc = annoService.getDoc().createElement(ITEMS_TAG);
		for (DBObject obj : objects) {
			Language lang = Language.getLanguage(obj.get(DB_LANG).toString());
			if (!lang.equals(annoService.getLanguage()))
				continue;
			String id = obj.get(DB_ID).toString();
			String content = ((Map<String, String>) obj.get(DB_CONTENT)).get(DB_CONTENT_FULL);
			Map<String, String> attributes = new HashMap<String, String>();
			String created = ((Map<String, String>) obj.get(DB_CREATED)).get(DB_CREATED_FORMATTED);
			String publisher = ((Map<String, String>) obj.get(DB_PUBLISHER)).get(DB_PUBLISHER_URL);
			String source = obj.get(DB_SOURCE).toString();
			String sourceType = obj.get(DB_SOURCETYPE).toString();
			String creator = ((Map<String, String>) obj.get(DB_CREATOR)).get(DB_CREATOR_URL);
			attributes.put(DB_LANG, lang.toString());
			attributes.put(DB_CREATED, created);
			attributes.put(DB_PUBLISHER, publisher);
			attributes.put(DB_SOURCE, source);
			attributes.put(DB_SOURCETYPE, sourceType);
			attributes.put(DB_CREATOR, creator);

			Runnable annoThread = new AnnoThread(id, content, attributes, xmlDoc, annoService);
			executorService.execute(annoThread);
		}
		return xmlDoc;
	}

	public String printXML(String filename, Element xmlDoc) throws Exception {
		int itemNum = xmlDoc.getChildNodes().getLength();
		statisticsPW.println(filename + ": " + itemNum);
		PrintWriter pw = new PrintWriter(createFileWithPW(filename));
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		StringWriter writer = new StringWriter();
		StreamResult result = new StreamResult(writer);
		DOMSource dom = new DOMSource(xmlDoc);
		transformer.transform(dom, result);
		String xmlItems = writer.toString();
		pw.println(xmlItems);
		pw.close();
		return xmlItems;
	}

	private File createFileWithPW(String filename) throws FileNotFoundException {
		File file = new File(filename);
		if (!file.exists()) {
			file.getParentFile().mkdirs();
		}
		return file;
	}

}
