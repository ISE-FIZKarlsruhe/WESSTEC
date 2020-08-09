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


// This class could miss some useful contents inside the tags which are not specified in the code. 
public class SourceDocumentsIndexer_tag {

	private MongoDBWriter writer;
	private SAXReader reader;
	private Document xmlDoc;

	public SourceDocumentsIndexer_tag(String url, String dbName, String collName, File file) {
		try {
			this.writer = new MongoDBWriter(url, dbName, collName);
			reader = new SAXReader();
			reader.setValidation(false);
			reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			xmlDoc = reader.read(file);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (DocumentException e) {
			e.printStackTrace();
		}
	}

	// Web (WB) data: filenames beginning with "cmn-NG" and "eng-NG"
	public void writeWBElement() {
		Element root = xmlDoc.getRootElement();
		String doc_id = root.elementText("DOCID");
		String doc_type = root.elementText("DOCTYPE");
		String dateline = root.elementText("DATETIME");
		String headline = root.element("BODY").elementText("HEADLINE");
		Element text = root.element("BODY").element("TEXT");
		StringBuilder sb = new StringBuilder();
		for (Object p : text.elements("POST")) {
			String pText = ((Element) p).getTextTrim();
			sb.append(pText + "\n");
			for (Object q : ((Element) p).elements("quote")) {
				String qText = ((Element) q).getTextTrim();
				sb.append(qText + "\n");
			}
		}
		for (Object q : root.elements("quote")) {
			String qText = ((Element) q).getTextTrim();
			sb.append(qText + "\n");
		}
		String textString = sb.toString();
		BasicDBObject dbObject = new BasicDBObject();
		dbObject.append(Constants.SOURCE_DOC_ID, doc_id);
		dbObject.append(Constants.SOURCE_DOC_TYPE, "WB");
		dbObject.append(Constants.SOURCE_DOC_DATELINE, dateline);
		dbObject.append(Constants.SOURCE_DOC_HEADLINE, headline);
		dbObject.append(Constants.SOURCE_DOC_TEXT, textString);
		writer.write(dbObject);
	}

	// Discussion Forum (DF) data: filenames starting with "bolt-"
	public void writeDFElement() {
		Element root = xmlDoc.getRootElement();
		String doc_id = root.attributeValue("id");
		String headline = root.elementText("headline");
		StringBuilder sb = new StringBuilder();
		for (Object p : root.elements("post")) {
			String pText = ((Element) p).getTextTrim();
			sb.append(pText + "\n");
			for (Object q : ((Element) p).elements("quote")) {
				String qText = ((Element) q).getTextTrim();
				sb.append(qText + "\n");
			}
		}
		for (Object q : root.elements("quote")) {
			String qText = ((Element) q).getTextTrim();
			sb.append(qText + "\n");
		}
		String textString = sb.toString();
		BasicDBObject dbObject = new BasicDBObject();
		dbObject.append(Constants.SOURCE_DOC_ID, doc_id);
		dbObject.append(Constants.SOURCE_DOC_TYPE, "DF");
		dbObject.append(Constants.SOURCE_DOC_HEADLINE, headline);
		dbObject.append(Constants.SOURCE_DOC_TEXT, textString);
		writer.write(dbObject);
	}

	// Newswire (NW) data
	public void writeNWElement() {
		Element root = xmlDoc.getRootElement();
		String doc_id = root.attributeValue("id");
		String type = root.attributeValue("type");
		String dateline = root.elementText("DATELINE");
		String headline = root.elementText("HEADLINE");
		Element text = root.element("TEXT");
		StringBuilder sb = new StringBuilder();
		String textString;

		if (type.equals("story")) {
			for (Object p : text.elements("P")) {
				String pText = ((Element) p).getTextTrim();
				sb.append(pText + "\n");
			}
			textString = sb.toString();
		} else {
			textString = text.getTextTrim();
		}

		BasicDBObject dbObject = new BasicDBObject();
		dbObject.append(Constants.SOURCE_DOC_ID, doc_id);
		dbObject.append(Constants.SOURCE_DOC_TYPE, "NW");
		if (dateline != null && !dateline.equals(""))
			dbObject.append(Constants.SOURCE_DOC_DATELINE, dateline);
		if (headline != null && !headline.equals(""))
			dbObject.append(Constants.SOURCE_DOC_HEADLINE, headline);
		dbObject.append(Constants.SOURCE_DOC_TEXT, textString);
		writer.write(dbObject);
	}

	public void close() {
		writer.close();
	}

	// arg1: "mongodb://aifb-ls3-maia.aifb.kit.edu:19005"
	// arg2: "/home/aifb-ls3-maia/lzh/LDC/LDC2015E17/data/2014/eval/source_documents"
	// or "/home/aifb-ls3-maia/lzh/LDC/LDC2015E17/data/2014/training/source_documents"
	// arg3: KBP
	// arg4: eval_source_2014
	// or training_source_2014
	public static void main(String args[]) {
		String url = args[0];
		File folder = new File(args[1]);
		String dbName = args[2];
		String collName = args[3];
		for (File file : folder.listFiles()) {
			if (!file.isDirectory() && file.getName().contains("xml")) {
				System.out.println(file.getName());
				SourceDocumentsIndexer_tag indexer = new SourceDocumentsIndexer_tag(url, dbName, collName, file);
				if (file.getName().startsWith("cmn-") || file.getName().startsWith("eng-")) {
					indexer.writeWBElement();
				} else if (file.getName().startsWith("bolt-")) {
					indexer.writeDFElement();
				} else {
					indexer.writeNWElement();
				}
			}
		}

	}

}
