package edu.kit.aifb.gwifi.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;

import com.sun.org.apache.xerces.internal.dom.DocumentImpl;

import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.annotation.Mention;
import edu.kit.aifb.gwifi.annotation.detection.DisambiguationUtil;
import edu.kit.aifb.gwifi.annotation.detection.NLPTopicDetector;
import edu.kit.aifb.gwifi.annotation.detection.Topic;
import edu.kit.aifb.gwifi.annotation.preprocessing.DocumentPreprocessor;
import edu.kit.aifb.gwifi.annotation.preprocessing.HtmlPreprocessor;
import edu.kit.aifb.gwifi.annotation.preprocessing.PreprocessedDocument;
import edu.kit.aifb.gwifi.annotation.preprocessing.WikiPreprocessor;
import edu.kit.aifb.gwifi.annotation.tagging.DocumentTagger;
import edu.kit.aifb.gwifi.annotation.tagging.HtmlTagger;
import edu.kit.aifb.gwifi.annotation.tagging.WikiTagger;
import edu.kit.aifb.gwifi.annotation.weighting.TopicDisambiguator;
import edu.kit.aifb.gwifi.annotation.weighting.TopicRefiner;
import edu.kit.aifb.gwifi.crosslinking.LanguageLinksSearcher;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.util.Position;
import edu.kit.aifb.gwifi.util.RelatednessCache;
import edu.kit.aifb.gwifi.util.WebContentRetriever;
import edu.kit.aifb.gwifi.util.nlp.Language;

public class NoContextDisambiguationService implements D2WService {

	private static Logger logger = Logger.getLogger(NoContextDisambiguationService.class);

	private static LinkFormat DEFAULT_LINK_FORMAT = LinkFormat.AUTO;
	private static RepeatMode DEFAULT_REPEAT_MODE = RepeatMode.ALL;
	private static ResponseMode DEFAULT_RESPONSE_MODE = ResponseMode.BEST;
	private static MentionMode DEFAULT_MENTION_MODE = MentionMode.NON_OVERLAPPED;
	private static NLPModel DEFAULT_NLP_MODEL = NLPModel.NGRAM;
	private static DisambiguationModel DEFAULT_DISAMBIGUATION_MODEL = DisambiguationModel.PAGERANK;
	private static KB DEFAULT_KB = KB.WIKIPEDIA;
	private static String linkClassName = "wm_wikifiedLink";

	private static Document doc = new DocumentImpl();
	private static DecimalFormat decimalFormat = (DecimalFormat) NumberFormat.getInstance(Locale.US);

	public final static String KB_WIKIPEDIA = "wikipedia";
	public final static String KB_DBPEDIA = "dbpedia";

	private final static String WIKIPEDIA_URL = ".wikipedia.org/wiki/";
	private final static String DBPEDIA_URL = ".dbpedia.org/resource/";

	private Wikipedia wikipedia;
	private DisambiguationUtil disambiguationUtil;
	private NLPTopicDetector topicDetector;
	private TopicDisambiguator topicDisambiguator;
	private TopicRefiner topicRefiner;
	private DocumentPreprocessor preprocessor;
	private LanguageLinksSearcher langlinks;

	private LinkFormat linkFormat;
	private NLPModel nlpModel;
	private DisambiguationModel disambiguationModel;
	private ResponseMode responseMode;
	private MentionMode mentionMode;
	private RepeatMode repeatMode;

	private Language inputLang;
	private Language outputLang;
	private KB kb;

	public static void main(String[] args) throws Exception {

		NoContextDisambiguationService service = new NoContextDisambiguationService("configs/hub-template.xml",
				"configs/wikipedia-template-en.xml", "configs/NLPConfig.properties", Language.EN, Language.EN,
				KB.DBPEDIA, NLPModel.NER, DisambiguationModel.PAGERANK, MentionMode.NON_OVERLAPPED, ResponseMode.BEST,
				RepeatMode.ALL);

		BufferedReader br = new BufferedReader(new FileReader(new File("res/aida-textb/4.txt")));
		StringBuffer sb = new StringBuffer();
		String line;
		while ((line = br.readLine()) != null) {
			sb.append(line).append("\n");
		}
		br.close();
		String xml = sb.toString().trim();

		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		Document inputDoc = docBuilder.parse(new InputSource(new StringReader(xml)));

		HashSet<Position> positions = new HashSet<Position>();
		String text = extractTextAndMentions(inputDoc, positions);

		String result = service.disambiguate(text, positions, null);
		System.out.println(result);

	}

