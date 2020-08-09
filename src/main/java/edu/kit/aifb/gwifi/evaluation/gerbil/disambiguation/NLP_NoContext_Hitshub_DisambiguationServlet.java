package edu.kit.aifb.gwifi.evaluation.gerbil.disambiguation;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;

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
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import edu.kit.aifb.gwifi.service.NoContextDisambiguationService;
import edu.kit.aifb.gwifi.service.ServiceUtil;
import edu.kit.aifb.gwifi.service.Service.DisambiguationModel;
import edu.kit.aifb.gwifi.util.Position;
import edu.kit.aifb.gwifi.util.nlp.Language;

/**
 * Servlet implementation class ConfigurationServlet
 */
public class NLP_NoContext_Hitshub_DisambiguationServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(NLP_NoContext_Hitshub_DisambiguationServlet.class);

	public final static String ITEM_TAG = "item";
	public final static String TEXT_TAG = "text";
	public final static String MENTION_TAG = "mention";
	public final static String MENTION_POSITION_TAG = "position";
	public final static String MENTION_LENGTH_TAG = "length";

	private final static String DISAMBIGUATION_SERVICE = "no_context_hitshub_disambiguation_service";

	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		String configPath = getServletContext().getInitParameter("configPath");
		String inputLang = getServletContext().getInitParameter("inputLanguage");
		String outputLang = getServletContext().getInitParameter("outputLanguage");
		String kb = getServletContext().getInitParameter("KB");
		String NLPModel = getServletContext().getInitParameter("NLPModel");

		try {
			NoContextDisambiguationService service = (NoContextDisambiguationService) getServletContext()
					.getAttribute(DISAMBIGUATION_SERVICE);
			if (service == null) {
				service = new NoContextDisambiguationService(configPath + "hub-template.xml",
						configPath + "wikipedia-template-" + inputLang + ".xml", configPath + "NLPConfig.properties",
						Language.getLanguage(inputLang), Language.getLanguage(outputLang), ServiceUtil.getKB(kb),
						ServiceUtil.getNLPModel(NLPModel), DisambiguationModel.HITSHUB, null, null, null);
				getServletContext().setAttribute(DISAMBIGUATION_SERVICE, service);
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
		try {
			/*
			 * the input xml structure: <item> <text> ... </text> <mention position= ... length = .../> ... <item>
			 * 
			 * E.g., <item> <text> But with age and the growing acceptance of medical marijuana, his parents were
			 * curious. </text> <mention position="43" length="7"/> <mention position="66" length="7"/> </item>
			 */
			Document inputDoc = readXML(request);
			HashSet<Position> positions = new HashSet<Position>();
			String text = extractTextAndMentions(inputDoc, positions);
			logger.debug("text: " + text);
			logger.debug("posisionts: " + positions);

			NoContextDisambiguationService service = (NoContextDisambiguationService) getServletContext()
					.getAttribute(DISAMBIGUATION_SERVICE);
			String xmlResponse = service.disambiguate(text, positions, null);
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

	public String extractTextAndMentions(Document doc, Set<Position> positions) {
		NodeList textNodeList = doc.getElementsByTagName(TEXT_TAG);
		String text = textNodeList.item(0).getFirstChild().getNodeValue();
		NodeList mentionNodeList = doc.getElementsByTagName(MENTION_TAG);
		for (int i = 0; i < mentionNodeList.getLength(); i++) {
			Element ele = (Element) mentionNodeList.item(i);
			int start = Integer.valueOf(ele.getAttribute(MENTION_POSITION_TAG));
			int length = Integer.valueOf(ele.getAttribute(MENTION_LENGTH_TAG));
			positions.add(new Position(start, start + length));
		}
		return text;
	}
}
