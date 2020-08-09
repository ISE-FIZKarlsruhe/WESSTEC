package edu.kit.aifb.gwifi.yinwang;

import java.io.File;
import java.io.PrintWriter;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;

public class ExtractAidaconelltestb {
	private Mongo mongo;
	private DB db;
	private DBCollection mycollection;
	private PrintWriter pr;
	private Map<String, Integer> cate2num;

	public ExtractAidaconelltestb(String filename) throws Exception {
		mongoInit();
		pr = new PrintWriter(new File(filename));
		cate2num = new HashMap<String, Integer>();
	}

	private void mongoInit() {
		try {
			mongo = new Mongo("aifb-ls3-remus.aifb.kit.edu", 19010);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		db = mongo.getDB("congDB");
		mycollection = db.getCollection("aidaConelltestb");
	}

	private void getDataFromMongodb() throws Exception {
		DBCursor cur = mycollection.find();
		// System.err.print(cur.size());
		int i = 1;
		while (cur.hasNext()) {
			DBObject dbobj = cur.next();
			String id = dbobj.get("_id").toString();
			String name = dbobj.get("name").toString();
			String cg = dbobj.get("category").toString();

			// System.out.println(cg);
			System.out.println(i + "\t" + "this text name is:" + " " + name + "\t" + "id in db:" + " " + id + "\t"
					+ "this category is:" + " " + cg);
			pr.println(i + "\t" + "this text name is:" + "\t" + name + "\t" + "this category is:" + "\t" + cg);

			String[] cateNames = cg.split(",");
			for(int j = 0; j < cateNames.length; j++) {
				Integer num = cate2num.get(cateNames[j]);
				if(num == null) {
					num = 0;
				} 
				cate2num.put(cateNames[j], num+1);
			}
			
			i++;
			switch (i) {
			case 222:
				cur.next();
				i++;
				break;
			}
		}
		
		cate2num = sortByValue(cate2num);
		pr.println();
		for(String cate : cate2num.keySet()) {
			pr.println(cate + ": " + cate2num.get(cate));
		}
		
		pr.close();
	}

	public static Map<String, Integer> sortByValue(Map<String, Integer> map) {
		List<Map.Entry<String, Integer>> list = new LinkedList<>(map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
			public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
				return (o2.getValue()).compareTo(o1.getValue());
			}
		});

		Map<String, Integer> result = new LinkedHashMap<>();
		for (Map.Entry<String, Integer> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}
	
	public static void main(String[] args) throws Exception {

		// String filename = args[0];
		ExtractAidaconelltestb d = new ExtractAidaconelltestb("res/aidaConelltestb.txt");
		d.getDataFromMongodb();

	}

}