	public NoContextDisambiguationService(String hubconfig, String wikiconfig, String NLPconfig, Language inputLang,
			Language outputLang, KB kb, NLPModel nlpModel, DisambiguationModel disambiguationModel,
			MentionMode mentionMode, ResponseMode responseMode, RepeatMode repeatMode) throws Exception {

		HubConfiguration config = new HubConfiguration(new File(hubconfig));
		langlinks = new LanguageLinksSearcher(config.getLanglinksPath());

		File databaseDirectory = new File(wikiconfig);
		wikipedia = new Wikipedia(databaseDirectory, false);
		disambiguationUtil = new DisambiguationUtil(wikipedia);
		topicDetector = new NLPTopicDetector(wikipedia, disambiguationUtil, NLPconfig, false, false);
		topicDisambiguator = new TopicDisambiguator(disambiguationUtil);
		topicRefiner = new TopicRefiner();

		preprocessor = new WikiPreprocessor(wikipedia);
		
		this.linkFormat = DEFAULT_LINK_FORMAT;

		this.inputLang = inputLang;
		if (outputLang == null)
			this.outputLang = inputLang;
		else
			this.outputLang = outputLang;

		if (kb == null)
			this.kb = DEFAULT_KB;
		else
			this.kb = kb;

		if (nlpModel == null)
			this.nlpModel = DEFAULT_NLP_MODEL;
		else
			this.nlpModel = nlpModel;

		if (disambiguationModel == null)
			this.disambiguationModel = DEFAULT_DISAMBIGUATION_MODEL;
		else
			this.disambiguationModel = disambiguationModel;

		if (mentionMode == null)
			this.mentionMode = DEFAULT_MENTION_MODE;
		else
			this.mentionMode = mentionMode;

		if (responseMode == null)
			this.responseMode = DEFAULT_RESPONSE_MODE;
		else
			this.responseMode = responseMode;

		if (repeatMode == null)
			this.repeatMode = DEFAULT_REPEAT_MODE;
		else
			this.repeatMode = repeatMode;
	}

	public String disambiguate(String source, Set<Position> positions, List<Annotation> annotations) throws Exception {
		if (source == null || source.trim().length() == 0) {
			System.out.println("You must specify a source document to wikify");
		}

		if (annotations == null)
			annotations = new ArrayList<Annotation>();
		String wikifiedDoc = wikifyAndGatherAnnotations(source, positions, annotations);

		// double docScore = 0;
		// for (Topic t : detectedTopics)
		// docScore = docScore + t.getRelatednessToOtherTopics();

		Element xmlResponse = buildXMLResponse(doc, wikifiedDoc, annotations);

		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

		StringWriter writer = new StringWriter();
		StreamResult result = new StreamResult(writer);
		DOMSource dom = new DOMSource(xmlResponse);
		transformer.transform(dom, result);
		return writer.toString();
	}

	public Element buildXMLResponse(Document doc, String wikifiedDoc, Collection<Annotation> annotations)
			throws Exception {
		Element xmlResponse = createElement(doc, RESPONSE_TAG);
		Element xmlWikifiedDoc = createCDATAElement(doc, WIKIFIED_DOC_TAG, wikifiedDoc);
		// xmlWikifiedDoc.setAttribute("sourceMode", sourceMode.toString());
		// xmlWikifiedDoc.setAttribute("documentScore", format(docScore));
		xmlResponse.appendChild(xmlWikifiedDoc);

		Element xmlDetectedTopics = createElement(doc, ANNOS_TAG);
		for (Annotation anno : annotations) {

			// the annotation with weight less than the minimum weight will not
			// be added in the detected topic list
			if (anno.getWeight() <= wikipedia.getConfig().getMinWeight())
				continue;

			Element annoEle = createElement(doc, ANNO_TAG);
			annoEle.setAttribute(ANNO_ID_TAG, String.valueOf(anno.getId()));
			annoEle.setAttribute(ANNO_DISPLAYNAME_TAG, anno.getTitle());
			annoEle.setAttribute(ANNO_URL_TAG, anno.getURL());
			annoEle.setAttribute(ANNO_WEIGHT_TAG, format(anno.getWeight()));

			Element mentionEle = createElement(doc, MENTION_TAG);
			mentionEle.setAttribute(MENTION_LABEL_TAG, String.valueOf(anno.getMention().getTerm()));
			int start = anno.getMention().getPosition().getStart();
			int end = anno.getMention().getPosition().getEnd();
			mentionEle.setAttribute(MENTION_POS_TAG, String.valueOf(start));
			mentionEle.setAttribute(MENTION_LENGTH_TAG, String.valueOf(end - start));

			annoEle.appendChild(mentionEle);

			xmlDetectedTopics.appendChild(annoEle);
		}
		xmlResponse.appendChild(xmlDetectedTopics);

		return xmlResponse;
	}

