package edu.kit.aifb.gwifi.mingyuzuo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.dom4j.DocumentException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;

import edu.kit.aifb.gwifi.evaluation.kbp.EntityBean;
import edu.kit.aifb.gwifi.service.NLPAnnotationService;
import edu.kit.aifb.gwifi.service.Service.DisambiguationModel;
import edu.kit.aifb.gwifi.service.Service.KB;
import edu.kit.aifb.gwifi.service.Service.MentionMode;
import edu.kit.aifb.gwifi.service.Service.NLPModel;
import edu.kit.aifb.gwifi.service.Service.RepeatMode;
import edu.kit.aifb.gwifi.service.Service.ResponseMode;
import edu.kit.aifb.gwifi.util.nlp.Language;

public class Source2014
{
	private NLPAnnotationService zhservice;
	private NLPAnnotationService enservice;

	private Mongo mongo;
	private DB db;
	private DBCollection eval_source_2014;
	private DBCollection getentityid_kbp_kb;
	private DBCollection eval_answers_2014;
	private DBCollection getqueries;
	private DBCollection goldfileEntid;
	public DBCollection newDictionary;
	public DBCollection baidu;
	
	private String nilInkbp = null;
	private static int nrOfNILInkbp = 1;

	private PrintWriter prAnswers;
	private PrintWriter prXML;
	private PrintWriter prgwifiResult;
	private PrintWriter prdocTypeAll;
	private PrintWriter prdocTypeDF;
	private PrintWriter prdocTypeNW;
	private PrintWriter prdocTypeWB;
	private PrintWriter prqueryByDF;
	private PrintWriter prqueryByNW;
	private PrintWriter prqueryByWB;
	private PrintWriter prentityNILvsE;

	private PrintWriter prDFRight;
	private PrintWriter prDFGeSeWrong;
	private PrintWriter prDFGnSeWrong;
	private PrintWriter prDFGeSnWrong;

	private PrintWriter prNWRight;
	private PrintWriter prNWGeSeWrong;
	private PrintWriter prNWGnSeWrong;
	private PrintWriter prNWGeSnWrong;
	

	// private static String chinesePattern = "CMN";
	// private static Pattern pattern =
	// Pattern.compile("CMN*",Pattern.CASE_INSENSITIVE);

