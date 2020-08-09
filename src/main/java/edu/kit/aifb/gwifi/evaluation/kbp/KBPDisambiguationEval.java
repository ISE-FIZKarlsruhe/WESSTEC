package edu.kit.aifb.gwifi.evaluation.kbp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.dom4j.DocumentException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;

import edu.kit.aifb.gwifi.service.NLPDisambiguationService;
import edu.kit.aifb.gwifi.service.Service.DisambiguationModel;
import edu.kit.aifb.gwifi.service.Service.KB;
import edu.kit.aifb.gwifi.service.Service.MentionMode;
import edu.kit.aifb.gwifi.service.Service.NLPModel;
import edu.kit.aifb.gwifi.service.Service.RepeatMode;
import edu.kit.aifb.gwifi.service.Service.ResponseMode;
import edu.kit.aifb.gwifi.util.Position;
import edu.kit.aifb.gwifi.util.nlp.Language;

public class KBPDisambiguationEval {
	
	public static final String KBP_DB = "KBP";
	public static final String KBP_KB_COLL = "kbp_kb";
	public static final String EVAL_SOURCE_COLL = "eval_source_2013";
	public static final String EVAL_QUERIES_COLL = "eval_queries_2013";
	public static final String EVAL_LINKS_COLL = "eval_links_2013";
	public static final String EVAL_ANSWERS_COLL = "eval_answers_2013_ner";
	
	public static final String NLP_MODEL = "ner";
	
	private NLPDisambiguationService zhservice;
	private NLPDisambiguationService enservice;

	private Mongo mongo;
	private DB db;
	private DBCollection eval_source;
	private DBCollection kbp_kb;
	private DBCollection eval_answers;
	private DBCollection eval_queries;
	private DBCollection eval_links;
	public DBCollection newDictionary;
	public DBCollection baidu;

	private String nilInkbp = null;
	private static int nrOfNILInkbp = 1;

	private BufferedWriter prAnswers;
	private BufferedWriter prgwifiResult;
	private BufferedWriter prdocTypeAll;
	private BufferedWriter prdocTypeDF;
	private BufferedWriter prdocTypeNW;
	private BufferedWriter prdocTypeWB;

	private BufferedWriter prDFRight;
	private BufferedWriter prDFGeSeWrong;
	private BufferedWriter prDFGnSeWrong;
	private BufferedWriter prDFGeSnWrong;

	private BufferedWriter prNWRight;
	private BufferedWriter prNWGeSeWrong;
	private BufferedWriter prNWGnSeWrong;
	private BufferedWriter prNWGeSnWrong;

	private BufferedWriter prWBRight;
	private BufferedWriter prWBGeSeWrong;
	private BufferedWriter prWBGnSeWrong;
	private BufferedWriter prWBGeSnWrong;

