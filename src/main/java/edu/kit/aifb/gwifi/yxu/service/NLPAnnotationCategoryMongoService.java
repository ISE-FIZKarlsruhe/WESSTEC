package edu.kit.aifb.gwifi.yxu.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.sun.org.apache.xerces.internal.dom.DocumentImpl;

import edu.kit.aifb.gwifi.service.Service.DisambiguationModel;
import edu.kit.aifb.gwifi.service.Service.KB;
import edu.kit.aifb.gwifi.service.Service.MentionMode;
import edu.kit.aifb.gwifi.service.Service.NLPModel;
import edu.kit.aifb.gwifi.service.Service.RepeatMode;
import edu.kit.aifb.gwifi.service.Service.ResponseMode;
import edu.kit.aifb.gwifi.util.nlp.Language;

public class NLPAnnotationCategoryMongoService {

	public final static String ITEMS_TAG = "items";
	public final static String ITEM_TAG = "item";
	public final static String DB_ID_KEY = "_id";
	public final static String DB_CONTENT_KEY = "content";
	public final static String DB_FULL_CONTENT_KEY = "full";
	public final static String DB_LANG_KEY = "lang";

	private NLPAnnotationCategoryService annoService;

	public class AnnoThread implements Runnable {
		private String id;
		private String content;
		private Element xmlDoc;
		private NLPAnnotationCategoryService service;

		public AnnoThread(String id, String content, Element xmlDoc,
				NLPAnnotationCategoryService service) {
			this.id = id;
			this.content = content;
			this.xmlDoc = xmlDoc;
			this.service = service;
		}

		@Override
		public void run() {
			try {
				System.out.println(id + "    start!");
				Element ele = service.annotateContent(id, content.replace("#", " "));
				// synchronized (xmlDoc) {
				if(ele != null)
					xmlDoc.appendChild(ele);
				// }
				System.out.println(id + "    end!");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	public NLPAnnotationCategoryMongoService(NLPModel nlpModel,
			Language language) throws Exception {
		annoService = new NLPAnnotationCategoryService(
				"configs/hub-template.xml",
				"configs/wikipedia-template-en.xml",
				"configs/NLPConfig.properties", language, language,
				KB.WIKIPEDIA, nlpModel, DisambiguationModel.PAGERANK,
				MentionMode.NON_OVERLAPPED, ResponseMode.BEST, RepeatMode.ALL);
		annoService.setWikiDocumentPreprocessor();
		annoService.annotateContent("", "initial");
	}

	public NLPAnnotationCategoryService getAnnoService() {
		return this.annoService;
	}

	public static void main(String[] args) throws Exception {
		Language language = Language.getLanguage(args[0]);
		NLPModel nlpModel = NLPModel.valueOf(args[1].toUpperCase());
		int maxThreadNum = Integer.parseInt(args[2]);
		int startPos = Integer.parseInt(args[3]);
		int endPos = Integer.parseInt(args[4]);
		NLPAnnotationCategoryMongoService mongoService = new NLPAnnotationCategoryMongoService(
				nlpModel, language);
		MongoClient mongoClient = mongoService.initMongoDB();
		DBCollection collection = mongoService.getCollection(mongoClient);
		DBCursor cursor = mongoService.genDBCursor(collection, language,
				startPos, endPos);
		try {
			ExecutorService executorService = Executors
					.newFixedThreadPool(maxThreadNum);
			Element xmlDoc = mongoService.genXMLDoc(cursor,
					mongoService.getAnnoService(), executorService);
			executorService.shutdown();
			while (!executorService.isTerminated()) {
				// waiting for the terminate of the thread pool service
			}
			mongoService.printXML(nlpModel, language, xmlDoc);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			mongoClient.close();
		}
	}

	// connect to db server
	public MongoClient initMongoDB() throws Exception {
		MongoClient mongoClient = new MongoClient(new MongoClientURI(
				"mongodb://aifb-ls3-remus.aifb.kit.edu:19015"));
		return mongoClient;
	}

	public DBCollection getCollection(MongoClient mongoClient) {
		DB db = mongoClient.getDB("brexit-xlimeress");
		System.out.println("Connect to database successfully!");
		DBCollection collection = db.getCollection("MicroPostBean");
		System.out.println("Collection selected successfully!");
		return collection;
	}

	public DBCursor genDBCursor(DBCollection collection, Language language,
			int startPos, int endPos) {
		int start = startPos - 1;
		int end = endPos + 1;
		BasicDBObject langQuery = new BasicDBObject(DB_LANG_KEY,
				language.toString());
		BasicDBObject fields = new BasicDBObject();
		fields.put(DB_CONTENT_KEY, 1);
		fields.put(DB_LANG_KEY, 2);
		DBCursor cursor = collection.find(langQuery, fields);
		if (start < 0)
			start = 0;
		cursor.skip(start);
		if (end > start)
			cursor.maxScan(end);
		return cursor;
	}

	@SuppressWarnings("unchecked")
	public Element genXMLDoc(DBCursor cursor,
			NLPAnnotationCategoryService service,
			ExecutorService executorService) throws Exception {
		Element xmlDoc = service.getDoc().createElement(ITEMS_TAG);
		DBObject obj;
		String tempId;
		String tempContent;
		Language tempLang;
		while (cursor.hasNext()) {
			obj = cursor.next();
			tempLang = Language.getLanguage(obj.get(DB_LANG_KEY).toString());
			if (!tempLang.equals(service.getLanguage()))
				continue;
			tempId = obj.get(DB_ID_KEY).toString();
			tempContent = ((Map<String, String>) obj.get(DB_CONTENT_KEY))
					.get(DB_FULL_CONTENT_KEY);
			Runnable annoThread = new AnnoThread(tempId, tempContent, xmlDoc,
					service);
			executorService.execute(annoThread);
		}
		return xmlDoc;
	}

	public String printXML(NLPModel nlpModel, Language language, Element xmlDoc)
			throws Exception {
		int itemNum = xmlDoc.getChildNodes().getLength();
		System.out.println(itemNum);
		String filename = "results/xml_" + language.getLabel() + "_"
				+ nlpModel.name() + "_Items.xml";
		PrintWriter pw = new PrintWriter(createFileWithPW(filename));
		Transformer transformer = TransformerFactory.newInstance()
				.newTransformer();
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(
				"{http://xml.apache.org/xslt}indent-amount", "2");
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