	private String wikifyAndGatherAnnotations(String source, Set<Position> positions, List<Annotation> annotations)
			throws IOException, Exception {

		if (source == null || source.trim().equals(""))
			return "";

		PreprocessedDocument doc = preprocessor.preprocess(source);
		// for (Article bt: bannedTopicList)
		// doc.banTopic(bt.getId()) ;

		// TODO: find smarter way to resolve this hack, which stops wikifier
		// from detecting "Space (punctuation)" ;
		doc.banTopic(143856);

		RelatednessCache rc = new RelatednessCache(disambiguationUtil.getArticleComparer());
		// retrieve the topics and their references for each different lower-case label text
		// TODO: consider merging the same topics with different label texts
		List<Topic> topics = topicDetector.getTopics(doc, rc, positions);
		topics = topicDisambiguator.getWeightedTopics(topics, rc, disambiguationModel);
		// topics = topicRefiner.getRefinedTopics(topics, rc);

		ArrayList<Topic> detectedTopics = new ArrayList<Topic>();
		for (Topic topic : topics) {
			int id = topic.getId();
			String title = topic.getTitle();
			String displayName = extractCrossDescription(id, title, inputLang, outputLang);
			if (displayName == null || displayName.equals(""))
				continue;
			String uri = getURI(displayName, inputLang, outputLang, kb);
			topic.setURI(uri);
			topic.setDisplayName(displayName);

			// the annotation with weight less than the minimum weight will not
			// be marked in the wikified text
			if (topic.getWeight() >= wikipedia.getConfig().getMinWeight())
				detectedTopics.add(topic);
		}

		DocumentTagger dt = new MyWikiTagger(linkFormat);

		// the overlapped positions not tagged in the wikified text
		Set<Position> overlappedPositions = new HashSet<Position>();
		// the filtered positions not tagged in the wikified text, e.g., there is an annotation already
		// tagged before these positions in the same region
		Set<Position> filteredPositions = new HashSet<Position>();
		String taggedText = dt.tag(doc, detectedTopics, repeatMode, overlappedPositions, filteredPositions);

		Map<Mention, TreeSet<Annotation>> mention2annos = new LinkedHashMap<Mention, TreeSet<Annotation>>();
		for (Topic topic : detectedTopics) {
			for (Position position : topic.getPositions()) {
//				if (mentionMode.equals(MentionMode.NON_OVERLAPPED) && overlappedPositions.contains(position))
//					continue;
//				else if (filteredPositions.contains(position))
//					continue;
//				else {
					String term = source.substring(position.getStart(), position.getEnd());
					Mention mention = new Mention(position, term);
					TreeSet<Annotation> annos = mention2annos.get(mention);
					if (annos == null) {
						annos = new TreeSet<Annotation>();
						mention2annos.put(mention, annos);
					}
					Annotation anno = new Annotation(topic.getId(), topic.getTitle());
					anno.setMention(mention);
					anno.setWeight(topic.getWeight());
					anno.setDisplayName(topic.getDisplayName());
					anno.setURL(topic.getURI());
					annos.add(anno);
//				}
			}
		}

		if (annotations != null) {
			for (Mention mention : mention2annos.keySet()) {
				if(positions.contains(mention.getPosition())) {
					TreeSet<Annotation> annos = mention2annos.get(mention);
					Annotation best = annos.last();
//					if (responseMode.equals(ResponseMode.BEST)) {
						annotations.add(best);
//					} else if (responseMode.equals(ResponseMode.ALL)) {
//						for (Annotation anno : annos) {
//							annotations.add(anno);
//						}
//					} else {
//						annotations.add(best);
//					}
				}
			}
		}

		return taggedText;
	}

	private static SourceMode resolveSourceMode(String source) {

		// try to parse source as url
		try {
			// fix omitted http prefix
			if (source.matches("(?i)^www\\.(.*)$"))
				source = "http://" + source;

			URL url = new URL(source);
			return SourceMode.URL;
		} catch (MalformedURLException e) {
		}
		;

		// count html elements and wiki link elements
		int htmlCount = 0;
		Pattern htmlTag = Pattern.compile("<(.*?)>");
		Matcher m = htmlTag.matcher(source);
		while (m.find())
			htmlCount++;

		int wikiCount = 0;
		Pattern wikiTag = Pattern.compile("\\[\\[(.*?)\\]\\]");
		m = wikiTag.matcher(source);
		while (m.find())
			wikiCount++;

		if (htmlCount > wikiCount)
			return SourceMode.HTML;
		else
			return SourceMode.WIKI;

	}

