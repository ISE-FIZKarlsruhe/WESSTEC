package edu.kit.aifb.gwifi.mingyuzuo;

import java.io.File;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import edu.kit.aifb.gwifi.evaluation.kbp.Constants;
import edu.kit.aifb.gwifi.mingyuzuo.IsChineseOrNot;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.mongo.DBConstants;
import edu.kit.aifb.gwifi.mongo.MongoResource;
import edu.kit.aifb.gwifi.mongo.Property;
import edu.kit.aifb.gwifi.util.nlp.LabelNormalizer;
import edu.kit.aifb.gwifi.util.nlp.Language;

/**
 * This class is responsible to build the collection named "LabelIndex_**" in MongoDB
 * 
 */
public class MongoLabelForGoogleSearchIndexer {

	private DBCollection _zhdbCollection;
	private DBCollection _endbCollection;
	private Wikipedia _wikipedia;
	private LabelNormalizer _normalizer;

	private DBCollection _queryCollection;
	private DBCollection _zhgoogleCollection;
	private DBCollection _engoogleCollection;
	
	private Language _lang;

	public MongoLabelForGoogleSearchIndexer(String dbDir, String langLabel, String year) throws Exception {
		_lang = Language.getLanguage(langLabel);
		_zhdbCollection = MongoResource.INSTANCE.getDB().getCollection(DBConstants.LABEL_COLLECTION + "zh");
		_endbCollection = MongoResource.INSTANCE.getDB().getCollection(DBConstants.LABEL_COLLECTION + "en");
//		_zhgoogleCollection = MongoResource.INSTANCE.getDB().getCollection("googleForTest_zh_" + year);
//		_engoogleCollection = MongoResource.INSTANCE.getDB().getCollection("googleForTest_en_" + year);
		
		_zhgoogleCollection = MongoResource.INSTANCE.getDB().getCollection("googleForTest_allQuery_zh_" + year);
		_engoogleCollection = MongoResource.INSTANCE.getDB().getCollection("googleForTest_allQuery_en_" + year);
		
		_queryCollection = MongoResource.INSTANCE.getDB().getCollection(Constants.EVAL_QUERIES_2013);
		_wikipedia = new Wikipedia(new File(dbDir), false);
		_normalizer = new LabelNormalizer(_lang);
	}

	// "configs/MongoConfig_gwifi.properties" "configs/wikipedia-template-zh.xml" "zh" "2013"
	public static void main(String[] args) {
		try {
			String configPath = args[0];
			Property.setProperties(configPath);
			MongoLabelForGoogleSearchIndexer indexer = new MongoLabelForGoogleSearchIndexer(args[1], args[2], args[3]);
			indexer.insertLabel();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void insertLabel() throws Exception {
		DBCursor curqueries = _queryCollection.find();
		int i = 0;
		
		boolean isChinese;
		
		while (curqueries.hasNext()) {
			DBObject curobj = curqueries.next();
			String labelText = curobj.get(Constants.QUERIES_NAME).toString();
			String nLabelText = null;
			isChinese = IsChineseOrNot.isChinese(labelText);
			if(isChinese)
			{
				nLabelText = _normalizer.normalize(labelText);
			}
//			BasicDBObject query = new BasicDBObject();
//			query.put(DBConstants.LABEL_NORM_TEXT, nLabelText);
//			
//			DBObject curobjLabel = null;
//			isChinese = IsChineseOrNot.isChinese(nLabelText);
//			if(isChinese)
//			{
//				curobjLabel = _zhdbCollection.findOne(query);
//			}else{
//				curobjLabel = _endbCollection.findOne(query);
//			}
//			
			
//			if(curobjLabel == null) {
				BasicDBObject dbObject = new BasicDBObject(DBConstants.LABEL_TEXT, labelText)
						.append(DBConstants.LABEL_NORM_TEXT, nLabelText)
						.append(DBConstants.LABEL_SOURCE, DBConstants.SOURCE_GOOGLE_SEARCH);
				if(isChinese)
				{
					_zhgoogleCollection.insert(dbObject);
				}else{
					_engoogleCollection.insert(dbObject);
				}
				
				i++;
				System.out.println(i+"\t"+labelText+"\t"+nLabelText);
//			}
		}	
//		_dbCollection.createIndex(new BasicDBObject(DBConstants.LABEL_TEXT, 1));
//		_dbCollection.createIndex(new BasicDBObject(DBConstants.LABEL_NORM_TEXT, 1));
//		_dbCollection.createIndex(new BasicDBObject(DBConstants.LABEL_SOURCE, 1));
	}
}