	public Source2014(String folder) throws Exception
	{
		zhgwifiInit(folder);
		engwifiInit(folder);
		mongoInit();
		// pattern = Pattern.compile(chinesePattern);

		String answers = folder + "zmy/answers.tab";
		String xml = folder + "zmy/queries.xml";
		String gwifiResults = folder + "zmy/gwifiResults.txt";
		String docTypeAll = folder + "zmy/docTypeAll.txt";
		String docTypeDF = folder + "zmy/docTypeDF.txt";
		String docTypeNW = folder + "zmy/docTypeNW.txt";
		String docTypeWB = folder + "zmy/docTypeWB.txt";
		String queryByDF = folder + "zmy/queryByDF.txt";
		String queryByNW = folder + "zmy/queryByNW.txt";
		String queryByWB = folder + "zmy/queryByWB.txt";
		String entityNILvsE = folder + "zmy/entityNILvsE.txt";

		File answerFile = new File(answers);
		if (!answerFile.exists())
			answerFile.getParentFile().mkdirs();
		File xmlFile = new File(xml);
		if (!xmlFile.exists())
			xmlFile.getParentFile().mkdirs();
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
		File queryByDFFile = new File(queryByDF);
		if (!queryByDFFile.exists())
			queryByDFFile.getParentFile().mkdirs();
		File queryByNWFile = new File(queryByNW);
		if (!queryByNWFile.exists())
			queryByNWFile.getParentFile().mkdirs();
		File queryByWBFile = new File(queryByWB);
		if (!queryByWBFile.exists())
			queryByWBFile.getParentFile().mkdirs();
		File entityNILvsEFIle = new File(entityNILvsE);
		if (!entityNILvsEFIle.exists())
			entityNILvsEFIle.getParentFile().mkdirs();

		prAnswers = new PrintWriter(answerFile);
		prXML = new PrintWriter(xmlFile);
		prgwifiResult = new PrintWriter(gwifiResultFile);
		prdocTypeAll = new PrintWriter(docTypeAllFile);
		prdocTypeDF = new PrintWriter(docTypeDFFile);
		prdocTypeNW = new PrintWriter(docTypeNWFile);
		prdocTypeWB = new PrintWriter(docTypeWBFile);
		prqueryByDF = new PrintWriter(queryByDFFile);
		prqueryByNW = new PrintWriter(queryByNWFile);
		prqueryByWB = new PrintWriter(queryByWBFile);
		prentityNILvsE = new PrintWriter(entityNILvsEFIle);

		String DFRight = folder + "zmy2/DFRight.txt";
		String DFGeSeWrong = folder + "zmy2/DFGeSeWrong.txt";
		String DFGnSeWrong = folder + "zmy2/DFGnSeWrong.txt";
		String DFGeSnWrong = folder + "zmy2/DFGeSnWrong.txt";
		String NWRight = folder + "zmy2/NWRight.txt";
		String NWGeSeWrong = folder + "zmy2/NWGeSeWrong.txt";
		String NWGnSeWrong = folder + "zmy2/NWGnSeWrong.txt";
		String NWGeSnWrong = folder + "zmy2/NWGeSnWrong.txt";

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

		prDFRight = new PrintWriter(prDFRightFile);
		prDFGeSeWrong = new PrintWriter(prDFGeSeWrongFile);
		prDFGnSeWrong = new PrintWriter(prDFGnSeWrongFile);
		prDFGeSnWrong = new PrintWriter(prDFGeSnWrongFile);
		prNWRight = new PrintWriter(prNWRightFile);
		prNWGeSeWrong = new PrintWriter(prNWGeSeWrongFile);
		prNWGnSeWrong = new PrintWriter(prNWGnSeWrongFile);
		prNWGeSnWrong = new PrintWriter(prNWGeSnWrongFile);

	}

	/**
	 * initial the Gwifi service
	 * 
	 * @param folder
	 * @throws Exception
	 */
	private void zhgwifiInit(String folder) throws Exception
	{
		zhservice = new NLPAnnotationService("configs/hub-template.xml", "configs/wikipedia-template-zh.xml",
				"configs/NLPConfig.properties", Language.ZH, Language.EN, KB.DBPEDIA, NLPModel.NER,
				DisambiguationModel.PAGERANK, MentionMode.NON_OVERLAPPED, ResponseMode.BEST, RepeatMode.FIRST);
	}

	/**
	 * initial the Gwifi service
	 * 
	 * @param folder
	 * @throws Exception
	 */
	private void engwifiInit(String folder) throws Exception
	{
		enservice = new NLPAnnotationService("configs/hub-template.xml", "configs/wikipedia-template-en.xml",
				"configs/NLPConfig.properties", Language.EN, Language.EN, KB.DBPEDIA, NLPModel.NER,
				DisambiguationModel.PAGERANK, MentionMode.NON_OVERLAPPED, ResponseMode.BEST, RepeatMode.FIRST);
	}

	/**
	 * initial the mongoDB
	 */
	@SuppressWarnings("deprecation")
	private void mongoInit()
	{
		try
		{
			mongo = new Mongo("aifb-ls3-maia.aifb.kit.edu", 19005);
		}
		catch (UnknownHostException e)
		{
			e.printStackTrace();
		}
		db = mongo.getDB("KBP");
		eval_source_2014 = db.getCollection("eval_source_2014");
		getentityid_kbp_kb = db.getCollection("kbp_kb");
		getqueries = db.getCollection("eval_queries_2014");
		eval_answers_2014 = db.getCollection("test");
		goldfileEntid = db.getCollection("eval_links_2014");

		newDictionary = db.getCollection("DictionaryFrom_cedict_ts");
		baidu = db.getCollection("DictionaryFrom_zh_en_links");

	}

