package edu.kit.aifb.gwifi.mingyuzuo;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreAnnotations.AnswerAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.CoreMap;

public class CheckEntityType
{
	public String entity_type;
	public String checkEntityType(String mention) throws SAXException, IOException
	{		
		String serializedClassifier = "/home/zmy/Library/stanford/NERClassifer/english.all.3class.distsim.crf.ser.gz";

	    AbstractSequenceClassifier classifier = CRFClassifier.getClassifierNoExceptions(serializedClassifier);

//	    classifier.classifyToString(mention_name, "xml", true);
	    
	    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder;
		Document inputDoc =	null;
		try
		{
			docBuilder = docFactory.newDocumentBuilder();
			
			List<List<CoreLabel>> xml = classifier.classify(mention);
			 for (List<CoreLabel> sentence : xml) {
		          for (CoreLabel word : sentence) {
		            System.out.println(word.get(AnswerAnnotation.class));
		          }
		          System.out.println();
		        }
			
			inputDoc = docBuilder.parse(new InputSource(new StringReader(mention)));
		}
		catch (ParserConfigurationException e)
		{
			e.printStackTrace();
		}
	  
		NodeList annoNodeList = inputDoc.getElementsByTagName("wi");
		for (int i = 0; i < annoNodeList.getLength(); i++)
		{
			Element ele = (Element) annoNodeList.item(i);
			String type = ele.getAttribute("entity");

			if(type.equals("ORGANIZATION"))
			{
				entity_type = "ORG";
			}else if(type.equals("LOCATION"))
			{
				entity_type = "GPE";
			}else if(type.equals("PERSON"))
			{
				entity_type = "PER";
			}
		}
		return entity_type;
	}
	
	public static void main(String[] args) throws SAXException, IOException
	{
		CheckEntityType entityType = new CheckEntityType();
		
		System.out.println(entityType.checkEntityType("北京"));
	}
}
