package edu.kit.aifb.gwifi.demo;

import java.awt.List;
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
import com.mongodb.DBObject;
import com.mongodb.MongoClient;

import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Page;
import edu.kit.aifb.gwifi.model.Page.PageType;

import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.util.WikipediaConfiguration;
import edu.kit.aifb.gwifi.util.WikipediaTools;

public class LDAEntityRepWordsArticle {
	/*
	public static boolean inSquare(int[] indexOfSquare, int index){
		int mark=0;
		for(int i=0; i<indexOfSquare.length; i++){
			if(index<indexOfSquare[i]){
				mark = i;
				break;
			}
		}
		return mark%2==1;
	}*/
	public static void main(String[] args) throws Exception {
		/*
		String s = "[[United Kingdom|British]], [[[[United States|American]], [[Canada under British rule (1763–1867)|Canadian]]]], "
				+ "[[[[New Spain|Mexican]] and other [[Hispanic America|Spanish American]] british, american business, Canadian correspondence]]";
		String markup_temp = s;
		Map<String, String> entity2code = new HashMap<String, String>();
		Map<String, String> label2entity = new HashMap<String, String>();
		Matcher match = Pattern.compile("((?<=\\u005B\\u005B)[^\\u005B]*?(?=\\u005D\\u005D))").matcher(s);
		//Matcher match = Pattern.compile("((?<=\\u005B\\u005B)[^\\u005B]*?(?=\\u005D\\u005D))").matcher(markup_en);
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
				else{
					if(!label2entity.containsKey(entityName))
						label2entity.put(entityName, entityName);
				}
				markup_temp = markup_temp.replace("[["+entity+"]]", code);
			}
			ArrayList<Map.Entry<String, String>> labellist =
				    new ArrayList<Map.Entry<String, String>>(label2entity.entrySet());
			Collections.sort(labellist,new Comparator<Map.Entry<String,String>>() {
	            public int compare(Entry<String, String> o1,
	                    Entry<String, String> o2) {
	            	String s1 = o1.getKey();
	            	String s2 = o2.getKey();
	            	//return o1.getKey().compareTo(o2.getKey());
	                return s2.length()-s1.length();
	            }
	            
	        });
	        
	        for(Map.Entry<String,String> mapping:labellist){ 
				markup_temp = markup_temp.replaceAll("(?i)"+mapping.getKey(), entity2code.get(mapping.getValue()));
				//System.out.println(mapping.getKey()+":"+mapping.getValue()); 
	        } 
			for(String entity : entity2code.keySet()){
				markup_temp = markup_temp.replace(entity2code.get(entity), "[["+entity+"]]");
			}
			System.out.println(markup_temp);
			/*
		while(match.find()){
			int start = match.start();
			System.out.println(start);
			int end = match.end();
			System.out.println(end);
			String entity = match.group();
			String entityName = entity;
			String labelName = null;
			if(entity.contains("|")){
				entityName = entity.substring(0, entity.indexOf("|"));
				labelName = entity.substring(entity.indexOf("|")+1, entity.length());
				//labelName = labelName.replaceAll("\\s|\\p{P}\\s*|\r\n|\n\r", " ");
				System.out.println(entityName);
				System.out.println(labelName);
			}
			//entityName = entityName.replace("\\s|\\p{P}\\s*|\r\n|\n\r", " ");
			if(entityName != null && !entityName.trim().equals(""))
				markup_temp = markup_temp.replace("[["+entity+"]]", "[["+entityName+"]]");
			if(labelName != null && !labelName.equals(entityName) && !labelName.trim().equals("")){
				//String entity_temp = entity.replaceAll("\\s|\\p{P}\\s*|\r\n|\n\r", " ");
				markup_temp = markup_temp.replace(" "+entityName+" ", " [["+entityName+"]] ");
				markup_temp = markup_temp.replace(" "+labelName+" ", " [["+entityName+"]] ");
			}
		
		}
		System.out.println(markup_temp);
		*/
		
		MongoClient conn_wiki = new MongoClient("aifb-ls3-remus.aifb.kit.edu");
		DB db_wiki = conn_wiki.getDB("wiki");
		DBCollection langlinks = db_wiki.getCollection("langlinks");
		
		File databaseDirectory_en = new File("/slow/users/lzh/yang/configs/wikipedia-template-en.xml");
		WikipediaConfiguration conf_en = new WikipediaConfiguration(databaseDirectory_en);
		File databaseDirectory_zh = new File("/slow/users/lzh/yang/configs/wikipedia-template-zh.xml");
		WikipediaConfiguration conf_zh = new WikipediaConfiguration(databaseDirectory_zh);

		// TextProcessor textProcessor = new TextFolder();
		conf_en.setDefaultTextProcessor(null);
		conf_zh.setDefaultTextProcessor(null);
		
		Wikipedia wikipedia_en = new Wikipedia(conf_en, false);
		Wikipedia wikipedia_zh = new Wikipedia(conf_zh, false);
		
		System.out.println("The Wikipedia environment has been initialized.");
		
		OutputStreamWriter writer_en = new OutputStreamWriter(
				new FileOutputStream("EntityLDA/article/article_en.txt"), "UTF-8");
		OutputStreamWriter writer_zh = new OutputStreamWriter(
				new FileOutputStream("EntityLDA/article/article_zh.txt"), "UTF-8");
		OutputStreamWriter writer_entity = new OutputStreamWriter(
				new FileOutputStream("EntityLDA/article/entity.txt"), "UTF-8");
		
		OutputStreamWriter writer_markup_en = new OutputStreamWriter(
				new FileOutputStream("EntityLDA/article/markup_en.txt"), "UTF-8");
		
		OutputStreamWriter writer_markup_zh = new OutputStreamWriter(
				new FileOutputStream("EntityLDA/article/markup_zh.txt"), "UTF-8");
		
		DBCursor cur_links = langlinks.find(new BasicDBObject("s_lang", "en").append("t_lang", "zh"), 
	             new BasicDBObject() ).addOption(com.mongodb.Bytes.QUERYOPTION_NOTIMEOUT);
		
		//Iterator<Page> iter = wikipedia_en.getPageIterator(PageType.article);
		int count = 1;
		while(cur_links.hasNext()){
			DBObject dbo = cur_links.next();
			int id_en = (Integer)dbo.get("s_id");
			int id_zh = (Integer)dbo.get("t_id");
			
			String title_en = (String)dbo.get("s_title");
			title_en = title_en.replaceAll("\\s", "_");
			
			String title_zh = (String)dbo.get("t_title");
			title_zh = title_zh.replaceAll("\\s", "_");
			
			Article article_en = wikipedia_en.getArticleById(id_en);
			Article article_zh = wikipedia_zh.getArticleById(id_zh);
			
			if(article_en == null || article_zh == null)
				continue;
			String text_en = article_en.getPlainText();
			
			if(text_en.length() < 500)
				continue;
			
			String text_zh = article_zh.getPlainText();
			
			//text_en = text_en.replaceAll("\r\n|\r|\n|\n\r", " ");
			//text_zh = text_zh.replaceAll("\r\n|\r|\n|\n\r", " ");
			
			text_en = text_en.replaceAll("\\s", " ");
			text_en = text_en.toLowerCase();
			text_zh = text_zh.replaceAll("\\s", " ");
			text_zh = text_zh.toLowerCase();
			
			String s_en =  id_en + "\t" + title_en + "\t" + text_en + "\n";
			String s_zh =  id_zh + "\t" + title_zh + "\t" + text_zh + "\n";
			
			writer_en.write(s_en);
			writer_zh.write(s_zh);
			
			String markup_en = article_en.getMarkup();
			String markup_temp = markup_en;
			//markup_temp = markup_temp.replaceAll("\\s|\\p{P}\\s*|\r\n|\n\r", " ");
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
			writer_markup_en.write(id_en+"\t"+title_en+"\t"+markup_temp+"\n");
			writer_markup_en.flush();
			
			StringBuilder sb = new StringBuilder();
			match = Pattern.compile("((?<=\\u005B\\u005B)[^\\u005B]*?(?=\\u005D\\u005D))").matcher(markup_temp);
			while(match.find()){
				String entity = match.group();
				if(entity != null && !entity.trim().equals("")){
					entity = entity.replaceAll("\\s", "_");
					sb.append(entity + " ");
				}
			}
			
			entity2code.clear();
			label2entity.clear();
			labellist.clear();
			
			String markup_zh = article_zh.getMarkup();
			markup_temp = markup_zh;
			//markup_temp = markup_temp.replaceAll("\\s|\\p{P}\\s*|\r\n|\n\r", " ");
			markup_temp = markup_temp.replaceAll("\\s", " ");
			markup_temp = markup_temp.toLowerCase();
			match = Pattern.compile("((?<=\\u005B\\u005B)[^\\u005B]*?(?=\\u005D\\u005D))").matcher(markup_zh);
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
		    labellist = new ArrayList<Map.Entry<String, String>>(label2entity.entrySet());
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
			
			writer_markup_zh.write(id_zh+"\t"+title_zh+"\t"+markup_temp+"\n");
			writer_markup_zh.flush();
			
			match = Pattern.compile("((?<=\\u005B\\u005B)[^\\u005B]*?(?=\\u005D\\u005D))").matcher(markup_temp);
			while(match.find()){
				String entity = match.group();
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
						
					}
					if(entity != null && !entity.trim().equals("")){
						entity = entity.replaceAll("\\s", "_");
						entity = entity.toLowerCase();
						sb.append(entity + " ");
					}
					cur.close();
				}
			}
			String s_entity = id_en + "\t" + title_en + "\t" + sb.toString() + "\n";
			writer_entity.write(s_entity);
			writer_entity.flush();
			System.out.println("add "+ count++ + " articles!");
		}
		
		cur_links.close();
		writer_en.close();
		writer_zh.close();
		writer_entity.close();	
		writer_markup_en.close();
		writer_markup_zh.close();
	}

}
