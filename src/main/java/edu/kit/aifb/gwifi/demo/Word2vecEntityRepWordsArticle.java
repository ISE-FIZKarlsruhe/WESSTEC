package edu.kit.aifb.gwifi.demo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;

import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Page;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.model.Page.PageType;
import edu.kit.aifb.gwifi.util.WikipediaConfiguration;
import edu.kit.aifb.gwifi.util.WikipediaTools;

public class Word2vecEntityRepWordsArticle {
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
		
		OutputStreamWriter writer_en = new OutputStreamWriter(
				new FileOutputStream("Entity-Word2vec/article/en_entity.txt"), "UTF-8");
		OutputStreamWriter writer_zh = new OutputStreamWriter(
				new FileOutputStream("Entity-Word2vec/article/zh_entity.txt"), "UTF-8");
		OutputStreamWriter writer_markup_en = new OutputStreamWriter(
				new FileOutputStream("Entity-Word2vec/article/markup_en.txt"), "UTF-8");
		OutputStreamWriter writer_markup_zh = new OutputStreamWriter(
				new FileOutputStream("Entity-Word2vec/article/markup_zh.txt"), "UTF-8");
		Iterator<Page> iter = wikipedia_en.getPageIterator(PageType.article);
		int count = 1;
		while(iter.hasNext()){
			Page p = iter.next();
			String markup_en = p.getMarkup();
			String title = p.getTitle();
			title = title.replaceAll("\\s", "_");
			Integer id = p.getId();
			String markup_temp = markup_en;
			markup_temp = markup_temp.replaceAll("\\s", " ");
			markup_temp = markup_temp.toLowerCase();
			if(markup_en == null || markup_en.trim().equals(""))
				continue;
			Map<String, String> entity2code = new HashMap<String, String>();
			Map<String, String> label2entity = new HashMap<String, String>();
			
			Matcher match = Pattern.compile("((?<=\\u005B\\u005B)[^\\u005B]*?(?=\\u005D\\u005D))").matcher(markup_en);
			while(match.find()){
				String entity = match.group();
				String entityName = entity;
				String labelName = null;
				if(entity.contains("|")){
					entityName = entity.substring(0, entity.indexOf("|"));
					labelName = entity.substring(entity.indexOf("|")+1, entity.length());
				}
				String code = Integer.toString(entityName.hashCode());
				if(!entity2code.containsKey(entityName))
					entity2code.put(entityName, code);
				if(labelName != null){
					if(!label2entity.containsKey(labelName))
						label2entity.put(labelName, entityName);
				}
				if(!label2entity.containsKey(entityName))
					label2entity.put(entityName, entityName);
				markup_temp = markup_temp.replace("[["+entity+"]]", code);
			}
			ArrayList<Map.Entry<String, String>> labellist =
				    new ArrayList<Map.Entry<String, String>>(label2entity.entrySet());
			Collections.sort(labellist,new Comparator<Map.Entry<String,String>>() {
	            public int compare(Entry<String, String> o1,
	                    Entry<String, String> o2) {
	            	String s1 = o1.getKey();
	            	String s2 = o2.getKey();
	            	//return o1.getValue().compareTo(o2.getValue());
	                return s2.length()-s1.length();
	            }
	            
	        });
			
			for(Map.Entry<String,String> mapping:labellist){ 
				markup_temp = markup_temp.replace(" "+mapping.getKey()+" ", " "+entity2code.get(mapping.getValue())+" ");
				//System.out.println(mapping.getKey()+":"+mapping.getValue()); 
	        } 
			for(String entity : entity2code.keySet()){
				markup_temp = markup_temp.replace(entity2code.get(entity), "[["+entity+"]]");
			}
			
			writer_markup_en.write(id+"\t"+title+"\t"+markup_temp+"\n");
			writer_markup_en.flush();
			markup_en = markup_temp;
			match = Pattern.compile("((?<=\\u005B\\u005B)[^\\u005B]*?(?=\\u005D\\u005D))").matcher(markup_en);
			while(match.find()){
				String entity = match.group();
				String label = entity;
				if(entity!=null && !entity.trim().equals("")){
					entity = "_"+ entity.replaceAll("\\s|\\p{P}\\s*", "_");
				}
				markup_temp = markup_temp.replace("[["+label+"]]", entity);
			}
			String plainText_en = WikipediaTools.extractPlainText(markup_temp);
			String s = id + "\t" + title + "\t" + plainText_en + "\n";
			writer_en.write(s);
			writer_en.flush();
			System.out.println("add english articles: "+ count++);
		}
		
