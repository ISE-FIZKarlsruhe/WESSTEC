package edu.kit.aifb.gwifi.evaluation.kbp.index;

import java.io.File;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.xml.sax.SAXException;

import com.mongodb.BasicDBObject;

import edu.kit.aifb.gwifi.evaluation.kbp.Constants;
import edu.kit.aifb.gwifi.evaluation.kbp.mongodb.MongoDBWriter;

public class QueriesIndexer {

	private MongoDBWriter writer;
	private SAXReader reader;
	private Document xmlDoc; 
	
	public QueriesIndexer(String url, String dbName, String collName, String filePath) {
		try {
			this.writer = new MongoDBWriter(url, dbName, collName);
			reader = new SAXReader();
			reader.setValidation(false);
			reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			File file = new File(filePath);
			xmlDoc = reader.read(file);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (DocumentException e) {
			e.printStackTrace();
		}
	}
	
	public void writeQueries() {
		Element root = xmlDoc.getRootElement();
		int i = 0;
		for (Object element : root.elements("query")) {
			String id = ((Element) element).attributeValue("id");
			String name = ((Element) element).elementText("name");
			String docid = ((Element) element).elementText("docid");
			String beg = ((Element) element).elementText("beg");
			String end = ((Element) element).elementText("end");
			System.out.println(++i + ":" + id + ":" + name + ":" + docid);
			BasicDBObject dbObject = new BasicDBObject();
			dbObject.append(Constants.QUERIES_QUERY_ID, id);
			dbObject.append(Constants.QUERIES_DOC_ID, docid);
			dbObject.append(Constants.QUERIES_NAME, name);
			dbObject.append(Constants.QUERIES_BEGIN_POSITION, beg);
			dbObject.append(Constants.QUERIES_END_POSITION, end);
			writer.write(dbObject);
		}
		
		writer.index(Constants.QUERIES_QUERY_ID);
		writer.index(Constants.QUERIES_DOC_ID);
		writer.index(Constants.QUERIES_NAME);
		writer.index(Constants.QUERIES_BEGIN_POSITION);
		writer.index(Constants.QUERIES_END_POSITION);
	}
	
	public void close() {
		writer.close();
	}
	
	// arg1: "mongodb://aifb-ls3-maia.aifb.kit.edu:19005"
	// arg2: "/home/aifb-ls3-maia/lzh/LDC/LDC2015E17/data/2014/eval/tac_kbp_2014_chinese_entity_linking_evaluation_queries.xml"
	// or "/home/aifb-ls3-maia/lzh/LDC/LDC2015E17/data/2014/training/tac_kbp_2014_chinese_entity_linking_training_queries.xml"
	// arg3: KBP
	// arg4: eval_queries_2014
	// or training_queries_2014
	public static void main(String args[]) {
		String url = args[0];
		String filePath = args[1];
		String dbName = args[2];
		String collName = args[3];
		QueriesIndexer indexer = new QueriesIndexer(url, dbName, collName, filePath);
		indexer.writeQueries();	
		indexer.close();
	}

}
