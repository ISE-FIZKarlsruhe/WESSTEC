package edu.kit.aifb.gwifi.evaluation.kbp.index;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.xml.sax.SAXException;

import com.mongodb.BasicDBObject;

import edu.kit.aifb.gwifi.evaluation.kbp.Constants;
import edu.kit.aifb.gwifi.evaluation.kbp.mongodb.MongoDBWriter;

public class SourceDocumentsIndexer_regular {

	private MongoDBWriter writer;
	private SAXReader xmlReader;
	private Document xmlDoc;
	private File docFile;

	public SourceDocumentsIndexer_regular(String url, String dbName, String collName, File file) {
		try {
			this.writer = new MongoDBWriter(url, dbName, collName);
			xmlReader = new SAXReader();
			xmlReader.setValidation(false);
			xmlReader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			xmlDoc = xmlReader.read(file);
			docFile = file;
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (DocumentException e) {
			e.printStackTrace();
		}
	}

	// Web (WB) data: filenames beginning with "cmn-NG" and "eng-NG"
	public void writeWBElement() {
		Element root = xmlDoc.getRootElement();
		String doc_id = root.elementText("DOCID").trim();
		String text = extractText(docFile);

		BasicDBObject dbObject = new BasicDBObject();
		dbObject.append(Constants.SOURCE_DOC_ID, doc_id);
		dbObject.append(Constants.SOURCE_DOC_TYPE, "WB");
		dbObject.append(Constants.SOURCE_DOC_TEXT, text);
		writer.write(dbObject);
	}

	// Discussion Forum (DF) data: filenames starting with "bolt-"
	public void writeDFElement() {
		Element root = xmlDoc.getRootElement();
		String doc_id = root.attributeValue("id");
		String text = extractText(docFile);

		BasicDBObject dbObject = new BasicDBObject();
		dbObject.append(Constants.SOURCE_DOC_ID, doc_id);
		dbObject.append(Constants.SOURCE_DOC_TYPE, "DF");
		dbObject.append(Constants.SOURCE_DOC_TEXT, text);
		writer.write(dbObject);
	}

	// Newswire (NW) data
	public void writeNWElement() {
		Element root = xmlDoc.getRootElement();
		String doc_id = root.attributeValue("id");
		String text = extractText(docFile);

		BasicDBObject dbObject = new BasicDBObject();
		dbObject.append(Constants.SOURCE_DOC_ID, doc_id);
		dbObject.append(Constants.SOURCE_DOC_TYPE, "NW");
		dbObject.append(Constants.SOURCE_DOC_TEXT, text);
		writer.write(dbObject);
	}

	public static String extractText(File file) {
		String text = "";
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
			String line;
			while ((line = reader.readLine()) != null) {
				text += line + " "; // change newline to whitespace
			}

			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		text = text.replaceAll("<.+?>", "\n");
		text = text.replaceAll("&amp;", "&");
		return text;
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
				SourceDocumentsIndexer_regular indexer = new SourceDocumentsIndexer_regular(url, dbName, collName, file);
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
	
//	public static void main(String args[]) throws IOException {
//		File file = new File("/Users/leizhang/Data/datasets/LDC/LDC2015E17_ZH/data/2014/eval/source_documents/AFP_ENG_20100326.0151.xml");
//		String text = extractText(file);
//		BufferedWriter bw = new BufferedWriter(new FileWriter(new File("res/test.txt")));
//		bw.write(text);
//		bw.close();
//	}

}
