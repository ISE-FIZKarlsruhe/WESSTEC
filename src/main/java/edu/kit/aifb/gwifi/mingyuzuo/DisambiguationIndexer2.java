package edu.kit.aifb.gwifi.mingyuzuo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Disambiguation;
import edu.kit.aifb.gwifi.model.Page.PageType;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.util.PageIterator;

public class DisambiguationIndexer2 {

	private Wikipedia wikipedia;
	private DB db;
	private DBCollection labelEntityZH;
	private DBCollection LabelEntityIndex_ZH_LC_test;


	// 1. o_id
	// 2. id
	// 3. o_entity
	// 4. entity
	// 5. label
	// 6. s_label???
	// 7. t_label???
	// 8. lc_label
	// 9. source: wiki_label, wiki_disamb, baidu
	public DisambiguationIndexer2(String dbDir) throws Exception {
		wikipedia = new Wikipedia(new File(dbDir), false);
		db = DBBuilder.getDB();
		System.out.println("test1");
		LabelEntityIndex_ZH_LC_test = db.getCollection("LabelEntityIndex_ZH_LC_test");
		System.out.println("test2");
	}

	// "configs/wikipedia-template-en.xml" "en" "configs/configuration_xsearch.properties"
	public static void main(String[] args) {
		try {
			DisambiguationIndexer2 indexer = new DisambiguationIndexer2("configs/wikipedia-template-zh.xml");
			indexer.insertData();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
//		String s = "菲尔・杰克逊";
//		System.out.println(s);
//		StringBuffer sb = new StringBuffer();
//		
//		for (int i = 0; i < s.length(); i++) {
//			char c = s.charAt(i);
//			System.out.println(c + ": " + Character.isIdeographic(c));
//	    }
		System.out.println("done!");
	}

	public void insertData() throws IOException {
		
		labelEntityZH = db.getCollection(DBConstants.LABEL_ENTITY_COLLECTION_ZH_LC);
		DBCursor cursor = labelEntityZH.find();
		save2wikidisamb("o_id", "id", "o_entity", "entity", "label", "s_label", "t_label", "lc_label");
		
		
		while(cursor.hasNext())
		{
			DBObject curobj = cursor.next();
			String label =  curobj.get("label").toString();
			String o_id = curobj.get("id").toString();
			String o_entity = curobj.get("entity").toString();
			String lclabel = label.toLowerCase();
//			Long slinkOccCount = Long.parseLong(curobj.get("slinkOccCount").toString());
//			Long slinkDocCount = Long.parseLong(curobj.get("slinkDocCount").toString());
			if(label.equals(""))
				continue;
			List<String> entityList = getLabelDisambiguation(label);
			for(String entity :entityList)
			{
				String id = getEntityID(entity);
				save2wikidisamb(o_id, id, o_entity, entity, label, label, label, lclabel);
			}
			break;
		}
		
	}

	public List<String> getLabelDisambiguation(String label)
	{
		List<String> entityList = new ArrayList<>();
		
		PageIterator pageIterator = wikipedia.getPageIterator(PageType.disambiguation);

		while (pageIterator.hasNext()) {

			Article article = (Article)pageIterator.next();
			String markup = article.getMarkup();
			
			if(!label.equals(article.getTitle()))
				continue;
			
			if(article instanceof Disambiguation)
			{
				Matcher m = Pattern.compile("((?<=\\u005B\\u005B).*?(?=\\u005D\\u005D))").matcher(markup);
				while (m.find()) {
					String entity = m.group();

					if (entity.contains("|"))
						entity = entity.substring(0, entity.indexOf("|"));
					
					if (entity == null || entity.equals(""))
						continue;

					entityList.add(entity);
				}
			}
		}
		pageIterator.close();
		
		return entityList;
	}
	
	public String getEntityID(String entity)
	{
		BasicDBObject query = new BasicDBObject();
		query.put("entity", entity);
		DBObject curobj = db.getCollection(DBConstants.ENTITY_COLLECTION_ZH).findOne(query);
		return curobj.get("id").toString();
	}
	
	public void save2wikidisamb(String o_id, String id, String o_entity, String entity, String label, String s_label, String t_label, String lc_label)
	{
		DBObject insertData;
		insertData = new BasicDBObject();
		insertData.put("o_id", o_id);
		insertData.put("id", id);		
		insertData.put("o_entity", o_entity);
		insertData.put("entity", entity);
		insertData.put("label", label);
		insertData.put("s_label", s_label);
		insertData.put("t_label", t_label);
		insertData.put("lc_label", lc_label);
		insertData.put("source", "wiki_disamb");
	
		LabelEntityIndex_ZH_LC_test.insert(insertData);
		
	}
	
}
