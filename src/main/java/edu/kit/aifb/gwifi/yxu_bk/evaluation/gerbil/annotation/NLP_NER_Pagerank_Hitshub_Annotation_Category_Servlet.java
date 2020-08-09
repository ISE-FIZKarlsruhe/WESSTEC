package edu.kit.aifb.gwifi.yxu_bk.evaluation.gerbil.annotation;

import java.io.IOException;
import java.io.StringReader;

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
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import edu.kit.aifb.gwifi.service.ServiceUtil;
import edu.kit.aifb.gwifi.service.Service.DisambiguationModel;
import edu.kit.aifb.gwifi.service.Service.MentionMode;
import edu.kit.aifb.gwifi.service.Service.NLPModel;
import edu.kit.aifb.gwifi.service.Service.RepeatMode;
import edu.kit.aifb.gwifi.service.Service.ResponseMode;
import edu.kit.aifb.gwifi.util.nlp.Language;
import edu.kit.aifb.gwifi.yxu_bk.service.NLPAnnotationCategoryService;

/**
 * Servlet implementation class ConfigurationServlet
 */
public class NLP_NER_Pagerank_Hitshub_Annotation_Category_Servlet extends
		HttpServlet {
	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger
			.getLogger(NLP_NER_Pagerank_Hitshub_Annotation_Category_Servlet.class);

	public final static String ITEM_TAG = "item";
	public final static String TEXT_TAG = "text";
	private final static String ANNOTATION_CATEGORY_SERVICE = "ner_pagerank_hitshub_annotation_category_service";

	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		String configPath = getServletContext().getInitParameter("configPath");
		String inputLang = getServletContext()
				.getInitParameter("inputLanguage");
		String outputLang = getServletContext().getInitParameter(
				"outputLanguage");
		String kb = getServletContext().getInitParameter("KB");

		try {
			// TODO add new service, and set the parameter list for it.
			NLPAnnotationCategoryService service = (NLPAnnotationCategoryService) getServletContext()
					.getAttribute(ANNOTATION_CATEGORY_SERVICE);
			if (service == null) {
				service = new NLPAnnotationCategoryService(configPath
						+ "hub-template.xml", configPath
						+ "wikipedia-template-" + inputLang + ".xml",
						configPath + "NLPConfig.properties",
						Language.getLanguage(inputLang),
						Language.getLanguage(outputLang),
						ServiceUtil.getKB(kb), NLPModel.NER,
						DisambiguationModel.PAGERANK,
						MentionMode.NON_OVERLAPPED, ResponseMode.BEST,
						RepeatMode.ALL);
				getServletContext().setAttribute(ANNOTATION_CATEGORY_SERVICE,
						service);
			}
			logger.debug("The Wikipedia environment for " + inputLang
					+ " has been initialized.");
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		try {
			/*
			 * the input xml structure: <item> <text> ... </text> <item>
			 * 
			 * E.g., <item> <text> But with age and the growing acceptance of
			 * medical marijuana, his parents were curious. </text> </item>
			 */
			Document inputDoc = readXML(request);
			String text = extractText(inputDoc);
			logger.debug("text: " + text);

			// TODO new service
			NLPAnnotationCategoryService service = (NLPAnnotationCategoryService) getServletContext()
					.getAttribute(ANNOTATION_CATEGORY_SERVICE);
			String xmlResponse = service.annotate(text, null);
			logger.debug("output: " + xmlResponse);
			Document outputDoc = readXML(xmlResponse);

			writeXML(outputDoc, response);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerFactoryConfigurationError e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

	public Document readXML(HttpServletRequest request) throws SAXException,
			IOException, ParserConfigurationException {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory
				.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		Document doc = docBuilder.parse(request.getInputStream());
		return doc;
	}

	protected static Document readXML(String xml) throws SAXException,
			IOException, ParserConfigurationException {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory
				.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		Document doc = docBuilder.parse(new InputSource(new StringReader(xml)));
		return doc;
	}

	public void writeXML(Document doc, HttpServletResponse response)
			throws TransformerFactoryConfigurationError, TransformerException,
			IOException {
		response.setContentType("text/xml; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");

		Transformer transformer = TransformerFactory.newInstance()
				.newTransformer();
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");

		StreamResult result = new StreamResult(response.getOutputStream());
		DOMSource source = new DOMSource(doc);
		transformer.transform(source, result);
	}

	public String extractText(Document doc) {
		NodeList textNodeList = doc.getElementsByTagName(TEXT_TAG);
		String text = textNodeList.item(0).getFirstChild().getNodeValue();
		return text;
	}
}
