package edu.kit.aifb.gwifi.yinwang;

import java.io.File;
import java.io.PrintWriter;
import java.net.UnknownHostException;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;

import edu.kit.aifb.gwifi.service.NLPAnnotationService;
import edu.kit.aifb.gwifi.service.Service.DisambiguationModel;
import edu.kit.aifb.gwifi.service.Service.KB;
import edu.kit.aifb.gwifi.service.Service.MentionMode;
import edu.kit.aifb.gwifi.service.Service.NLPModel;
import edu.kit.aifb.gwifi.service.Service.RepeatMode;
import edu.kit.aifb.gwifi.service.Service.ResponseMode;
import edu.kit.aifb.gwifi.util.nlp.Language;

public class Weibo {
	
	private NLPAnnotationService service;
	private Mongo mongo;
	private DB db;
	private DBCollection mycollection;
//	private BasicDBObject query;
	private PrintWriter pr;
	
	public Weibo(String filename) throws Exception{
		gwifiInit();
		mongoInit();
		pr = new PrintWriter(new File(filename));
	}
	// Parameter NLPModel.NGRAM/NER/POS
	private void gwifiInit() throws Exception{
//		service = new NGramAnnotationService("configs/hub-template.xml","configs/wikipedia-template-en.xml", "en");
		service = new NLPAnnotationService("configs/hub-template.xml",
				"configs/wikipedia-template-en.xml", "configs/NLPConfig.properties", Language.EN, Language.EN,
				KB.DBPEDIA, NLPModel.NGRAM, DisambiguationModel.PAGERANK, MentionMode.NON_OVERLAPPED, ResponseMode.BEST,
				RepeatMode.FIRST);

	}
	private void mongoInit(){
		try {
			mongo = new Mongo("aifb-ls3-maia.aifb.kit.edu", 19005);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		db = mongo.getDB("KBP");
		mycollection = db.getCollection("weibo");
	}
	
	private void getDataFromMongodb() throws Exception{
		DBCursor cur = mycollection.find();
		System.err.print(cur.size());
		int i=1;
		

//		for(int j = 0; j <= 116 ; j++){
//			cur.next();
//			i = j +2;
//		}
		while(cur.hasNext()){
//			if(i==4347){
//			break;
//		}
			
			DBObject dbobj= cur.next();
			String text = dbobj.get("text").toString();
			String id = dbobj.get("id").toString();
			String dbid = dbobj.get("_id").toString();
			String gwifiResult = callGwifiService(text);
			//System.out.print(title);
			System.err.println(i + "\t" + "this text id is:" + "\t" + id + "\t" + "id in db:" + "\t" +dbid);
			System.out.println(gwifiResult);
			pr.println(i + "\t" + "this text id is:" + "\t" + id + "\t" + "id in db:" + "\t" +dbid);
			pr.println(gwifiResult);
			
			i++;

			switch(i){
				case 119: cur.next(); i++;
				break;
				case 938: cur.next(); i++;
				break;
				case 1868: cur.next(); i++;
				break;
    			case 2643: cur.next(); i++;
    			break;
				case 3292: cur.next(); i++;
				break;
			}
		}
		
		pr.close();
	}

	public String callGwifiService(String text) throws Exception{
		
		return service.annotate(text, null);

		
		}
	public static void main(String[] args) throws Exception {

//		String filename = args[0];
		Weibo s = new Weibo("D:/result/all_en.txt");
		s.getDataFromMongodb();
	}


}
