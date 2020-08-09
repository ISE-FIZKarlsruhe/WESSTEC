package edu.kit.aifb.gwifi.yinwang;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

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
import com.mongodb.DBObject;
import com.mongodb.Mongo;

import edu.kit.aifb.gwifi.service.NLPAnnotationService;

public class readtxt {
	
	private Mongo mongo;
	private DB db;
	private DBCollection weibo;
	private DBCollection weibo_marked;
	
	private BufferedReader buf;
	private String doc_id;
	private String doc_text;
	private String is_parallel;
	
	public readtxt()
	{
		mongoInit();
	}


	public String readxmlFile(String filename) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(new File(filename)));
		StringBuffer sb = new StringBuffer();
		String line;
		while ((line = br.readLine()) != null) {
			sb.append(line).append("\n");
		}
		br.close();
		return sb.toString().trim();
		
	}

	public List getEntitiesFromxml(String xmlstring)
			throws IOException, SAXException, ParserConfigurationException, DocumentException {
		
		List<MarkedText> markedText = new ArrayList<MarkedText>();

		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		Document inputDoc = docBuilder.parse(new InputSource(new StringReader(xmlstring)));
		
		NodeList root = inputDoc.getElementsByTagName("weibo");
		Element element = (Element) root.item(0);
		String rowDoc = element.getTextContent().replace("\n", "");
		
		String[] a = rowDoc.split("\t");
	
//		for(int i = 0; i < a.length; i++)
//		{
//			System.out.println(a[i]);
//		}
		
		for(int j = 0; j<(a.length-1)/4; j++)
		{
			doc_id = a[2 + 4 * j];
			doc_text = a[4 + 4 * j];
			
			doc_text = doc_text.substring(26);
			
			String[] b = doc_text.split("     ");
			doc_text = b[0].trim();
			
    		System.out.println(doc_id);
			System.out.println(doc_text);
     		System.out.println();
			
			is_parallel = getParallelValue(doc_id);
			this.saveInfo2Mongodb(doc_id, doc_text, is_parallel);
			markedText.add(new MarkedText(doc_id,doc_text, is_parallel));
		}
		
		return markedText;
		
	}

	private String getParallelValue(String doc_id) {
		BasicDBObject query = new BasicDBObject();
		query.put("id", doc_id);
		DBObject curobjid = null;
		String result= null;
		
		if(null!= weibo.findOne(query))
			curobjid = weibo.findOne(query);
			result = curobjid.get("is_parallel").toString();
		
		return result;
	}

	public void saveInfo2Mongodb(String doc_id,String doc_text,String is_parallel){
		DBObject insertData;
		insertData= new BasicDBObject();
		insertData.put("doc_id", doc_id);
		insertData.put("doc_text",doc_text);		
		insertData.put("is_parallel",is_parallel);
		weibo_marked.insert(insertData);

	}
	
	public String readfile(String filename) throws IOException, DocumentException {
		InputStream is = new FileInputStream(filename);
		buf = new BufferedReader(new InputStreamReader(is));

		String line = buf.readLine();
		StringBuilder sb = new StringBuilder();

		while (line != null) {
			if (line.contains("xml")) {
				line = line.replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "");
			}

			sb.append(line).append("\n");
			line = buf.readLine();
		}

		String fileAsString = sb.toString();
//		System.out.println("Contents : " + fileAsString);
		return fileAsString;
	}

	private void mongoInit(){
		try {
			mongo = new Mongo("aifb-ls3-maia.aifb.kit.edu", 19005);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		db = mongo.getDB("KBP");
		weibo = db.getCollection("weibo");
		weibo_marked = db.getCollection("weibo_marked");
		weibo_marked.drop();
	}
	
	public static void main(String argv[])
			throws IOException, SAXException, ParserConfigurationException, DocumentException {
		String filePath = "C:/Users/Nekromantik/Desktop/1-870.txt";
		readtxt r = new readtxt();
		String a = r.readfile(filePath);
		r.getEntitiesFromxml(a);
		System.err.println("Done!!!!!!");
	}

}
