package edu.kit.aifb.gwifi.xlime;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class xLiMeTool {

	private final static String ITEMS = "items";
	private final static String ITEM = "item";
	private final static String ID = "_id";
	public xLiMeTool(){
		//do nothing
	}

	private static int extractIds(Document doc,
			Collection<String> ids) {
		if(ids==null) return 1;
		NodeList itemsNodeList = doc.getElementsByTagName(ITEMS);
		Element itemsEle = (Element) itemsNodeList.item(0);
		NodeList itemNodeList = itemsEle.getElementsByTagName(ITEM);
		Element itemEle;
		String itemId;
		for (int i = 0; i < itemNodeList.getLength(); i++) {
			itemEle = (Element) itemNodeList.item(i);
			itemId = itemEle.getAttribute(ID);
			ids.add(itemId);
		}
		return 0;
	}

	private static Document readXML(String xml) throws SAXException,
			IOException, ParserConfigurationException {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory
				.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		Document doc = docBuilder.parse(new InputSource(new StringReader(xml)));
		return doc;
	}
	
	//count number of the common ids in files
	//filepath1 filepath2 filepath3...
	//"F:\project\text_categorization\xlime_social_en_1000001-1250000.xml" "F:\project\text_categorization\xlime_social_en_1000001-1250000_.xml"
	private static void printInfo(Map<String, Set<String>> file2Ids){
		if(file2Ids==null || file2Ids.size()==0) return;
		Set<String> commonIds = new HashSet<String>();
		commonIds.addAll(file2Ids.values().iterator().next());
		for(Set<String> ids: file2Ids.values()){
			commonIds.retainAll(ids);
		}
		System.out.println("number of the common ids: "+commonIds.size());
		for(String file: file2Ids.keySet()){
			Set<String> fileIds = file2Ids.get(file);
			System.out.println("number of the different ids in:");
			System.out.println(file+" : "+(fileIds.size()-commonIds.size()));
		}
	}
	
	public static void main(String[] args) throws Exception {
		Map<String, Set<String>> file2Ids = new HashMap<String, Set<String>>();
		for(int i=0; i<args.length;i++){
			byte[] file = Files.readAllBytes(Paths.get(args[i]));
			String xml = new String(file, StandardCharsets.UTF_8);
			Set<String> ids = new HashSet<String>();
			extractIds(readXML(xml), ids);
			file2Ids.put(args[i],ids);
		}
		printInfo(file2Ids);

	}

}