	public static Element createElement(Document doc, String tagName) {
		return doc.createElement(tagName);
	}

	public static Text createTextNode(Document doc, String data) {
		return doc.createTextNode(data);
	}

	public static Element createCDATAElement(Document doc, String tagName, String data) {
		Element e = doc.createElement(tagName);
		e.appendChild(doc.createCDATASection(data));
		return e;
	}

	public static String format(double number) {
		return decimalFormat.format(number);
	}

	public static class MyHtmlTagger extends HtmlTagger {

		LinkFormat linkFormat;
		String linkStyle;

		public MyHtmlTagger(LinkFormat linkFormat, String linkStyle) {
			this.linkFormat = linkFormat;
			this.linkStyle = linkStyle;
			if (this.linkStyle != null)
				this.linkStyle = this.linkStyle.trim();
		}

		public String getTag(String anchor, Topic topic) {

			String url = topic.getURI();

			if (url == null) {
				return anchor;
			}

			StringBuffer tag = new StringBuffer("<a");
			tag.append(" href=\"" + url + "\"");

			tag.append(" class=\"" + linkClassName + "\"");

			if (linkFormat == LinkFormat.HTML_ID || linkFormat == LinkFormat.HTML_ID_WEIGHT)
				tag.append(" pageId=\"" + topic.getId() + "\"");

			if (linkFormat == LinkFormat.HTML_ID_WEIGHT)
				tag.append(" linkProb=\"" + format(topic.getWeight()) + "\"");

			if (linkStyle != null && linkStyle.length() > 0)
				tag.append(" style=\"" + linkStyle + "\"");

			tag.append(">");
			tag.append(anchor);
			tag.append("</a>");

			return tag.toString();
		}
	}

	public static class MyWikiTagger extends WikiTagger {

		LinkFormat linkFormat;

		public MyWikiTagger(LinkFormat linkFormat) {
			this.linkFormat = linkFormat;
		}

		public String getTag(String anchor, Topic topic) {

			int id = topic.getId();
			String url = topic.getURI();
			String displayName = topic.getDisplayName();

			if (url == null) {
				return anchor;
			}

			StringBuffer tag = new StringBuffer("[[");

			if (linkFormat == LinkFormat.WIKI_ID || linkFormat == LinkFormat.WIKI_ID_WEIGHT) {
				tag.append(id);

				if (linkFormat == LinkFormat.WIKI_ID_WEIGHT) {
					tag.append("|");
					tag.append(format(topic.getWeight()));
				}

				tag.append("|");
				tag.append(anchor);

			} else {

				if (displayName.compareToIgnoreCase(anchor) == 0)
					tag.append(anchor);
				else {
					tag.append(displayName);
					tag.append("|");
					tag.append(anchor);
				}
			}

			tag.append("]]");
			return tag.toString();
		}
	}

	public String extractCrossDescription(int pageId, String title, Language input, Language output) {
		if (input.equals(output))
			return title;
		String displayName = null;
		try {
			if (!input.equals(output)) {
				displayName = langlinks.getDescription(pageId, input.toString(), output.toString());
			}

			return displayName;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String getURI(String title, Language input, Language output, KB kb) {
		String uri = null;
		if (kb.equals(KB.DBPEDIA)) {
			if (output.equals(Language.EN))
				uri = "http://" + DBPEDIA_URL.substring(1) + title.replace(" ", "_");
			else
				uri = "http://" + output.toString() + DBPEDIA_URL + title.replace(" ", "_");
		} else if (kb.equals(KB.WIKIPEDIA)) {
			uri = "http://" + output.toString() + WIKIPEDIA_URL + title.replace(" ", "_");
		} else {
			uri = "http://" + output.toString() + WIKIPEDIA_URL + title.replace(" ", "_");
		}
		return uri;
	}

	public static String readFile(String file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = null;
		StringBuilder stringBuilder = new StringBuilder();
		String ls = System.getProperty("line.separator");

		while ((line = reader.readLine()) != null) {
			stringBuilder.append(line);
			stringBuilder.append(ls);
		}

		reader.close();
		return stringBuilder.toString();
	}

	public DOMSource getDOMSource(String source, String outputLangLabel, String kbLabel) throws Exception {
		return null;
	}

	public final static String ITEM_TAG = "item";
	public final static String TEXT_TAG = "text";
	public final static String MENTION_TAG = "mention";
	public final static String MENTION_POSITION_TAG = "position";
	public final static String MENTION_LENGTH_TAG = "length";

	public static String extractTextAndMentions(Document doc, Set<Position> positions) {
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
