package edu.kit.aifb.gwifi.evaluation.kbp.index;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import com.mongodb.BasicDBObject;

import edu.kit.aifb.gwifi.evaluation.kbp.Constants;
import edu.kit.aifb.gwifi.evaluation.kbp.mongodb.MongoDBWriter;

public class LinksIndexer {
	
	private MongoDBWriter writer;
	private BufferedReader reader; 
	
	public LinksIndexer(String url, String dbName, String collName, String filePath) {
		try {
			this.writer = new MongoDBWriter(url, dbName, collName);
			this.reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public void writeLinks() {
		try {
			String line = "";
			int i = 0;
			while ((line = reader.readLine()) != null) {
				String[] data = line.split("\\s+");
				System.out.println(++i + ":" + data[0] + ":" + data[1] + ":" + data[2] + ":" + data[3]);
				BasicDBObject dbObject = new BasicDBObject();
				dbObject.append(Constants.LINKS_QUERY_ID, data[0]);
				dbObject.append(Constants.LINKS_ENTITY_ID, data[1]);
				dbObject.append(Constants.LINKS_ENTITY_TYPE, data[2]);
				dbObject.append(Constants.LINKS_GENRE, data[3]);
				writer.write(dbObject);
			}

			writer.index(Constants.LINKS_QUERY_ID);
			writer.index(Constants.LINKS_ENTITY_ID);
			writer.index(Constants.LINKS_ENTITY_TYPE);
			writer.index(Constants.LINKS_GENRE);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void close() {
		try {
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		writer.close();
	}
	
	// arg1: "mongodb://aifb-ls3-maia.aifb.kit.edu:19005"
	// arg2: "/home/aifb-ls3-maia/lzh/LDC/LDC2015E17/data/2014/eval/tac_kbp_2014_chinese_entity_linking_evaluation_KB_links.tab"
	// or "/home/aifb-ls3-maia/lzh/LDC/LDC2015E17/data/2014/training/tac_kbp_2014_chinese_entity_linking_training_KB_links.tab"
	// or /Users/leizhang/Data/LDC/LDC2015E17/data/2014/training/tac_kbp_2014_chinese_entity_linking_training_KB_links.tab
	// or /Users/leizhang/Data/LDC/LDC2015E17/data/2014/eval/tac_kbp_2014_chinese_entity_linking_evaluation_KB_links.tab
	// arg3: KBP
	// arg4: eval_links_2014
	// or training_links_2014
	public static void main(String args[]) {
		String url = args[0];
		String filePath = args[1];
		String dbName = args[2];
		String collName = args[3];
		LinksIndexer indexer = new LinksIndexer(url, dbName, collName, filePath);
		indexer.writeLinks();
		indexer.close();
	}

}
