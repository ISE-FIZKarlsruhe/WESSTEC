package edu.kit.aifb.gwifi.evaluation.kbp.index;

import java.io.File;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.mongodb.BasicDBObject;

import edu.kit.aifb.gwifi.evaluation.kbp.Constants;
import edu.kit.aifb.gwifi.evaluation.kbp.mongodb.MongoDBWriter;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Wikipedia;

public class KnowledgeBaseIndexer {

	private MongoDBWriter writer;
	private SAXReader reader;
	private Document xmlDoc;

	private Wikipedia wikipedia;

	public KnowledgeBaseIndexer(String url, String dbName, String collName, File file, String configFile) {
		try {
			this.writer = new MongoDBWriter(url, dbName, collName);
			reader = new SAXReader();
			reader.setValidation(false);
			reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			xmlDoc = reader.read(file);

			if (configFile != null)
				wikipedia = new Wikipedia(new File(configFile), false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void writeKB() {
		Element root = xmlDoc.getRootElement();
		int i = 0;
		for (Object element : root.elements("entity")) {
			String wikiTitle = ((Element) element).attributeValue("wiki_title");
			if (wikipedia != null) {
				Article article = wikipedia.getArticleByTitle(wikiTitle);
				if (article != null)
					wikiTitle = article.getTitle();
			}
			String type = ((Element) element).attributeValue("type");
			String id = ((Element) element).attributeValue("id");
			String name = ((Element) element).attributeValue("name");
			String wikiText = ((Element) element).element("wiki_text").getText();
			System.out.println(++i + ":" + wikiTitle + ":" + type + ":" + id + ":" + name + ":" + wikiText);
			BasicDBObject dbObject = new BasicDBObject();
			dbObject.append(Constants.KB_ENTITY_ID, id);
			dbObject.append(Constants.KB_WIKI_TITLE, wikiTitle);
			dbObject.append(Constants.KB_ENTITY_NAME, name);
			dbObject.append(Constants.KB_ENTITY_TYPE, type);
			dbObject.append(Constants.KB_WIKI_TEXT, wikiText);
			writer.write(dbObject);
		}

		writer.index(Constants.KB_ENTITY_ID);
		writer.index(Constants.KB_WIKI_TITLE);
		writer.index(Constants.KB_ENTITY_NAME);
		writer.index(Constants.KB_ENTITY_TYPE);
	}

	public void close() {
		writer.close();
	}

	// arg0: "mongodb://aifb-ls3-maia.aifb.kit.edu:19005"
	// arg1 "/home/aifb-ls3-maia/lzh/LDC/LDC2014T16/tac_kbp_ref_know_base/data/"
	// or "/Users/leizhang/Data/LDC/LDC2014T16/tac_kbp_ref_know_base/data/"
	// arg2: KBP
	// arg3: kbp_kb
	// arg4:
	public static void main(String args[]) {
		String url = args[0];
		File folder = new File(args[1]);
		String dbName = args[2];
		String collName = args[3];
		String config = null;
		if (args.length == 5)
			config = args[4];
		for (File file : folder.listFiles()) {
			if (!file.isDirectory() && file.getName().contains("xml")) {
				KnowledgeBaseIndexer indexer = new KnowledgeBaseIndexer(url, dbName, collName, file, config);
				indexer.writeKB();
				indexer.close();
			}
		}
	}

}