	public KBPDisambiguationEval(String configFolder, String outputFolder) throws Exception {
		zhgwifiInit(configFolder);
		engwifiInit(configFolder);
		mongoInit();

		String answers = outputFolder + NLP_MODEL + "/answers.tab";
		String gwifiResults = outputFolder + NLP_MODEL + "/gwifiResults.txt";
		String docTypeAll = outputFolder + NLP_MODEL + "/docTypeAll.txt";
		String docTypeDF = outputFolder + NLP_MODEL + "/docTypeDF.txt";
		String docTypeNW = outputFolder + NLP_MODEL + "/docTypeNW.txt";
		String docTypeWB = outputFolder + NLP_MODEL + "/docTypeWB.txt";

		File answerFile = new File(answers);
		if (!answerFile.exists())
			answerFile.getParentFile().mkdirs();
		File gwifiResultFile = new File(gwifiResults);
		if (!gwifiResultFile.exists())
			gwifiResultFile.getParentFile().mkdirs();
		File docTypeAllFile = new File(docTypeAll);
		if (!docTypeAllFile.exists())
			docTypeAllFile.getParentFile().mkdirs();
		File docTypeDFFile = new File(docTypeDF);
		if (!docTypeDFFile.exists())
			docTypeDFFile.getParentFile().mkdirs();
		File docTypeNWFile = new File(docTypeNW);
		if (!docTypeNWFile.exists())
			docTypeNWFile.getParentFile().mkdirs();
		File docTypeWBFile = new File(docTypeWB);
		if (!docTypeWBFile.exists())
			docTypeWBFile.getParentFile().mkdirs();

		prAnswers = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(answerFile), "UTF8"));
		prgwifiResult = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(gwifiResultFile), "UTF8"));
		prdocTypeAll = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(docTypeAllFile), "UTF8"));
		prdocTypeDF = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(docTypeDFFile), "UTF8"));
		prdocTypeNW = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(docTypeNWFile), "UTF8"));
		prdocTypeWB = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(docTypeWBFile), "UTF8"));

		String DFRight = outputFolder + NLP_MODEL + "/DFRight.txt";
		String DFGeSeWrong = outputFolder + NLP_MODEL + "/DFGeSeWrong.txt";
		String DFGnSeWrong = outputFolder + NLP_MODEL + "/DFGnSeWrong.txt";
		String DFGeSnWrong = outputFolder + NLP_MODEL + "/DFGeSnWrong.txt";
		String NWRight = outputFolder + NLP_MODEL + "/NWRight.txt";
		String NWGeSeWrong = outputFolder + NLP_MODEL + "/NWGeSeWrong.txt";
		String NWGnSeWrong = outputFolder + NLP_MODEL + "/NWGnSeWrong.txt";
		String NWGeSnWrong = outputFolder + NLP_MODEL + "/NWGeSnWrong.txt";
		String WBRight = outputFolder + NLP_MODEL + "/WBRight.txt";
		String WBGeSeWrong = outputFolder + NLP_MODEL + "/WBGeSeWrong.txt";
		String WBGnSeWrong = outputFolder + NLP_MODEL + "/WBGnSeWrong.txt";
		String WBGeSnWrong = outputFolder + NLP_MODEL + "/WBGeSnWrong.txt";

		File prDFRightFile = new File(DFRight);
		if (!prDFRightFile.exists())
			prDFRightFile.getParentFile().mkdirs();
		File prDFGeSeWrongFile = new File(DFGeSeWrong);
		if (!prDFGeSeWrongFile.exists())
			prDFGeSeWrongFile.getParentFile().mkdirs();
		File prDFGnSeWrongFile = new File(DFGnSeWrong);
		if (!prDFGnSeWrongFile.exists())
			prDFGnSeWrongFile.getParentFile().mkdirs();
		File prDFGeSnWrongFile = new File(DFGeSnWrong);
		if (!prDFGeSnWrongFile.exists())
			prDFGeSnWrongFile.getParentFile().mkdirs();
		File prNWRightFile = new File(NWRight);
		if (!prNWRightFile.exists())
			prNWRightFile.getParentFile().mkdirs();
		File prNWGeSeWrongFile = new File(NWGeSeWrong);
		if (!prNWGeSeWrongFile.exists())
			prNWGeSeWrongFile.getParentFile().mkdirs();
		File prNWGnSeWrongFile = new File(NWGnSeWrong);
		if (!prNWGnSeWrongFile.exists())
			prNWGnSeWrongFile.getParentFile().mkdirs();
		File prNWGeSnWrongFile = new File(NWGeSnWrong);
		if (!prNWGeSnWrongFile.exists())
			prNWGeSnWrongFile.getParentFile().mkdirs();
		File prWBRightFile = new File(WBRight);
		if (!prWBRightFile.exists())
			prWBRightFile.getParentFile().mkdirs();
		File prWBGeSeWrongFile = new File(WBGeSeWrong);
		if (!prWBGeSeWrongFile.exists())
			prWBGeSeWrongFile.getParentFile().mkdirs();
		File prWBGnSeWrongFile = new File(WBGnSeWrong);
		if (!prWBGnSeWrongFile.exists())
			prWBGnSeWrongFile.getParentFile().mkdirs();
		File prWBGeSnWrongFile = new File(WBGeSnWrong);
		if (!prWBGeSnWrongFile.exists())
			prWBGeSnWrongFile.getParentFile().mkdirs();

		prDFRight = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(prDFRightFile), "UTF8"));
		prDFGeSeWrong = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(prDFGeSeWrongFile), "UTF8"));
		prDFGnSeWrong = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(prDFGnSeWrongFile), "UTF8"));
		prDFGeSnWrong = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(prDFGeSnWrongFile), "UTF8"));
		prNWRight = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(prNWRightFile), "UTF8"));
		prNWGeSeWrong = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(prNWGeSeWrongFile), "UTF8"));
		prNWGnSeWrong = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(prNWGnSeWrongFile), "UTF8"));
		prNWGeSnWrong = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(prNWGeSnWrongFile), "UTF8"));
		prWBRight = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(prWBRightFile), "UTF8"));
		prWBGeSeWrong = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(prWBGeSeWrongFile), "UTF8"));
		prWBGnSeWrong = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(prWBGnSeWrongFile), "UTF8"));
		prWBGeSnWrong = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(prWBGeSnWrongFile), "UTF8"));

	}

	/**
	 * initial the Gwifi service
	 * 
	 * @param folder
	 * @throws Exception
	 */
	private void zhgwifiInit(String folder) throws Exception {
		zhservice = new NLPDisambiguationService(folder + "hub-template.xml", folder + "wikipedia-template-zh.xml",
				folder + "NLPConfig.properties", Language.ZH, Language.EN, KB.WIKIPEDIA, NLPModel.NER,
				DisambiguationModel.PAGERANK, MentionMode.NON_OVERLAPPED, ResponseMode.BEST, RepeatMode.FIRST);
	}

	/**
	 * initial the Gwifi service
	 * 
	 * @param folder
	 * @throws Exception
	 */
	private void engwifiInit(String folder) throws Exception {
		enservice = new NLPDisambiguationService(folder + "hub-template.xml", folder + "wikipedia-template-en.xml",
				folder + "NLPConfig.properties", Language.EN, Language.EN, KB.WIKIPEDIA, NLPModel.NER,
				DisambiguationModel.PAGERANK, MentionMode.NON_OVERLAPPED, ResponseMode.BEST, RepeatMode.FIRST);
	}

	/**
	 * initial the mongoDB
	 */
	@SuppressWarnings("deprecation")
	private void mongoInit() {
		try {
			mongo = new Mongo("aifb-ls3-maia.aifb.kit.edu", 19005);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		db = mongo.getDB(KBP_DB);
		kbp_kb = db.getCollection(KBP_KB_COLL);
		eval_source = db.getCollection(EVAL_SOURCE_COLL);
		eval_queries = db.getCollection(EVAL_QUERIES_COLL);
		eval_links = db.getCollection(EVAL_LINKS_COLL);
		eval_answers = db.getCollection(EVAL_ANSWERS_COLL);
		eval_answers.drop();

		newDictionary = db.getCollection("DictionaryFrom_cedict_ts");
		baidu = db.getCollection("DictionaryFrom_zh_en_links");

	}

	/**
	 * get data from MongoDB collection eval_source_2014 and put them into Gwifi service and save the result in the file
	 * "kbp/.../results.txt"
	 * 
	 * @throws Exception
	 */
	private void getDataFromMongodb() throws Exception {

		DBCursor cur = eval_source.find();
		cur.addOption(com.mongodb.Bytes.QUERYOPTION_NOTIMEOUT);
		System.out.println("the number of documents: " + cur.size());

		int i = 0;
		while (cur.hasNext()) {
			i++;
			DBObject curobj = cur.next();
			String doc_id = curobj.get("doc_id").toString();
			System.out.println(i + ". the doc id is " + doc_id);

			String doc_text = curobj.get("doc_text").toString();
			HashSet<Position> positions = new HashSet<Position>();
			BasicDBObject query = new BasicDBObject();
			query.put("doc_id", doc_id);
			DBCursor curquery = eval_queries.find(query);
			while (curquery.hasNext()) {
				DBObject queryobj = curquery.next();
				String queryName = queryobj.get("query_name").toString();
				int start = doc_text.indexOf(queryName);
				int end = start + queryName.length();
				positions.add(new Position(start, end));
			}

			String gwifiResult;
			if (isChinese(doc_id) == true) {
				gwifiResult = zhcallGwifiService(doc_text, positions);
			} else {
				gwifiResult = encallGwifiService(doc_text, positions);
			}

			System.out.println(i + "\t" + "the id of this tex is:" + "\t" + doc_id);
			System.out.println();
			System.out.println(gwifiResult);
			System.out.println();

			prgwifiResult.write(i + "\t" + "the id of this tex is:" + "\t" + doc_id + "\n");
			prgwifiResult.write(gwifiResult + "\n");

			ArrayList<EntityBean> entitybeans = this.getEntInfoFromXml(gwifiResult);

			for (Iterator<EntityBean> iter = entitybeans.iterator(); iter.hasNext();) {
				EntityBean entityBean = iter.next();
				String entityname = entityBean.getEntityname();
				String mentionLabel = entityBean.getMentionLabel();
				String beg = entityBean.getBeg();
				String end = entityBean.getEnd();
				String type = entityBean.getType();
				String entityid = this.getEntidFromkbpkb(entityname);

				if (type.equals("ORG")) {
					type = "ORG";
				} else if (type.equals("LOC")) {
					type = "GPE";
				} else if (type.equals("PERSON")) {
					type = "PER";
				} else if (type.equals("MISC")) {
					type = "MISC"; // miscellaneous
				} else {
					type = "MISC"; // miscellaneous
				}

				this.saveAnswers2Mongodb(doc_id, entityname, entityid, mentionLabel, beg, end, type);
			}
		}
		prgwifiResult.close();
	}

	public static boolean isChinese(String docid) {
		if (docid.contains("cmn")) {
			return true;
		} else if (docid.contains("CMN")) {
			return true;
		} else
			return false;
	}

	/**
	 * call Gwifi service
	 * 
	 * @param text
	 * @return
	 * @throws Exception
	 */
	public String zhcallGwifiService(String text, Set<Position> positions) throws Exception {

		return zhservice.disambiguate(text, positions, null);

	}

	/**
	 * call Gwifi service
	 * 
	 * @param text
	 * @return
	 * @throws Exception
	 */
	public String encallGwifiService(String text, Set<Position> positions) throws Exception {

		return enservice.disambiguate(text, positions, null);

	}

	/**
	 * get EntityID from MongoDB colletion kbp_kb
	 * 
	 * @param displayName
	 * @return
	 */
	private String getEntidFromkbpkb(String displayName) {
		BasicDBObject query = new BasicDBObject();
		query.put("entity_name", displayName);
		String entityid;

		if (null != kbp_kb.findOne(query)) {
			entityid = kbp_kb.findOne(query).get("entitiy_id").toString();
			return entityid;
		} else {
			if (null != eval_answers.findOne(query)) {
				entityid = eval_answers.findOne(query).get("entity_id").toString();
				return entityid;
			} else {
				java.text.DecimalFormat format = new java.text.DecimalFormat("0000000");
				nilInkbp = "NILInkbp" + format.format(nrOfNILInkbp);
				nrOfNILInkbp++;
				return nilInkbp;
			}
		}
	}

	private String getEntNameFromkbpkb(String entityId) {
		BasicDBObject query = new BasicDBObject();
		query.put("entitiy_id", entityId);
		DBObject curobjEntityName = kbp_kb.findOne(query);
		String entityName;

		if (entityId.contains("NIL")) {
			entityName = entityId;
		} else {
			entityName = curobjEntityName.get("entity_name").toString();
		}
		return entityName;
	}

	/**
	 * read file from result of gwifi
	 * 
	 * @param filename
	 * @return
	 * @throws IOException
	 */
	public String readXmlFile(String filename) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(new File(filename)));
		StringBuffer sb = new StringBuffer();
		String line;
		while ((line = br.readLine()) != null) {
			sb.append(line).append("\n");
		}
		br.close();
		// String xml = sb.toString().trim();
		return sb.toString().trim();
	}

	/**
	 * get Entities from xml-file(result of gwifi).
	 * 
	 * @param xmlstring
	 * @return
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws DocumentException
	 */
	public ArrayList<EntityBean> getEntInfoFromXml(String xmlstring)
			throws IOException, SAXException, ParserConfigurationException, DocumentException {
		ArrayList<EntityBean> entities = new ArrayList<EntityBean>();

		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		Document inputDoc = docBuilder.parse(new InputSource(new StringReader(xmlstring)));
		NodeList annoNodeList = inputDoc.getElementsByTagName("Annotation");
		for (int i = 0; i < annoNodeList.getLength(); i++) {
			Element ele = (Element) annoNodeList.item(i);
			String entityname = ele.getAttribute("displayName");

			NodeList annoNodeListSon = inputDoc.getElementsByTagName("mention");
			Element ele2 = (Element) annoNodeListSon.item(i);

			String mentionLabel = ele2.getAttribute("label");
			String start = ele2.getAttribute("position");
			String mentionLength = ele2.getAttribute("length");
			String type = ele2.getAttribute("type");
			String beg = new Integer(Integer.parseInt(start) + 1).toString();
			String end = new Integer(Integer.parseInt(start) + Integer.parseInt(mentionLength)).toString();

			entities.add(new EntityBean(entityname, mentionLabel, beg, end, type));
		}
		return entities;
	}

	/**
	 * save the result of the test in a MongoDB collection: eval_answers_2014
	 * 
	 * @param doc_id
	 * @param entiyname
	 * @param entity_id
	 * @param mentionLabel
	 */
	public void saveAnswers2Mongodb(String doc_id, String entiyname, String entity_id, String mentionLabel, String beg,
			String end, String type) {
		DBObject insertData;
		insertData = new BasicDBObject();
		insertData.put("doc_id", doc_id);
		insertData.put("entity_name", entiyname);
		insertData.put("entity_id", entity_id);
		insertData.put("query_name", mentionLabel);
		insertData.put("beg", beg);
		insertData.put("end", end);
		insertData.put("entity_type", type);
		eval_answers.insert(insertData);

	}

	/**
	 * make a standard file of system output. It's also a answer list for queries. compare the result of Goldfile and
	 * system output.
	 * 
	 * @throws Exception
	 */
	private void getAnswers() throws Exception {

		DBCursor curqueries = eval_queries.find();
		prAnswers.write("query_id" + "\t" + "entity_id" + "\t" + "entity_type" + "\n");

		// the result of sorted by doc_type
		prdocTypeAll.write("query_id" + "\t" + "query_name" + "\t" + "Gold_file_answers" + "\t" + "System_out_answers"
				+ "\t" + "doc_id" + "\n");
		prdocTypeDF.write("query_id" + "\t" + "query_name" + "\t" + "Gold_file_answers" + "\t" + "System_out_answers"
				+ "\t" + "doc_id" + "\n");
		prdocTypeNW.write("query_id" + "\t" + "query_name" + "\t" + "Gold_file_answers" + "\t" + "System_out_answers"
				+ "\t" + "doc_id" + "\n");
		prdocTypeWB.write("query_id" + "\t" + "query_name" + "\t" + "Gold_file_answers" + "\t" + "System_out_answers"
				+ "\t" + "doc_id" + "\n");

		// the result of doc type of DF
		prDFRight.write("query_id" + "\t" + "query_name" + "\t" + "Gold_file_answers" + "\t" + "System_out_answers"
				+ "\t" + "doc_id" + "\n");
		prDFGeSeWrong.write("query_id" + "\t" + "query_name" + "\t" + "Gold_file_answers" + "\t" + "System_out_answers"
				+ "\t" + "doc_id" + "\n");
		prDFGnSeWrong.write("query_id" + "\t" + "query_name" + "\t" + "Gold_file_answers" + "\t" + "System_out_answers"
				+ "\t" + "doc_id" + "\n");
		prDFGeSnWrong.write("query_id" + "\t" + "query_name" + "\t" + "Gold_file_answers" + "\t" + "System_out_answers"
				+ "\t" + "doc_id" + "\n");

		// the result of doc type of NW
		prNWRight.write("query_id" + "\t" + "query_name" + "\t" + "Gold_file_answers" + "\t" + "System_out_answers"
				+ "\t" + "doc_id" + "\n");
		prNWGeSeWrong.write("query_id" + "\t" + "query_name" + "\t" + "Gold_file_answers" + "\t" + "System_out_answers"
				+ "\t" + "doc_id" + "\n");
		prNWGnSeWrong.write("query_id" + "\t" + "query_name" + "\t" + "Gold_file_answers" + "\t" + "System_out_answers"
				+ "\t" + "doc_id" + "\n");
		prNWGeSnWrong.write("query_id" + "\t" + "query_name" + "\t" + "Gold_file_answers" + "\t" + "System_out_answers"
				+ "\t" + "doc_id" + "\n");

		// the result of doc type of WB
		prWBRight.write("query_id" + "\t" + "query_name" + "\t" + "Gold_file_answers" + "\t" + "System_out_answers"
				+ "\t" + "doc_id" + "\n");
		prWBGeSeWrong.write("query_id" + "\t" + "query_name" + "\t" + "Gold_file_answers" + "\t" + "System_out_answers"
				+ "\t" + "doc_id" + "\n");
		prWBGnSeWrong.write("query_id" + "\t" + "query_name" + "\t" + "Gold_file_answers" + "\t" + "System_out_answers"
				+ "\t" + "doc_id" + "\n");
		prWBGeSnWrong.write("query_id" + "\t" + "query_name" + "\t" + "Gold_file_answers" + "\t" + "System_out_answers"
				+ "\t" + "doc_id" + "\n");

		HashMap<String, String> mapFoundNIL = new HashMap<String, String>();
		HashMap<String, String> mapNotFound = new HashMap<String, String>();

		int i = 1;

		while (curqueries.hasNext()) {

			DBObject curobj = curqueries.next();
			String queryid = curobj.get("query_id").toString();
			String docid = curobj.get("doc_id").toString();
			String queryname = curobj.get("query_name").toString();

			String goldfileEntid = getGoldfileEntid(queryid); // entityid in goldfile
			String goldfileEntType = getGoldfileEntType(queryid);

			String goldfileEntName = getEntNameFromkbpkb(goldfileEntid); // entity_name in goldfile。

			BasicDBObject query = new BasicDBObject();
			query.append("query_name", queryname).append("doc_id", docid);

			String sysoutEntid;
			String sysoutEntName;
			String sysoutEntType = "MISC";

			if (null != eval_answers.findOne(query)) {
				sysoutEntid = eval_answers.findOne(query).get("entity_id").toString();
				sysoutEntName = eval_answers.findOne(query).get("entity_name").toString();
				sysoutEntType = eval_answers.findOne(query).get("entity_type").toString();
				if (sysoutEntid.contains("NIL") == true) {
					String nilInkbp = sysoutEntid;
					if (mapFoundNIL.get(nilInkbp) == null) {
						java.text.DecimalFormat format = new java.text.DecimalFormat("0000");
						sysoutEntid = "NIL" + format.format(i);
						sysoutEntName = sysoutEntid;
						i++;
						mapFoundNIL.put(nilInkbp, sysoutEntid);
					} else {
						sysoutEntid = mapFoundNIL.get(nilInkbp);
						sysoutEntName = sysoutEntid;
					}
				}

			} else {
				if (mapNotFound.get(queryname) == null) {
					java.text.DecimalFormat format = new java.text.DecimalFormat("0000");
					sysoutEntid = "NIL" + format.format(1);
					sysoutEntName = sysoutEntid;
					i++;
					mapNotFound.put(queryname, sysoutEntid);
				} else {
					sysoutEntid = mapNotFound.get(queryname);
					sysoutEntName = sysoutEntid;
				}
			}

			String doctype = getDocType(docid);

			switch (doctype) {
			case "DF": {
				if (goldfileEntid.equals(sysoutEntid)
						|| ((true == goldfileEntid.contains("NIL")) && (true == sysoutEntid.contains("NIL")))) {
					prDFRight.write(queryid + "\t" + queryname + "\t" + goldfileEntName + "\t" + sysoutEntName + "\t"
							+ docid + "\n");
				} else if (!goldfileEntid.equals(sysoutEntid) && (false == goldfileEntid.contains("NIL"))
						&& (false == sysoutEntid.contains("NIL"))) {
					prDFGeSeWrong.write(queryid + "\t" + queryname + "\t" + goldfileEntName + "\t" + sysoutEntName
							+ "\t" + docid + "\n");
				} else if ((true == goldfileEntid.contains("NIL")) && (false == sysoutEntid.contains("NIL"))) {
					prDFGnSeWrong.write(queryid + "\t" + queryname + "\t" + goldfileEntName + "\t" + sysoutEntName
							+ "\t" + docid + "\n");
				} else if ((false == goldfileEntid.contains("NIL")) && (true == sysoutEntid.contains("NIL"))) {
					prDFGeSnWrong.write(queryid + "\t" + queryname + "\t" + goldfileEntName + "\t" + sysoutEntName
							+ "\t" + docid + "\n");
				}

				prdocTypeDF.write(queryid + "\t" + queryname + "\t" + goldfileEntName + "\t" + sysoutEntName + "\t"
						+ docid + "\n");

				break;
			}

			case "NW": {
				if (goldfileEntid.equals(sysoutEntid)
						|| ((true == goldfileEntid.contains("NIL")) && (true == sysoutEntid.contains("NIL")))) {
					prNWRight.write(queryid + "\t" + queryname + "\t" + goldfileEntName + "\t" + sysoutEntName + "\t"
							+ docid + "\n");
				} else if (!goldfileEntid.equals(sysoutEntid) && (false == goldfileEntid.contains("NIL"))
						&& (false == sysoutEntid.contains("NIL"))) {
					prNWGeSeWrong.write(queryid + "\t" + queryname + "\t" + goldfileEntName + "\t" + sysoutEntName
							+ "\t" + docid + "\n");
				} else if ((true == goldfileEntid.contains("NIL")) && (false == sysoutEntid.contains("NIL"))) {
					prNWGnSeWrong.write(queryid + "\t" + queryname + "\t" + goldfileEntName + "\t" + sysoutEntName
							+ "\t" + docid + "\n");
				} else if ((false == goldfileEntid.contains("NIL")) && (true == sysoutEntid.contains("NIL"))) {
					prNWGeSnWrong.write(queryid + "\t" + queryname + "\t" + goldfileEntName + "\t" + sysoutEntName
							+ "\t" + docid + "\n");
				}

				prdocTypeNW.write(queryid + "\t" + queryname + "\t" + goldfileEntName + "\t" + sysoutEntName + "\t"
						+ docid + "\n");

				break;
			}

			case "WB": {
				if (goldfileEntid.equals(sysoutEntid)
						|| ((true == goldfileEntid.contains("NIL")) && (true == sysoutEntid.contains("NIL")))) {
					prWBRight.write(queryid + "\t" + queryname + "\t" + goldfileEntName + "\t" + sysoutEntName + "\t"
							+ docid + "\n");
				} else if (!goldfileEntid.equals(sysoutEntid) && (false == goldfileEntid.contains("NIL"))
						&& (false == sysoutEntid.contains("NIL"))) {
					prWBGeSeWrong.write(queryid + "\t" + queryname + "\t" + goldfileEntName + "\t" + sysoutEntName
							+ "\t" + docid + "\n");
				} else if ((true == goldfileEntid.contains("NIL")) && (false == sysoutEntid.contains("NIL"))) {
					prWBGnSeWrong.write(queryid + "\t" + queryname + "\t" + goldfileEntName + "\t" + sysoutEntName
							+ "\t" + docid + "\n");
				} else if ((false == goldfileEntid.contains("NIL")) && (true == sysoutEntid.contains("NIL"))) {
					prWBGeSnWrong.write(queryid + "\t" + queryname + "\t" + goldfileEntName + "\t" + sysoutEntName
							+ "\t" + docid + "\n");
				}

				prdocTypeWB.write(queryid + "\t" + queryname + "\t" + goldfileEntName + "\t" + sysoutEntName + "\t"
						+ docid + "\n");
				break;
			}

			default:
				break;

			}

			prAnswers.write(queryid + "\t" + sysoutEntid + "\t" + sysoutEntType + "\n");

			prdocTypeAll.write(
					queryid + "\t" + queryname + "\t" + goldfileEntName + "\t" + sysoutEntName + "\t" + docid + "\n");
		}

		prAnswers.close();

		prdocTypeAll.close();
		prdocTypeDF.close();
		prdocTypeNW.close();
		prdocTypeWB.close();

		prDFRight.close();
		prDFGeSeWrong.close();
		prDFGnSeWrong.close();
		prDFGeSnWrong.close();

		prNWRight.close();
		prNWGeSeWrong.close();
		prNWGnSeWrong.close();
		prNWGeSnWrong.close();

		prWBRight.close();
		prWBGeSeWrong.close();
		prWBGnSeWrong.close();
		prWBGeSnWrong.close();

		System.out.println("Done!");
	}

	/**
	 * get entityid from GoldFile with queryid.
	 * 
	 * @param queryid
	 * @return entityid in GoldFile
	 */
	private String getGoldfileEntid(String queryid) {
		BasicDBObject query = new BasicDBObject();
		query.put("query_id", queryid);
		DBObject curobjid = null;
		if (null != eval_links.findOne(query)) {
			curobjid = eval_links.findOne(query);
		} else {
			return "not found";
		}
		String entityid = curobjid.get("entitiy_id").toString();
		return entityid;
	}

	private String getGoldfileEntType(String queryid) {
		BasicDBObject query = new BasicDBObject();
		query.put("query_id", queryid);
		DBObject curobjid = null;
		if (null != eval_links.findOne(query)) {
			curobjid = eval_links.findOne(query);
		} else {
			return "not found";
		}
		String entityType = curobjid.get("entity_type").toString();
		return entityType;
	}

	public String getDocType(String docid) {
		BasicDBObject query = new BasicDBObject();
		query.put("doc_id", docid);
		DBObject curobjid = null;
		if (null != eval_source.findOne(query)) {
			curobjid = eval_source.findOne(query);
		} else {
			return "not found";
		}
		String doctype = curobjid.get("doc_type").toString();
		return doctype;
	}