	/**
	 * get data from MongoDB collection eval_source_2014 and put them into Gwifi
	 * service and save the result in the file "zmy/results.txt"
	 * 
	 * @throws Exception
	 */
	private void getDataFromMongodb() throws Exception
	{

		DBCursor cur = eval_source_2014.find();
		cur.addOption(com.mongodb.Bytes.QUERYOPTION_NOTIMEOUT);
//		System.err.println(cur.size());

		int i = 1;
		// for(int j=1;j<=4;j++){
		// cur.next();
		// }
		while (cur.hasNext())
		{
			 if(i == 5){
			 break;
			 }

			DBObject curobj = cur.next();
			String doc_id = curobj.get("doc_id").toString();
			String doc_text = curobj.get("doc_text").toString();

			String gwifiResult;
			if (isChinese(doc_id) == true)
			{
				gwifiResult = zhcallGwifiService(doc_text);
			}
			else
			{
				gwifiResult = encallGwifiService(doc_text);
			}

			System.err.println(i + "\t" + "the id of this tex is:" + "\t" + doc_id);
			System.out.println();
			System.out.println(gwifiResult);
			System.out.println();

			prgwifiResult.println(i + "\t" + "the id of this tex is:" + "\t" + doc_id);
			prgwifiResult.println(gwifiResult);

			ArrayList<EntityBean> entitybeans = this.getEntInfoFromXml(gwifiResult);

			for (Iterator<EntityBean> iter = entitybeans.iterator(); iter.hasNext();)
			{
				EntityBean entityBean = iter.next();
				String entityname = entityBean.getEntityname();
				String mentionLabel = entityBean.getMentionLabel();
				String beg = entityBean.getBeg();
				String end = entityBean.getEnd();
				String type = entityBean.getType();
				String entityid = this.getEntidFromkbpkb(entityname);
				
				if(type.equals("ORG"))
				{
					type = "ORG";
				}else if(type.equals("LOC"))
				{
					type = "GPE";
				}else if(type.equals("PERSON"))
				{
					type = "PER";
				}else if(type.equals("MISC"))
				{
					type = "MISC";	//miscellaneous
				}
				
				this.saveAnswers2Mongodb(doc_id, entityname, entityid, mentionLabel, beg, end, type);
			}
			i++;
		}
		prgwifiResult.close();
	}

	public static boolean isChinese(String docid)
	{
		if (docid.contains("cmn"))
		{
			return true;
		}
		else if (docid.contains("CMN"))
		{
			return true;
		}
		else
			return false;
	}

	/**
	 * call Gwifi service
	 * 
	 * @param text
	 * @return
	 * @throws Exception
	 */
	public String zhcallGwifiService(String text) throws Exception
	{

		return zhservice.annotate(text, null);

	}

	/**
	 * call Gwifi service
	 * 
	 * @param text
	 * @return
	 * @throws Exception
	 */
	public String encallGwifiService(String text) throws Exception
	{

		return enservice.annotate(text, null);

	}

	/**
	 * get EntityID from MongoDB colletion kbp_kb
	 * 
	 * @param displayName
	 * @return
	 */
	private String getEntidFromkbpkb(String displayName)
	{
		BasicDBObject query = new BasicDBObject();
		query.put("entity_name", displayName);
		String entityid;
		
		if (null != getentityid_kbp_kb.findOne(query))
		{
			entityid = getentityid_kbp_kb.findOne(query).get("entitiy_id").toString();
			return entityid;
		}
		else
		{
			if (null != eval_answers_2014.findOne(query))
			{
				entityid = eval_answers_2014.findOne(query).get("entity_id").toString();
				return entityid;
			}
			else
			{
			java.text.DecimalFormat format = new java.text.DecimalFormat("0000000");
			nilInkbp = "NILInkbp" + format.format(nrOfNILInkbp);
			nrOfNILInkbp++;
			return nilInkbp;
			}
		}
	}

