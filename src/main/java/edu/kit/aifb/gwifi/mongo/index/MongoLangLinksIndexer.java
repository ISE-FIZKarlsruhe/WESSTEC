package edu.kit.aifb.gwifi.mongo.index;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

import edu.kit.aifb.gwifi.mongo.DBConstants;
import edu.kit.aifb.gwifi.mongo.MongoResource;
import edu.kit.aifb.gwifi.mongo.Property;
import edu.kit.aifb.gwifi.util.nlp.Language;

/**
 * This class is responsible to build the collection named "langlinks**", which takes two input files for each language,
 * e.g. en and zh, and the language links from en (source language) to zh (targe language) and from zh (source language)
 * to en (targe language) will be stored in MongoDB
 * 
 */
public class MongoLangLinksIndexer {

	private static String SPLITTER = ",\t";

	private DB _db;
	private DBCollection _langlinksCollection;
	private DBCollection _sEntityCollection;
	private DBCollection _tEntityCollection;

	private Language sLang;
	private Language tLang;

	private File sourceFile;
	private File targetFile;

	public MongoLangLinksIndexer(String dir, String sLangLabel, String tLangLabel) throws UnknownHostException {
		sLang = Language.getLanguage(sLangLabel);
		tLang = Language.getLanguage(tLangLabel);
		File[] files = new File(dir).listFiles();

		for (File file : files) {
			if (file.getName().equals("langlinks_" + sLang.getLabel() + ".csv"))
				sourceFile = file;
			else if (file.getName().equals("langlinks_" + tLang.getLabel() + ".csv"))
				targetFile = file;
		}

		_db = MongoResource.INSTANCE.getDB();
		_langlinksCollection = _db.getCollection(DBConstants.LANGLINKS_COLLECTION);
		_sEntityCollection = _db.getCollection(DBConstants.ENTITY_COLLECTION + sLang.getLabel());
		_tEntityCollection = _db.getCollection(DBConstants.ENTITY_COLLECTION + tLang.getLabel());
	}

	// "configs/configuration_esa.properties" "/home/ls3data/users/lzh/langlinks/201605" "en" "zh"
	public static void main(String[] args) throws IOException {

		double start = System.currentTimeMillis();
		try {
			String configPath = args[0];
			Property.setProperties(configPath);

			MongoLangLinksIndexer langlinksIndex = new MongoLangLinksIndexer(args[1], args[2], args[3]);
			langlinksIndex.index();

			MongoResource.INSTANCE.finalizing();
		} catch (Exception e) {
			e.printStackTrace();
		}
		double end = System.currentTimeMillis();
		System.out.println("The total time in min: " + (end - start) / 1000 / 60);
	}

	public void index() throws Exception {
		if (sourceFile != null)
			index(sourceFile, tLang.getLabel(), false);
		if (targetFile != null)
			index(targetFile, sLang.getLabel(), true);

		_langlinksCollection
				.createIndex(new BasicDBObject(DBConstants.LANGLINKS_SOURCE_ID, 1).append(DBConstants.LANGLINKS_SOURCE_LANGUAGE, 1));
		_langlinksCollection
				.createIndex(new BasicDBObject(DBConstants.LANGLINKS_TARGET_ID, 1).append(DBConstants.LANGLINKS_TARGET_LANGUAGE, 1));
	}

	public void index(File file, String lang, boolean reverse) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));

		int i = 0;
		String line;
		while ((line = br.readLine()) != null) {
			if (++i % 100000 == 0)
				System.out.println(i + " langlinks have been processed.");
			String[] splits = line.split(SPLITTER);

			if (splits.length != 3 || !splits[1].trim().equals(lang))
				continue;

			String tTitle = splits[2].trim();
			if (tTitle.equals(""))
				continue;

			int sId = Integer.parseInt(splits[0]);

			insert(sId, tTitle, reverse);
		}

		br.close();
	}

	private void insert(int sId, String tTitle, boolean reverse) {
		DBObject sObj;
		DBObject tObj;
		String sLabel;
		String tLabel;
		if (!reverse) {
			sObj = _sEntityCollection.findOne(new BasicDBObject(DBConstants.ENTITY_ID, sId));
			tObj = _tEntityCollection.findOne(new BasicDBObject(DBConstants.ENTITY_NAME, tTitle));
			sLabel = sLang.getLabel();
			tLabel = tLang.getLabel();
		} else {
			sObj = _tEntityCollection.findOne(new BasicDBObject(DBConstants.ENTITY_ID, sId));
			tObj = _sEntityCollection.findOne(new BasicDBObject(DBConstants.ENTITY_NAME, tTitle));
			sLabel = tLang.getLabel();
			tLabel = sLang.getLabel();
		}
		if (null != sObj && null != tObj) {
			BasicDBObject dbObject = new BasicDBObject(DBConstants.LANGLINKS_SOURCE_ID, sObj.get(DBConstants.ENTITY_ID))
					.append(DBConstants.LANGLINKS_SOURCE_ENTITY, sObj.get(DBConstants.ENTITY_NAME))
					.append(DBConstants.LANGLINKS_SOURCE_LANGUAGE, sLabel)
					.append(DBConstants.LANGLINKS_TARGET_ID, tObj.get(DBConstants.ENTITY_ID))
					.append(DBConstants.LANGLINKS_TARGET_ENTITY, tObj.get(DBConstants.ENTITY_NAME))
					.append(DBConstants.LANGLINKS_TARGET_LANGUAGE, tLabel);
			_langlinksCollection.insert(dbObject);
		}
	}

}