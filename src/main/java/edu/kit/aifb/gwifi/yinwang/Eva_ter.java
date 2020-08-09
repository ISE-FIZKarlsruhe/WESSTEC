package edu.kit.aifb.gwifi.yinwang;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.dom4j.DocumentException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import java.util.HashMap; 
import java.util.Map;
import edu.kit.aifb.gwifi.service.NLPAnnotationService;

public class Eva_ter {
	
	private BufferedReader buf;
	
	private String query1 = "Tour de France Nibali|07.2014";
	private String query2 = "World Cup|07.2014";
	private String query3 = "Germany Brazil|07.2014";
	private String query4 = "Klose|07.2014";
	private String query5 = "Kobe|12.2014";
	private String query6 = "Super Bowl|01.2015";
	private String query7 = "Clooney|09.2014";
	private String query8 = "Sony Kim Jong-un|12.2014";
	private String query9 = "Golden Globe Award|01.2015";
	private String query10 = "Eddie Redmayne Hawking|01.2015";
	private String query11 = "Alibaba|09.2014";
	private String query12 = "Cyber Monday Amazon|11.2014";
	private String query13 = "Singles Day Jack Ma|11.2014";
	private String query14 = "Malaysia Airlines|07.2014";
	private String query15 = "MD-80|07.2014";
	private String query16 = "Indonesia Java|12.2014";
	private String query17 = "Charlie Hebdo|01.2015";
	private String query18 = "Pistorius|09.2014";
	private String query19 = "Tim Cook|10.2014";
	private String query20 = "Shuji Nakamura Nobel Prize|10.2014";
	private String query21 = "Rosetta|11.2014";
	private String query22 = "Donald Tusk|12.2014";
	private HashMap<String, Double> map1 =  new HashMap();
	private Map<String,  Double> map2 =  new HashMap();
	private Map<String,  Double> map3 =  new HashMap();
	private Map<String,  Double> map4 =  new HashMap();
	private Map<String,  Double> map5 =  new HashMap();
	private Map<String,  Double> map6 =  new HashMap();
	private Map<String,  Double> map7 =  new HashMap();
	private Map<String,  Double> map8 =  new HashMap();
	private Map<String,  Double> map9 =  new HashMap();
	private Map<String,  Double> map10 =  new HashMap();
	private Map<String, Double> map11 =  new HashMap();
	private Map<String, Double> map12 =  new HashMap();
	private Map<String, Double> map13 =  new HashMap();
	private Map<String, Double> map14 =  new HashMap();
	private Map<String, Double> map15 =  new HashMap();
	private Map<String, Double> map16 =  new HashMap();
	private Map<String, Double> map17 =  new HashMap();
	private Map<String, Double> map18 =  new HashMap();
	private Map<String, Double> map19 =  new HashMap();
	private Map<String, Double> map20 =  new HashMap();
	private Map<String, Double> map21 =  new HashMap();
	private Map<String, Double> map22 =  new HashMap();
	
	
	public Eva_ter()
	{
		
	}
	
	
	public String readtxtFile(String filename) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(new File(filename)));
		StringBuffer sb = new StringBuffer();
		String line;
		
		while ((line = br.readLine()) != null) {
			sb.append(line).append("\n");
		}
	
		br.close();
		return sb.toString().trim();
		
	}

	
	public String readfile(String filename) throws IOException, DocumentException {
		InputStream is = new FileInputStream(filename);
		buf = new BufferedReader(new InputStreamReader(is));
		
		String line = buf.readLine();
		StringBuilder sb = new StringBuilder();
     
		while (line != null && line.contains("|")) {
			
			switch(line){
			case "Tour de France Nibali|07.2014" : sb.append(line).append("\n");String[] a1 =line.split("^");  double b1 =Double.valueOf(a1[1]).doubleValue();map1.put(a1[0], b1);
			break;
			case "query2" : sb.append(line).append("\n"); line = buf.readLine();String[] a2 =line.split("^"); double b2 =Double.valueOf(a2[1]).doubleValue();map2.put(a2[0], b2);
			break;
			case "query3" : sb.append(line).append("\n"); line = buf.readLine();String[] a3 =line.split("^");  double b3 =Double.valueOf(a3[1]).doubleValue();map3.put(a3[0], b3);
			break;
			case "query4" : sb.append(line).append("\n"); line = buf.readLine();String[] a4 =line.split("^");  double b4 =Double.valueOf(a4[1]).doubleValue();map4.put(a4[0], b4);
			break;
			case "query5" : sb.append(line).append("\n"); line = buf.readLine();String[] a5 =line.split("^");  double b5 =Double.valueOf(a5[1]).doubleValue();map5.put(a5[0], b5);
			break;
			case "query6" : sb.append(line).append("\n"); line = buf.readLine();String[] a6 =line.split("^");  double b6 =Double.valueOf(a6[1]).doubleValue();map6.put(a6[0], b6);
			break;
			case "query7" : sb.append(line).append("\n"); line = buf.readLine();String[] a7 =line.split("^");  double b7 =Double.valueOf(a7[1]).doubleValue();map7.put(a7[0], b7);
			break;
			case "query8" : sb.append(line).append("\n"); line = buf.readLine();String[] a8 =line.split("^");  double b8 =Double.valueOf(a8[1]).doubleValue();map8.put(a8[0], b8);
			break;
			case "query9" : sb.append(line).append("\n"); line = buf.readLine();String[] a9 =line.split("^");  double b9 =Double.valueOf(a9[1]).doubleValue();map9.put(a9[0], b9);
			break;
			case "query10" : sb.append(line).append("\n"); line = buf.readLine();String[] a10 =line.split("^");  double b10 =Double.valueOf(a10[1]).doubleValue();map10.put(a10[0], b10);
			break;
			case "query11" : sb.append(line).append("\n"); line = buf.readLine();String[] a11 =line.split("^");  double b11 =Double.valueOf(a11[1]).doubleValue();map11.put(a11[0], b11);
			break;
			case "query12" : sb.append(line).append("\n"); line = buf.readLine();String[] a12 =line.split("^");  double b12 =Double.valueOf(a12[1]).doubleValue();map12.put(a12[0], b12);
			break;
			case "query13" : sb.append(line).append("\n"); line = buf.readLine();String[] a13 =line.split("^");  double b13 =Double.valueOf(a13[1]).doubleValue();map13.put(a13[0], b13);
			break;
			case "query14" : sb.append(line).append("\n"); line = buf.readLine();String[] a14 =line.split("^");  double b14 =Double.valueOf(a14[1]).doubleValue();map14.put(a14[0], b14);
			break;
			case "query15" : sb.append(line).append("\n"); line = buf.readLine();String[] a15 =line.split("^");  double b15 =Double.valueOf(a15[1]).doubleValue();map15.put(a15[0], b15);
			break;
			case "query16" : sb.append(line).append("\n"); line = buf.readLine();String[] a16 =line.split("^");  double b16 =Double.valueOf(a16[1]).doubleValue();map16.put(a16[0], b16);
			break;
			case "query17" : sb.append(line).append("\n"); line = buf.readLine();String[] a17 =line.split("^");  double b17 =Double.valueOf(a17[1]).doubleValue();map17.put(a17[0], b17);
			break;
			case "query18" : sb.append(line).append("\n"); line = buf.readLine();String[] a18 =line.split("^");  double b18 =Double.valueOf(a18[1]).doubleValue();map18.put(a18[0], b18);
			break;
			case "query19" : sb.append(line).append("\n"); line = buf.readLine();String[] a19 =line.split("^");  double b19 =Double.valueOf(a19[1]).doubleValue();map19.put(a19[0], b19);
			break;
			case "query20" : sb.append(line).append("\n"); line = buf.readLine();String[] a20 =line.split("^");  double b20 =Double.valueOf(a20[1]).doubleValue();map20.put(a20[0], b20);
			break;
			case "query21" :sb.append(line).append("\n"); line = buf.readLine();String[] a21 =line.split("^");  double b21 =Double.valueOf(a21[1]).doubleValue();map21.put(a21[0], b21);
			break;
			case "query22" : sb.append(line).append("\n"); line = buf.readLine();String[] a22 =line.split("^");  double b22 =Double.valueOf(a22[1]).doubleValue();map22.put(a22[0], b22);
			break;
			}
			
			
			sb.append(line).append("\n"); 
			line = buf.readLine();
			
			
		}
//while (line != null) {
//	if (line.contains(query1)) {
//			
//			sb.append(line).append("\n");
//			line = buf.readLine();
//			String[] a =line.split("^");
//			Map<String, String> map2 = new HashMap<String, String>();
//			map2.put(a[0], a[1]);
//		}
		
		
		
		System.out.println("query1"+"\n"+map1+"query2"+"\n"+map2+"query3"+"\n"+map3+"query4"+"\n"+map4+"query5"+"\n"+map5);
		String fileAsString = sb.toString();
//		System.out.println("Contents : " + fileAsString);
		return fileAsString;
	}


	

	
	
	
//	public String[] getEntities(String str)
//	{
//		String[] a = str.split("^");
//		return a;
//	}
	
	
	
	
	
	public static void main(String argv[])
			throws IOException, SAXException, ParserConfigurationException, DocumentException {
		String filePath = "C:/Users/Nekromantik/Desktop/txt/evater/all.txt";
		Eva_ter r1 = new Eva_ter();
		String c = r1.readfile(filePath);
		
		System.err.println("Done!!!!!!");
	}
	
	
}