		count = 1;
		iter =  wikipedia_zh.getPageIterator(PageType.article);
		while(iter.hasNext()){
			Page p = iter.next();
			String markup_zh = p.getMarkup();
			String title = p.getTitle();
			title = title.replaceAll("\\s", "_");
			Integer id = p.getId();
			String markup_temp = markup_zh;
			markup_temp = markup_temp.replaceAll("\\s", " ");
			markup_temp = markup_temp.toLowerCase();
			if(markup_temp == null || markup_temp.trim().equals(""))
				continue;
			Map<String, String> entity2code = new HashMap<String, String>();
			Map<String, String> label2entity = new HashMap<String, String>();
			
			Matcher match = Pattern.compile("((?<=\\u005B\\u005B)[^\\u005B]*?(?=\\u005D\\u005D))").matcher(markup_zh);
			while(match.find()){
				String entity = match.group();
				String entityName = entity;
				String labelName = null;
				if(entity.contains("|")){
					entityName = entity.substring(0, entity.indexOf("|"));
					labelName = entity.substring(entity.indexOf("|")+1, entity.length());
				}
				String code = Integer.toString(entityName.hashCode());
				if(!entity2code.containsKey(entityName))
					entity2code.put(entityName, code);
				if(labelName != null){
					if(!label2entity.containsKey(labelName))
						label2entity.put(labelName, entityName);
				}
				if(!label2entity.containsKey(entityName))
					label2entity.put(entityName, entityName);
				markup_temp = markup_temp.replace("[["+entity+"]]", code);
			}
			ArrayList<Map.Entry<String, String>> labellist =
				    new ArrayList<Map.Entry<String, String>>(label2entity.entrySet());
			Collections.sort(labellist,new Comparator<Map.Entry<String,String>>() {
	            public int compare(Entry<String, String> o1,
	                    Entry<String, String> o2) {
	            	String s1 = o1.getKey();
	            	String s2 = o2.getKey();
	            	//return o1.getValue().compareTo(o2.getValue());
	                return s2.length()-s1.length();
	            }
	            
	        });
			
			for(Map.Entry<String,String> mapping:labellist){ 
				markup_temp = markup_temp.replace(mapping.getKey(), entity2code.get(mapping.getValue()));
				//System.out.println(mapping.getKey()+":"+mapping.getValue()); 
	        } 
			for(String entity : entity2code.keySet()){
				markup_temp = markup_temp.replace(entity2code.get(entity), "[["+entity+"]]");
			}
			writer_markup_zh.write(id+"\t"+title+"\t"+markup_temp+"\n");
			writer_markup_zh.flush();
			markup_zh = markup_temp;
			match = Pattern.compile("((?<=\\u005B\\u005B)[^\\u005B]*?(?=\\u005D\\u005D))").matcher(markup_zh);
			while(match.find()){
				String entity = match.group();
				String label = entity;	
				
				if(entity != null && !entity.trim().equals("")){
					Article art = wikipedia_zh.getArticleByTitle(entity);
					if(art == null)
						continue;
					int art_zh_id = art.getId();
					DBCursor cur = langlinks.find(
							new BasicDBObject("t_id", art_zh_id).append("t_lang", "zh").append("s_lang", "en"),
							new BasicDBObject("s_title", 1));
					if (cur.hasNext()) {
						entity = (String) cur.next().get("s_title");
					} else {
						cur = langlinks.find(
								new BasicDBObject("s_id", art_zh_id).append("s_lang", "zh").append("t_lang", "en"),
								new BasicDBObject("t_title", 1));
						if(cur.hasNext()){
							entity = (String) cur.next().get("t_title");
						}
						
						//else{
							//entityName = "";
						//}
					}
					if(entity != null && !entity.trim().equals("")){
						entity = "_" + entity.replaceAll("\\s|\\p{P}\\s*", "_");
						entity = entity.toLowerCase();
					}
					cur.close();
				}
				markup_temp = markup_temp.replace("[[" + label + "]]", " "+ entity + " ");
				
			}
			
			String plainText_zh = WikipediaTools.extractPlainText(markup_temp);
			String s = id + "\t" +  title + "\t" + plainText_zh + "\n";
			writer_zh.write(s);
			writer_zh.flush();
			System.out.println("add chinese articles: " + count++);
			
		}
		writer_en.close();
		writer_zh.close();
		writer_markup_en.close();
		writer_markup_zh.close();
	}

}
