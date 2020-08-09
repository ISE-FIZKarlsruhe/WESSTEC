package edu.kit.aifb.gwifi.servlet;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import edu.kit.aifb.gwifi.service.NLPAnnotationService;
import edu.kit.aifb.gwifi.service.Service.DisambiguationModel;
import edu.kit.aifb.gwifi.service.Service.KB;
import edu.kit.aifb.gwifi.service.Service.MentionMode;
import edu.kit.aifb.gwifi.service.Service.NLPModel;
import edu.kit.aifb.gwifi.service.Service.RepeatMode;
import edu.kit.aifb.gwifi.service.Service.ResponseMode;
import edu.kit.aifb.gwifi.util.nlp.Language;

/**
 * Servlet implementation class ConfigurationServlet
 */
public class NLPAnnotationServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(NLPAnnotationServlet.class);

	private final static String ANNOTATION_SERVICE = "annotation_service";
	
	private final static String PARAM_SOURCE = "source";

	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		String configPath = getServletContext().getInitParameter("configPath");
		String inputLangLabel = getServletContext().getInitParameter("inputLanguage");
		String outputLangLabel = getServletContext().getInitParameter("outputLanguage");
		String kbLabel = getServletContext().getInitParameter("KB");
		String nlpModelLabel = getServletContext().getInitParameter("NLPModel");
		String disambiguationModelLabel = getServletContext().getInitParameter("disambiguationModel");
		String mentionModeLabel = getServletContext().getInitParameter("mentionMode");
		String responseModeLabel = getServletContext().getInitParameter("responseMode");
		String repeatModeLabel = getServletContext().getInitParameter("repeatMode");

		Language inputLang = Language.getLanguage(inputLangLabel);
		Language outputLang;
		if (outputLangLabel != null)
			outputLang = Language.getLanguage(outputLangLabel);
		else
			outputLang = inputLang;

		KB kb;
		if (kbLabel == null)
			kb = null;
		else if (kbLabel.toLowerCase().equals("wikipedia"))
			kb = KB.WIKIPEDIA;
		else if (kbLabel.toLowerCase().equals("dbpedia"))
			kb = KB.DBPEDIA;
		else
			kb = null;

		NLPModel nlpModel;
		if (nlpModelLabel == null)
			nlpModel = null;
		else if (nlpModelLabel.toLowerCase().equals("ngram"))
			nlpModel = NLPModel.NGRAM;
		else if (nlpModelLabel.toLowerCase().equals("pos"))
			nlpModel = NLPModel.POS;
		else if (nlpModelLabel.toLowerCase().equals("ner"))
			nlpModel = NLPModel.NER;
		else
			nlpModel = null;
		
		DisambiguationModel disambiguationModel;
		if (disambiguationModelLabel == null)
			disambiguationModel = null;
		else if (disambiguationModelLabel.toLowerCase().equals("pagerank"))
			disambiguationModel = DisambiguationModel.PAGERANK;
		else if (disambiguationModelLabel.toLowerCase().equals("hitshub"))
			disambiguationModel = DisambiguationModel.HITSHUB;
		else if (disambiguationModelLabel.toLowerCase().equals("prior"))
			disambiguationModel = DisambiguationModel.PRIOR;
		else
			disambiguationModel = null;

		MentionMode mentionMode;
		if (mentionModeLabel == null)
			mentionMode = null;
		else if (mentionModeLabel.toLowerCase().equals("non-overlapped"))
			mentionMode = MentionMode.NON_OVERLAPPED;
		else if (mentionModeLabel.toLowerCase().equals("overlapped"))
			mentionMode = MentionMode.OVERLAPPED;
		else
			mentionMode = null;

		ResponseMode responseMode;
		if (responseModeLabel == null)
			responseMode = null;
		else if (responseModeLabel.toLowerCase().equals("best"))
			responseMode = ResponseMode.BEST;
		else if (responseModeLabel.toLowerCase().equals("all"))
			responseMode = ResponseMode.ALL;
		else
			responseMode = null;
		
		RepeatMode repeatMode;
		if (repeatModeLabel == null)
			repeatMode = null;
		else if (repeatModeLabel.toLowerCase().equals("all"))
			repeatMode = RepeatMode.ALL;
		else if (repeatModeLabel.toLowerCase().equals("first"))
			repeatMode = RepeatMode.FIRST;
		else if (repeatModeLabel.toLowerCase().equals("first_in_region"))
			repeatMode = RepeatMode.FIRST_IN_REGION;
		else
			repeatMode = null;
		
		try {
			NLPAnnotationService service = (NLPAnnotationService) getServletContext().getAttribute(ANNOTATION_SERVICE);
			if (service == null) {
				service = new NLPAnnotationService(configPath + "hub-template.xml",
						configPath + "wikipedia-template-" + inputLang + ".xml", configPath + "NLPConfig.properties",
						inputLang, outputLang, kb, nlpModel, disambiguationModel, mentionMode, responseMode, repeatMode);
				getServletContext().setAttribute(ANNOTATION_SERVICE, service);
			}
			logger.debug("The Wikipedia environment for " + inputLang + " has been initialized.");
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String source = null;
		
		try {
			@SuppressWarnings("unchecked")
			Map<String, String[]> parameterMap = request.getParameterMap();

			for (String parameter : parameterMap.keySet()) {
				if (parameter.equals(PARAM_SOURCE)) {
					String[] values = parameterMap.get(PARAM_SOURCE);
					StringBuffer sb = new StringBuffer();
					for (String value : values) {
						sb.append(" " + value);
					}
					source = sb.toString().trim();
				} 
			}
			
			NLPAnnotationService service = (NLPAnnotationService) getServletContext().getAttribute(ANNOTATION_SERVICE);
			String xmlResponse = service.annotate(source, null);
			logger.debug("output: " + xmlResponse);
			Document outputDoc = readXML(xmlResponse);

			writeXML(outputDoc, response);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	public Document readXML(HttpServletRequest request) throws SAXException, IOException, ParserConfigurationException {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		Document doc = docBuilder.parse(request.getInputStream());
		return doc;
	}

	protected static Document readXML(String xml) throws SAXException, IOException, ParserConfigurationException {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		Document doc = docBuilder.parse(new InputSource(new StringReader(xml)));
		return doc;
	}

	public void writeXML(Document doc, HttpServletResponse response)
			throws TransformerFactoryConfigurationError, TransformerException, IOException {
		response.setContentType("text/xml; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");

		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");

		StreamResult result = new StreamResult(response.getOutputStream());
		DOMSource source = new DOMSource(doc);
		transformer.transform(source, result);
	}
	
}
