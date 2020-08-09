package edu.kit.aifb.gwifi.extraction;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.* ;
import java.io.*;
import javax.xml.stream.*;

import org.apache.log4j.*;

import edu.kit.aifb.gwifi.model.Page.PageType;

/**
 * @author David Milne
 *
 * Parses the markup of a &gt;page&lt; element from a mediawiki dump, to convert it into a DumpPage object.
 */
public class DumpPageParser {
	
	private Logger log = Logger.getLogger(DumpPageParser.class);


	private XMLInputFactory xmlStreamFactory = XMLInputFactory.newInstance() ;

	private enum DumpTag {page, id, title, text, timestamp, ignorable} ;
	
	private LanguageConfiguration languageConfiguration ;
	private SiteInfo siteInfo ;
	
	//private Pattern redirectPattern ; 
	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'") ;
	

	public DumpPageParser(LanguageConfiguration lc, SiteInfo si) {
		this.languageConfiguration = lc ;
		this.siteInfo = si ;

	}

	public DumpPage parsePage(String markup) throws XMLStreamException {

		Integer id = null ;
		String title = null ;
		String text = null ;
		Date lastEdited = null ;
		StringBuffer characters = new StringBuffer() ;

		XMLStreamReader xmlStreamReader = xmlStreamFactory.createXMLStreamReader(new StringReader(markup)) ;

		while (xmlStreamReader.hasNext()) {

			int eventCode = xmlStreamReader.next();

			switch (eventCode) {
			case XMLStreamReader.START_ELEMENT :
				break;
			case XMLStreamReader.END_ELEMENT :

				switch(resolveDumpTag(xmlStreamReader.getLocalName())) {

				case id:
					//only take the first id (there is a 2nd one for the revision) 
					if (id == null) 
						id = Integer.parseInt(characters.toString().trim()) ;
					break ;
				case title:
					title = characters.toString().trim() ;
					break ;
				case text:
					text = characters.toString().trim() ;
					break ;
				case timestamp:
					try {
						lastEdited = dateFormat.parse(characters.toString().trim()) ;
					} catch (ParseException e) {
						lastEdited = null ;
					}
					break ;
				}

				characters = new StringBuffer() ;

				break;
			case XMLStreamReader.CHARACTERS :
				characters.append(xmlStreamReader.getText()) ;
			}
		}
		xmlStreamReader.close();

		if (id == null || title == null || text == null) 
			throw new XMLStreamException("Could not parse xml markup for page") ;
		
		
		//identify namespace - assume 0 (main) if there is no prefix, or if prefix doesn't match any known namespaces
		Integer namespaceKey = 0 ;
		int pos = title.indexOf(":") ;
		if (pos > 0) {
			String namespace = title.substring(0, pos) ;
			namespaceKey = siteInfo.getNamespaceKey(namespace) ;
			
			if (namespaceKey == null) 
				namespaceKey = 0 ;
			else 
				title = title.substring(pos+1) ;	
		}
		
		
		//ignore anything that isn't in main, category or template namespace
		if (namespaceKey != SiteInfo.CATEGORY_KEY && namespaceKey != SiteInfo.MAIN_KEY && namespaceKey != SiteInfo.TEMPLATE_KEY) {
			Logger.getLogger(DumpPageParser.class).info("Ignoring page " + id + ":" + title) ;
			return null ;
		}
		
		//identify page type ;
		PageType type ;
		String redirectTarget = null ;
		
		if (namespaceKey == SiteInfo.CATEGORY_KEY) {
			type = PageType.category ;
		} else if (namespaceKey == SiteInfo.TEMPLATE_KEY) {
			type = PageType.template ;
		} else {
			type = PageType.article ;
			
			Matcher m = languageConfiguration.getDisambiguationPattern().matcher(text) ;
			if (m.find()) {
				type = PageType.disambiguation ;
			}
			
			m = languageConfiguration.getRedirectPattern().matcher(text) ;
			if (m.find()) {
				type = PageType.redirect ;
				
				if (m.group(2) != null)
					redirectTarget = m.group(2) ;
				else
					redirectTarget = m.group(3) ;
			}
		}
		
		return new DumpPage(id, namespaceKey, type, title, text, redirectTarget, lastEdited) ;
		
	}


	private DumpTag resolveDumpTag(String tagName) {

		try {
			return DumpTag.valueOf(tagName) ;
		} catch (IllegalArgumentException e) {
			return DumpTag.ignorable ;
		}
	}

}
