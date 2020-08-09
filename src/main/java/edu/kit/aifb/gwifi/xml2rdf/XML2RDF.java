package edu.kit.aifb.gwifi.xml2rdf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class XML2RDF {
	
	public static void main(String[] args) throws Exception {		
		
//		String s = "test\\\"s\\";
//		System.out.println(s);
//		System.out.println(escapeCharacters(s));
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setValidating(false);
		DocumentBuilder db = dbf.newDocumentBuilder();
		
		Document doc = db.parse(new FileInputStream(new File(args[0])));
		
		NodeList itemEntries = doc.getElementsByTagName("item");
		
		FileWriter fw = new FileWriter(new File(args[1]));
		
		fw.write("@prefix sioc: <http://rdfs.org/sioc/ns#> .\n");	
		fw.write("@prefix dcterms: <http://purl.org/dc/terms/> .\n");	
		fw.write("@prefix xlime: <http://xlime.eu/> .\n");	
		fw.write("@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n");
//		fw.write("@prefix purl: <http://purl.org/ontology/po/> .\n");
		
		
		fw.write("\n");
		
		for (int i = 0; i < itemEntries.getLength(); i++) {
			Element itemNode = (Element) itemEntries.item(i);
					
			String id = escapeCharacters(itemNode.getAttribute("_id"));	
			String cid = escapeCharacters(itemNode.getAttribute("cid"));	
			String hostname = escapeCharacters(itemNode.getAttribute("hostname"));
			if (!id.contains("http://")) id = "http://xlime.eu/" + id;
			id = id.replace("vico-research.com/social", "xlime.eu");
			String created = escapeCharacters(itemNode.getAttribute("created"));
			String creator = escapeCharacters(itemNode.getAttribute("creator"));
			String lang = escapeCharacters(itemNode.getAttribute("lang")).replace("eng", "en");
			String publisher = escapeCharacters(itemNode.getAttribute("publisher"));
			String source = escapeCharacters(itemNode.getAttribute("source"));
			String sourceType = escapeCharacters(itemNode.getAttribute("sourceType")).replace(" ", "_");
			String content = escapeCharacters(itemNode.getElementsByTagName("content").item(0).getTextContent());
			content = content.substring(0, Math.min(content.length(), 50));
			
			if (created != null && !created.equals("")) {
				LocalDateTime datetime = LocalDateTime.parse(created, DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
				created = datetime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).replace(" ", "T");
			} else {
				created = escapeCharacters(itemNode.getAttribute("serialized-date"));
			}
			
			if (source == null || source.equals("")) {
				source = escapeCharacters(itemNode.getAttribute("uri"));
			} 
						
			fw.write("<" + id + ">\n");			
			if (sourceType != null && !sourceType.equals("")) fw.write("\ta\tsioc:" + sourceType + ";\n");		
			if (created != null && !created.equals("")) fw.write("\tdcterms:created\t\"" + created + "\"^^xsd:dateTime;\n");
			if (lang != null && !lang.equals("")) fw.write("\tdcterms:language\t\"" + lang + "\"^^xsd:language;\n");
			if (publisher != null && !publisher.equals("")) fw.write("\tdcterms:publisher\t<" + publisher + ">;\n");
			if (source != null && !source.equals("")) fw.write("\tdcterms:source\t<" + source + ">;\n");
			if (content != null && !content.trim().equals("")) fw.write("\tsioc:content\t\"" + content + "\"^^xsd:string;\n");
			if (creator != null && !creator.equals("")) fw.write("\tsioc:has_creator\t<" + creator + ">;\n");			
			if (hostname != null && !hostname.equals("")) {
				fw.write("\tdcterms:publisher\t<http://" + hostname + ">;\n");
				fw.write("\ta\txlime:news;\n");
			}
			if (id.length()==24) {
				if (cid != null && !cid.equals("")) fw.write("\txlime:channel\t\"" + cid + "\"^^xsd:string;\n");
				fw.write("\ta\txlime:asr;\n");
			}
			
			
			NodeList category = itemNode.getElementsByTagName("Category");
			for (int j = 0; j < category.getLength(); j++) {
				Element categoryNode = (Element) category.item(j);
				
				String hasConfidence = categoryNode.getAttribute("weight");
				String hasEntity = categoryNode.getAttribute("URL");	
				fw.write("\txlime:hasCategoryAnnotation [\n");
				fw.write("\t\txlime:hasConfidence\t\"" + hasConfidence + "\"^^xsd:double;\n");
				fw.write("\t\txlime:hasCategory\t<" + hasEntity.replaceAll("http://en.wikipedia.org/wiki/Category:","http://dbpedia.org/category/") + ">;\n");
				fw.write("\t] ;\n");
			}
						
			NodeList annotations = itemNode.getElementsByTagName("Annotation");
			for (int j = 0; j < annotations.getLength(); j++) {
				Element annotationNode = (Element) annotations.item(j);
				
				String hasConfidence = annotationNode.getAttribute("weight");
				String hasEntity = annotationNode.getAttribute("URL");	
				fw.write("\txlime:hasEntityAnnotation [\n");
				fw.write("\t\txlime:hasConfidence\t\"" + hasConfidence + "\"^^xsd:double;\n");
				fw.write("\t\txlime:hasEntity\t<" + hasEntity.replaceAll("http://en.wikipedia.org/wiki","http://dbpedia.org/resource") + ">;\n");
				
				NodeList mentions = annotationNode.getElementsByTagName("mention");				
				for (int k = 0; k < mentions.getLength(); k++) {					
					Element mentionsNode = (Element) mentions.item(k);				
					String hasStartPosition = mentionsNode.getAttribute("position");
					String hasLength = mentionsNode.getAttribute("length");
					
					fw.write("\t\txlime:hasPosition [\n");
					fw.write("\t\t\txlime:hasStartPosition\t\"" + hasStartPosition + "\"^^xsd:long;\n");
					fw.write("\t\t\txlime:hasEndPosition\t\"" + (Long.parseLong(hasStartPosition) + Long.parseLong(hasLength)) + "\"^^xsd:long\n");
					fw.write("\t\t]\n");
				}
				
				if (j + 1 == annotations.getLength()) {
					fw.write("\t] .\n");
				} else {
					fw.write("\t] ;\n");
				}				
			}
		}
		fw.close();
	}
	
	public static String escapeCharacters(String s) {		
		// return s.replaceAll("\"", "\\\\\"");
		// return s.replaceAll("[A-Za-z0-9\\s]\"", "$0\\\\\"");
		if (s == null || s.equals("")) return "";
		
		return s.charAt(s.length() - 1) == '\\' ? s.replaceAll("\\\\\"", "\"").replaceAll("\"", "\\\\\"") + " " : s.replaceAll("\\\\\"", "\"").replaceAll("\"", "\\\\\"");
	}	
	
}