	private String getEntNameFromkbpkb(String entityId)
	{
		BasicDBObject query = new BasicDBObject();
		query.put("entitiy_id", entityId);
		DBObject curobjEntityName = getentityid_kbp_kb.findOne(query);
		String entityName;

		if (entityId.contains("NIL"))
		{
			entityName = entityId;
		}
		else
		{
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
	public String readXmlFile(String filename) throws IOException
	{
		BufferedReader br = new BufferedReader(new FileReader(new File(filename)));
		StringBuffer sb = new StringBuffer();
		String line;
		while ((line = br.readLine()) != null)
		{
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
			throws IOException, SAXException, ParserConfigurationException, DocumentException
	{
		ArrayList<EntityBean> entities = new ArrayList<EntityBean>();

		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		Document inputDoc = docBuilder.parse(new InputSource(new StringReader(xmlstring)));
		NodeList annoNodeList = inputDoc.getElementsByTagName("Annotation");
		for (int i = 0; i < annoNodeList.getLength(); i++)
		{
			Element ele = (Element) annoNodeList.item(i);
			String entityname = ele.getAttribute("displayName");

			NodeList annoNodeListSon = inputDoc.getElementsByTagName("mention");
			Element ele2 = (Element) annoNodeListSon.item(i);
			
			String mentionLabel = ele2.getAttribute("label");
			String start = ele2.getAttribute("position");
			String mentionLength = ele2.getAttribute("length");
			String type = ele2.getAttribute("type");
			String beg = new Integer(Integer.parseInt(start)+1).toString();
			String end = new Integer(Integer.parseInt(start)+Integer.parseInt(mentionLength)).toString();
			
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
	public void saveAnswers2Mongodb(String doc_id, String entiyname, String entity_id, String mentionLabel, String beg, String end, String type)
	{
		DBObject insertData;
		insertData = new BasicDBObject();
		insertData.put("doc_id", doc_id);
		insertData.put("entity_name", entiyname);
		insertData.put("entity_id", entity_id);
		insertData.put("query_name", mentionLabel);
		insertData.put("beg", beg);
		insertData.put("end", end);
		insertData.put("entity_type", type);
		eval_answers_2014.insert(insertData);

	}

	/**
	 * make a standard file of system output. It's also a answer list for
	 * queries. compare the result of Goldfile and system output.
	 * 
	 * @throws Exception
	 */
	private void getAnswers() throws Exception
	{

		DBCursor curqueries = getqueries.find();
		prAnswers.println("query_id" + "\t" + "entity_id" + "\t" + "entity_type");
		prXML.println("<kbpentlink>");

		//the result of sorted by doc_type
		prdocTypeAll.println("query_id" + "\t" + "query_name" + "\t" + "Gold_file_answers" + "\t" + "System_out_answers"
				+ "\t" + "doc_id");
		prdocTypeDF.println("query_id" + "\t" + "query_name" + "\t" + "Gold_file_answers" + "\t" + "System_out_answers"
				+ "\t" + "doc_id");
		prdocTypeNW.println("query_id" + "\t" + "query_name" + "\t" + "Gold_file_answers" + "\t" + "System_out_answers"
				+ "\t" + "doc_id");
		prdocTypeWB.println("query_id" + "\t" + "query_name" + "\t" + "Gold_file_answers" + "\t" + "System_out_answers"
				+ "\t" + "doc_id");
	
		//the result of entity(NIL) in Gold file but with entity(E) in System output
		prentityNILvsE.println("query_id" + "\t" + "query_name" + "\t" + "Gold_file_answers" + "\t"
				+ "System_out_answers" + "\t" + "doc_id");

		// the result of doc type of DF
		prDFRight.println("query_id" + "\t" + "query_name" + "\t" + "Gold_file_answers" + "\t" + "System_out_answers"
				+ "\t" + "doc_id");
		prDFGeSeWrong.println("query_id" + "\t" + "query_name" + "\t" + "Gold_file_answers" + "\t"
				+ "System_out_answers" + "\t" + "doc_id");
		prDFGnSeWrong.println("query_id" + "\t" + "query_name" + "\t" + "Gold_file_answers" + "\t"
				+ "System_out_answers" + "\t" + "doc_id");
		prDFGeSnWrong.println("query_id" + "\t" + "query_name" + "\t" + "Gold_file_answers" + "\t"
				+ "System_out_answers" + "\t" + "doc_id");
	
		//the result of doc type of NW
		prNWRight.println("query_id" + "\t" + "query_name" + "\t" + "Gold_file_answers" + "\t" + "System_out_answers"
				+ "\t" + "doc_id");
		prNWGeSeWrong.println("query_id" + "\t" + "query_name" + "\t" + "Gold_file_answers" + "\t"
				+ "System_out_answers" + "\t" + "doc_id");
		prNWGnSeWrong.println("query_id" + "\t" + "query_name" + "\t" + "Gold_file_answers" + "\t"
				+ "System_out_answers" + "\t" + "doc_id");
		prNWGeSnWrong.println("query_id" + "\t" + "query_name" + "\t" + "Gold_file_answers" + "\t"
				+ "System_out_answers" + "\t" + "doc_id");

		// prqueryByDF.println("query_id" + "\t" + "entity_id");
		// prqueryByNW.println("query_id" + "\t" + "entity_id");
		// prqueryByWB.println("query_id" + "\t" + "entity_id");
		
		HashMap<String, String> mapFoundNIL = new HashMap<String, String>();
		HashMap<String, String> mapNotFound = new HashMap<String, String>();

		int i = 1;
		
		while (curqueries.hasNext())
		{

			DBObject curobj = curqueries.next();
			String queryid = curobj.get("query_id").toString();
			String docid = curobj.get("doc_id").toString();
			String queryname = curobj.get("query_name").toString(); 
			
			String goldfileEntid = getGoldfileEntid(queryid); //entityid in goldfile
			String goldfileEntType = getGoldfileEntType(queryid);
			
			String goldfileEntName = getEntNameFromkbpkb(goldfileEntid); // entity_name in goldfile。

			
			
//			BasicDBList condList = new BasicDBList();
//			
//			BasicDBObject query1 = new BasicDBObject();
//			query1.append("doc_id", docid);
			
			BasicDBObject query = new BasicDBObject();
			query.append("query_name", queryname);
			
//			condList.add(query1);
//			condList.add(query2);
//			
//			BasicDBObject query = new BasicDBObject();
//			query.put("$and", condList);
			
			String beg = null;			
			String end = null;
						
			String sysoutEntid;
			String sysoutEntName;
			String sysoutEntType = null;
			String nilInkbp = null;

			if (null != eval_answers_2014.findOne(query))
			{
				sysoutEntid = eval_answers_2014.findOne(query).get("entity_id").toString();
				sysoutEntName = eval_answers_2014.findOne(query).get("entity_name").toString();
				sysoutEntType = eval_answers_2014.findOne(query).get("entity_type").toString();
				beg = eval_answers_2014.findOne(query).get("beg").toString();
				end = eval_answers_2014.findOne(query).get("end").toString();
				if (sysoutEntid.contains("NIL") == true)
				{
					nilInkbp = sysoutEntid;
					if (mapFoundNIL.get(nilInkbp) == null)
					{
						java.text.DecimalFormat format = new java.text.DecimalFormat("0000");
						sysoutEntid = "NIL" + format.format(i);
						sysoutEntName = sysoutEntid;
						i++;
						mapFoundNIL.put(nilInkbp, sysoutEntid);
					}
					else
					{
						sysoutEntid = mapFoundNIL.get(nilInkbp);
						sysoutEntName = sysoutEntid;
					}
				}

			}
			else
			{
				if (mapNotFound.get(queryname) == null)
				{
					java.text.DecimalFormat format = new java.text.DecimalFormat("0000");
					sysoutEntid = "NIL" + format.format(1);
					sysoutEntName = sysoutEntid;
					i++;
					mapNotFound.put(queryname, sysoutEntid);
				}
				else
				{
					sysoutEntid = mapNotFound.get(queryname);
					sysoutEntName = sysoutEntid;
				}
			}
				
				
/*				if (null != eval_answers_2014.findOne(query))
				{
					sysoutEntid = eval_answers_2014.findOne(query).get("entity_id").toString();
					sysoutEntName = eval_answers_2014.findOne(query).get("entity_name").toString();
					if (sysoutEntid.equals("NIL") == true)
					{
						java.text.DecimalFormat format = new java.text.DecimalFormat("0000");
						sysoutEntid = "NIL" + format.format(i);
						sysoutEntName = sysoutEntid;
						i++;
					}
				}
				else
				{
					java.text.DecimalFormat format = new java.text.DecimalFormat("0000");
					sysoutEntid = "NIL" + format.format(i);
					sysoutEntName = sysoutEntid;
					i++;
				}	
*/				

			String doctype = getDocType(docid);

			/*
			 * switch (doctype) { case "DF": prdocTypeDF.println(queryid + "\t"
			 * + entityID); prqueryByDF.println(queryid + "\t" + goldfile);
			 * break;
			 * 
			 * case "NW": prdocTypeNW.println(queryid + "\t" + entityID);
			 * prqueryByNW.println(queryid + "\t" + goldfile); break;
			 * 
			 * case "WB": prdocTypeWB.println(queryid + "\t" + entityID);
			 * prqueryByWB.println(queryid + "\t" + goldfile); break;
			 * 
			 * default: break;
			 * 
			 * }
			 */
			switch (doctype)
			{
			case "DF":
				{
					if (goldfileEntid.equals(sysoutEntid)
							|| ((true == goldfileEntid.contains("NIL")) && (true == sysoutEntid.contains("NIL"))))
					{
						prDFRight.println(
								queryid + "\t" + queryname + "\t" + goldfileEntName + "\t" + sysoutEntName + "\t" + docid);
					}
					else if (!goldfileEntid.equals(sysoutEntid) && (false == goldfileEntid.contains("NIL"))
							&& (false == sysoutEntid.contains("NIL")))
					{
						prDFGeSeWrong.println(
								queryid + "\t" + queryname + "\t" + goldfileEntName + "\t" + sysoutEntName + "\t" + docid);
					}
					else if ((true == goldfileEntid.contains("NIL")) && (false == sysoutEntid.contains("NIL")))
					{
						prDFGnSeWrong.println(
								queryid + "\t" + queryname + "\t" + goldfileEntName + "\t" + sysoutEntName + "\t" + docid);
					}
					else if ((false == goldfileEntid.contains("NIL")) && (true == sysoutEntid.contains("NIL")))
					{
						prDFGeSnWrong.println(
								queryid + "\t" + queryname + "\t" + goldfileEntName + "\t" + sysoutEntName + "\t" + docid);
					}
					break;
				}

			case "NW":
				{
					if (goldfileEntid.equals(sysoutEntid)
							|| ((true == goldfileEntid.contains("NIL")) && (true == sysoutEntid.contains("NIL"))))
					{
						prNWRight.println(
								queryid + "\t" + queryname + "\t" + goldfileEntName + "\t" + sysoutEntName + "\t" + docid);
					}
					else if (!goldfileEntid.equals(sysoutEntid) && (false == goldfileEntid.contains("NIL"))
							&& (false == sysoutEntid.contains("NIL")))
					{
						prNWGeSeWrong.println(
								queryid + "\t" + queryname + "\t" + goldfileEntName + "\t" + sysoutEntName + "\t" + docid);
					}
					else if ((true == goldfileEntid.contains("NIL")) && (false == sysoutEntid.contains("NIL")))
					{
						prNWGnSeWrong.println(
								queryid + "\t" + queryname + "\t" + goldfileEntName + "\t" + sysoutEntName + "\t" + docid);
					}
					else if ((false == goldfileEntid.contains("NIL")) && (true == sysoutEntid.contains("NIL")))
					{
						prNWGeSnWrong.println(
								queryid + "\t" + queryname + "\t" + goldfileEntName + "\t" + sysoutEntName + "\t" + docid);
					}
					break;
				}

			case "WB":
				{
					prdocTypeWB.println(queryid + "\t" + queryname + "\t" + goldfileEntName + "\t" + sysoutEntName + "\t" + docid);
					break;
				}
				

			default:
				break;

			}

			prAnswers.println(queryid + "\t" + sysoutEntid + "\t" + sysoutEntType);

			prdocTypeAll.println(queryid + "\t" + queryname + "\t" + goldfileEntName + "\t" + sysoutEntName + "\t" + docid);
			
			prXML.println("<query id=" + "\""+ queryid + "\"" + ">");
			prXML.println("<name>" + queryname + "</name>");
			prXML.println("<docid>" + docid + "</docid>");
			prXML.println("<beg>" + beg + "</beg>");
			prXML.println("<end>" + end + "</end>");
			prXML.println("</query>");
			
		}
		
		prXML.println("</kbpentlink>");
		
		prAnswers.close();
		prXML.close();
		prdocTypeAll.close();
		prdocTypeDF.close();
		prdocTypeNW.close();
		prdocTypeWB.close();
		
		// prqueryByDF.close();
		// prqueryByNW.close();
		// prqueryByWB.close();
		
		prentityNILvsE.close();

		prDFRight.close();
		prDFGeSeWrong.close();
		prDFGnSeWrong.close();
		prDFGeSnWrong.close();
		
		prNWRight.close();
		prNWGeSeWrong.close();
		prNWGnSeWrong.close();
		prNWGeSnWrong.close();

		System.err.println("Done!");
	}

	/**
	 * get entityid from GoldFile with queryid.
	 * 
	 * @param queryid
	 * @return entityid in GoldFile
	 */
	private String getGoldfileEntid(String queryid)
	{
		BasicDBObject query = new BasicDBObject();
		query.put("query_id", queryid);
		DBObject curobjid = null;
		if (null != goldfileEntid.findOne(query))
		{
			curobjid = goldfileEntid.findOne(query);
		}
		else
		{
			return "not found";
		}
		String entityid = curobjid.get("entitiy_id").toString();
		return entityid;
	}
	
	private String getGoldfileEntType(String queryid)
	{
		BasicDBObject query = new BasicDBObject();
		query.put("query_id", queryid);
		DBObject curobjid = null;
		if (null != goldfileEntid.findOne(query))
		{
			curobjid = goldfileEntid.findOne(query);
		}
		else
		{
			return "not found";
		}
		String entityType = curobjid.get("entity_type").toString();
		return entityType;
	}

	public String getDocType(String docid)
	{
		BasicDBObject query = new BasicDBObject();
		query.put("doc_id", docid);
		DBObject curobjid = null;
		if (null != eval_source_2014.findOne(query))
		{
			curobjid = eval_source_2014.findOne(query);
		}
		else
		{
			return "not found";
		}
		String doctype = curobjid.get("doc_type").toString();
		return doctype;
	}

	public static void main(String[] args) throws Exception
	{
		Source2014 s = new Source2014("");
		s.getDataFromMongodb();
		s.getAnswers();
		
	}
}