//	public static void main(String[] args) throws Exception {
//		KBPDisambiguationEval s = new KBPDisambiguationEval(args[0], args[1]);
//		s.getDataFromMongodb();
//		s.getAnswers();
//
//	}
	
	// for testing
	public static void main(String[] args) throws Exception {
		KBPDisambiguationEval s = new KBPDisambiguationEval(args[0], args[1]);
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in,  "UTF-8"));

		while (true) {
			System.out.println("\nEnter doc_id: ");
			String doc_id = in.readLine();
			
			s.test(doc_id);
		}	
	}
	
	private void test(String doc_id) throws Exception {
		BasicDBObject query = new BasicDBObject();
		query.put("doc_id", doc_id);
		DBObject curobj = eval_source.findOne(query);
		
		if(curobj == null)
			return;

		String doc_text = curobj.get("doc_text").toString();
		HashSet<Position> positions = new HashSet<Position>();
		query = new BasicDBObject();
		query.put("doc_id", doc_id);
		DBCursor curquery = eval_queries.find(query);
		while (curquery.hasNext()) {
			DBObject queryobj = curquery.next();
			String queryName = queryobj.get("query_name").toString();
			int start = doc_text.indexOf(queryName);
			int end = start + queryName.length();
			positions.add(new Position(start, end));
		}

		String gwifiResult;
		if (isChinese(doc_id) == true) {
			gwifiResult = zhcallGwifiService(doc_text, positions);
		} else {
			gwifiResult = encallGwifiService(doc_text, positions);
		}

		System.out.println(gwifiResult);
		System.out.println();
	}
}