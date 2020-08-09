package edu.kit.aifb.gwifi.yinwang;
import java.io.File;
import java.io.PrintWriter;
import java.net.UnknownHostException;


import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;




public class Extractdata {
	
	private Mongo mongo;
	private DB db;
	private DBCollection mycollection;
	private PrintWriter pr;
	
	public  Extractdata(String filename) throws Exception{
		mongoInit();
		pr = new PrintWriter(new File(filename));
	}
	
	
	
	private void mongoInit(){
		try {
			mongo = new Mongo("aifb-ls3-maia.aifb.kit.edu", 19005);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		db = mongo.getDB("KBP");
		mycollection = db.getCollection("weibo_marked");
	}
		private void getDataFromMongodb() throws Exception{
			DBCursor cur = mycollection.find();
			//System.err.print(cur.size());
			int i=1;
			while(cur.hasNext()){
				DBObject dbobj= cur.next();
				String a = "false";
				String parallel = dbobj.get("is_parallel").toString();
				String text = dbobj.get("doc_text").toString();
				//String cg = dbobj.get("category").toString();
				
				//System.out.println(cg);
				if (parallel.equals(a) ){
					
				System.out.println(i + "\t" + "is parallel or not:" + "\t" + parallel + "\t" + "Text:" + "\t" +text  );
				pr.println(i+"   "+"is parallel or not:" + "  " + parallel + "\t" + "Text:" + "\t" +text+ "\r\n"+"\r\n");
				
	       i++;
				}
//	       switch(i){
//			case 278: cur.next(); i++;
//			break;
//			case 334: cur.next(); i++;
//			break;
//			case 1025: cur.next(); i++;
//			break;
//			case 1051: cur.next(); i++;
//			break;
//			case 1054: cur.next(); i++;
//			break;
//		}
	       
	       
    }
			pr.close();		
		 }		
			public static void main(String[] args) throws Exception {

//				String filename = args[0];
				Extractdata c = new Extractdata("C:/Users/Nekromantik/Desktop/markednp.txt");
				c.getDataFromMongodb();
				
			}		
			

		
		
		
		
}