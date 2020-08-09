package edu.kit.aifb.gwifi.hsa;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;

import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.util.WikipediaConfiguration;

public class CreateTestArticle {
	public static void main(String[] args) throws Exception{
		
		MongoClient conn_wiki = new MongoClient("aifb-ls3-remus.aifb.kit.edu");
		DB db_wiki = conn_wiki.getDB("wiki");
		
		DBCollection langlinks = db_wiki.getCollection("langlinks");
		
		File databaseDirectory_en = new File("/slow/users/lzh/yang/configs/wikipedia-template-en.xml");
		WikipediaConfiguration conf_en = new WikipediaConfiguration(databaseDirectory_en);
		
		File databaseDirectory_zh = new File("/slow/users/lzh/yang/configs/wikipedia-template-zh.xml");
		WikipediaConfiguration conf_zh = new WikipediaConfiguration(databaseDirectory_zh);
		
		//TextProcessor textProcessor = new TextFolder();
		conf_en.setDefaultTextProcessor(null);
		conf_zh.setDefaultTextProcessor(null);
		
		Wikipedia wikipedia_en = new Wikipedia(conf_en, false);
		Wikipedia wikipedia_zh = new Wikipedia(conf_zh, false);
		
		System.out.println("The Wikipedia environment has been initialized.");

		OutputStreamWriter out_en = new OutputStreamWriter(
				new FileOutputStream("output/article_en.out"), "UTF-8");
		OutputStreamWriter out_zh = new OutputStreamWriter(
				new FileOutputStream("output/article_zh.out"), "UTF-8");
		OutputStreamWriter out_entity = new OutputStreamWriter(
				new FileOutputStream("output/entity.out"), "UTF-8");
		
		DBCursor cur_links = langlinks.find(new BasicDBObject("s_lang", "en").append("t_lang", "zh"), 
				             new BasicDBObject() ).addOption(com.mongodb.Bytes.QUERYOPTION_NOTIMEOUT);
		
		int count = 1;
		while(cur_links.hasNext()){
			DBObject dbo = cur_links.next();
			int id_en = (Integer)dbo.get("s_id");
			int id_zh = (Integer)dbo.get("t_id");
			
			String title_en = (String)dbo.get("s_title");
			title_en = title_en.replaceAll("\\s", "_");
			
			String title_zh = (String)dbo.get("t_title");
			
			Article article_en = wikipedia_en.getArticleById(id_en);
			Article article_zh = wikipedia_zh.getArticleById(id_zh);
			
			String text_en = article_en.getPlainText();
			
			if(text_en.length() < 500)
				continue;
			
			String text_zh = article_zh.getPlainText();
			
			text_en = text_en.replaceAll("\r\n|\r|\n|\n\r", " ");
			text_zh = text_zh.replaceAll("\r\n|\r|\n|\n\r", " ");
			
			String s_en =  id_en + "\t" + title_en + "\t" + text_en + "\n";
			String s_zh =  id_zh + "\t" + title_zh + "\t" + text_zh + "\n";
			
			out_en.write(s_en);
			out_zh.write(s_zh);
			
			out_en.flush();
			out_zh.flush();
			
			String markup_en = article_en.getMarkup();
			String markup_zh = article_zh.getMarkup();
			
			Matcher m_en = Pattern.compile("((?<=\\u005B\\u005B).*?(?=\\u005D\\u005D))").matcher(markup_en);
			Matcher m_zh = Pattern.compile("((?<=\\u005B\\u005B).*?(?=\\u005D\\u005D))").matcher(markup_zh);
			
			StringBuilder sb = new StringBuilder();
			
			while(m_en.find()){
				String entityName = m_en.group();
				if (entityName.contains("|"))
					entityName = entityName.substring(0, entityName.indexOf("|"));
				if(entityName != null && !entityName.equals("")){
					entityName = entityName.replaceAll("\\s", "_");
					sb.append(entityName + " ");
				}
			}
			
			while(m_zh.find()){
				String entityName = m_zh.group();
				if (entityName.contains("|"))
					entityName = entityName.substring(0, entityName.indexOf("|"));
				if(entityName != null && !entityName.equals("")){
					Article art = wikipedia_zh.getArticleByTitle(entityName);
					if(art == null)
						continue;
					int art_zh_id = art.getId();
					DBCursor cur = langlinks.find(
							new BasicDBObject("t_id", art_zh_id).append("t_lang", "zh").append("s_lang", "en"),
							new BasicDBObject("s_title", 1));
					if (cur.hasNext()) {
						entityName = (String) cur.next().get("s_title");
					} else {
						cur = langlinks.find(
								new BasicDBObject("s_id", art_zh_id).append("s_lang", "zh").append("t_lang", "en"),
								new BasicDBObject("t_title", 1));
						if(cur.hasNext()){
							entityName = (String) cur.next().get("t_title");
						}
						
						else{
							entityName = "";
						}
					}
					if(entityName != null && !entityName.equals("")){
						entityName = entityName.replaceAll("\\s", "_");
						sb.append(entityName + " ");
					}
					cur.close();
				}
			}
			
			String s_entity =  id_en + "\t" + title_en + "\t" + sb.toString() + "\n";
			out_entity.write(s_entity);
			out_entity.flush();
			
			System.out.println(count + " documents has been added!");
			count++;
		}
		cur_links.close();
		out_en.close();
		out_zh.close();
		out_entity.close();
		
		System.out.println("create test article finished!");
	}

}
