package edu.kit.aifb.gwifi.xlime;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import com.sun.org.apache.xerces.internal.dom.DocumentImpl;

import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.annotation.Mention;
import edu.kit.aifb.gwifi.annotation.detection.DisambiguationUtil;
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
import edu.kit.aifb.gwifi.comparison.text.TextComparer;
import edu.kit.aifb.gwifi.crosslinking.LanguageLinksSearcher;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.service.A2WService;
import edu.kit.aifb.gwifi.service.HubConfiguration;
import edu.kit.aifb.gwifi.util.Position;
import edu.kit.aifb.gwifi.util.RelatednessCache;
import edu.kit.aifb.gwifi.util.WebContentRetriever;
import edu.kit.aifb.gwifi.util.WikipediaConfiguration;
import edu.kit.aifb.gwifi.util.nlp.Language;
import edu.kit.aifb.gwifi.util.text.CaseFolder;
import edu.kit.aifb.gwifi.yxu.annotation.detection.NLPTopicDetector;
import edu.kit.aifb.gwifi.yxu.annotation.detection.TopicCategory;

public class NLPAnnotationService implements A2WService {

	private static Logger logger = Logger.getLogger(NLPAnnotationService.class);

	private static boolean DEFAULT_TOOLTIPS = true;
	private static SourceMode DEFAULT_SOURCE_MODE = SourceMode.WIKI;
	private static LinkFormat DEFAULT_LINK_FORMAT = LinkFormat.AUTO;
	private static RepeatMode DEFAULT_REPEAT_MODE = RepeatMode.FIRST;
	private static ResponseFormat DEFAULT_RESPONSE_FORMAT = ResponseFormat.XML;
	private static ResponseMode DEFAULT_RESPONSE_MODE = ResponseMode.BEST;
	private static MentionMode DEFAULT_MENTION_MODE = MentionMode.NON_OVERLAPPED;
	private static NLPModel DEFAULT_NLP_MODEL = NLPModel.POS;
	private static DisambiguationModel DEFAULT_DISAMBIGUATION_MODEL = DisambiguationModel.PAGERANK;
	private static KB DEFAULT_KB = KB.DBPEDIA;
	private static String linkClassName = "wm_wikifiedLink";

	private static Document doc = new DocumentImpl();
	private static DecimalFormat decimalFormat = (DecimalFormat) NumberFormat.getInstance(Locale.US);

	public final static String KB_WIKIPEDIA = "wikipedia";
	public final static String KB_DBPEDIA = "dbpedia";

	private final static String WIKIPEDIA_URL = ".wikipedia.org/wiki/";
	private final static String DBPEDIA_URL = ".dbpedia.org/resource/";

	public final static String ITEMS_TAG = "items";
	public final static String ITEM_TAG = "item";
	public final static String TEXT_TAG = "text";
	public final static String DB_ID_KEY = "_id";
	public final static String DB_CONTENT_KEY = "content";

	private Wikipedia wikipedia;
	private DisambiguationUtil disambiguationUtil;
	private NLPTopicDetector topicDetector;
	private TopicDisambiguator topicDisambiguator;
	private TextComparer searcher;
	private TopicRefiner topicRefiner;
	private WebContentRetriever retriever;
	private DocumentPreprocessor preprocessor;
	private LanguageLinksSearcher langlinks;

	private SourceMode sourceMode;
	private LinkFormat linkFormat;
	private ResponseFormat responseFormat;
	private NLPModel nlpModel;
	private DisambiguationModel disambiguationModel;
	private ResponseMode responseMode;
	private MentionMode mentionMode;
	private RepeatMode repeatMode;

	private Language inputLang;
	private Language outputLang;
	private KB kb;

	public NLPAnnotationService(String hubconfig, String wikiconfig, String NLPconfig, Language inputLang,
			Language outputLang, KB kb, NLPModel nlpModel, DisambiguationModel disambiguationModel,
			MentionMode mentionMode, ResponseMode responseMode, RepeatMode repeatMode) throws Exception {

		HubConfiguration config = new HubConfiguration(new File(hubconfig));
		langlinks = new LanguageLinksSearcher(config.getLanglinksPath());

		File databaseDirectory = new File(wikiconfig);
		// if(inputLang.equals(Language.EN)) {
		// WikipediaConfiguration conf = new WikipediaConfiguration(databaseDirectory);
		// CaseFolder textProcessor = new CaseFolder();
		// conf.setDefaultTextProcessor(textProcessor);
		// wikipedia = new Wikipedia(conf, false);
		// } else {
		wikipedia = new Wikipedia(databaseDirectory, false);
		// }

		disambiguationUtil = new DisambiguationUtil(wikipedia);
		topicDetector = new NLPTopicDetector(wikipedia, disambiguationUtil, searcher, NLPconfig, false, false);
		topicDisambiguator = new TopicDisambiguator(disambiguationUtil);
		topicRefiner = new TopicRefiner();

		this.sourceMode = DEFAULT_SOURCE_MODE;
		this.linkFormat = DEFAULT_LINK_FORMAT;
		this.responseFormat = DEFAULT_RESPONSE_FORMAT;

		this.inputLang = inputLang;
		if (outputLang == null)
			this.outputLang = inputLang;
		else
			this.outputLang = outputLang;

		// searcher = new MongoCLESASearcher(inputLang.getLabel(), outputLang.getLabel());

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

	public Language getLanguage() {
		return this.inputLang;
	}

	public void setWikiDocumentPreprocessor() {
		preprocessor = new WikiPreprocessor(wikipedia);
	}

	public Document getDoc() {
		return this.doc;
	}

	public Element annotateContent(String id, String content, Map<String, String> attributes) throws Exception {
		if (content == null || content.trim().length() == 0) {
			System.out.println("You must specify a source document to wikify");
		}
		SourceMode sourceMode;
		LinkFormat linkFormat;
		ResponseFormat responseFormat;
		if (this.sourceMode == SourceMode.AUTO)
			sourceMode = resolveSourceMode(content);
		else
			sourceMode = this.sourceMode;
		if (this.linkFormat == LinkFormat.AUTO) {
			if (sourceMode == SourceMode.WIKI)
				linkFormat = LinkFormat.WIKI;
			else
				linkFormat = LinkFormat.HTML_ID_WEIGHT;
		} else
			linkFormat = this.linkFormat;
		if (sourceMode == SourceMode.URL)
			responseFormat = ResponseFormat.DIRECT;
		else
			responseFormat = this.responseFormat;

		List<Annotation> annotations = new ArrayList<Annotation>();
		Element xmlItem = null;
		if (responseFormat == ResponseFormat.DIRECT) {
			String wikifiedDoc = wikifyAndGatherAnnotations(content, annotations, sourceMode, linkFormat);
			if (annotations.size() > 0)
				xmlItem = buildXMLItem(doc, id, content, wikifiedDoc, attributes, null);
		} else if (responseFormat == ResponseFormat.XML) {
			String wikifiedDoc = wikifyAndGatherAnnotations(content, annotations, sourceMode, linkFormat);
			if (annotations.size() > 0)
				xmlItem = buildXMLItem(doc, id, content, wikifiedDoc, attributes, annotations);
		}

		return xmlItem;
	}

	public Element buildXMLItem(Document doc, String id, String content, String wikifiedDoc,
			Map<String, String> attributes, Collection<Annotation> annotations) throws Exception {
		Element xmlItem = createElement(doc, ITEM_TAG);
		xmlItem.setAttribute(DB_ID_KEY, id);
		if (attributes != null) {
			for (String key : attributes.keySet()) {
				xmlItem.setAttribute(key, attributes.get(key));
			}
		}
		Element xmlContent = createCDATAElement(doc, DB_CONTENT_KEY, content);
		xmlItem.appendChild(xmlContent);
		if (wikifiedDoc == null)
			return xmlItem;
		Element xmlWikifiedDoc = createCDATAElement(doc, WIKIFIED_DOC_TAG, wikifiedDoc);
		xmlItem.appendChild(xmlWikifiedDoc);
		if (annotations == null)
			return xmlItem;
		Element xmlDetectedTopics = createAnnosElement(doc, ANNOS_TAG, annotations);
		xmlItem.appendChild(xmlDetectedTopics);
		return xmlItem;
	}

	public Element buildXMLResponse(Document doc, String wikifiedDoc, Collection<Annotation> annotations,
			Collection<TopicCategory> categories) throws Exception {
		Element xmlResponse = createElement(doc, RESPONSE_TAG);
		Element xmlWikifiedDoc = createCDATAElement(doc, WIKIFIED_DOC_TAG, wikifiedDoc);
		xmlResponse.appendChild(xmlWikifiedDoc);
		Element xmlDetectedTopics = createAnnosElement(doc, ANNOS_TAG, annotations);
		xmlResponse.appendChild(xmlDetectedTopics);
		return xmlResponse;
	}

	protected Element createAnnosElement(Document doc, String tagName, Collection<Annotation> annotations) {
		Element annosEle = createElement(doc, ANNOS_TAG);
		for (Annotation anno : annotations) {
			Element annoEle = createElement(doc, ANNO_TAG);
			annoEle.setAttribute(ANNO_ID_TAG, String.valueOf(anno.getId()));
			annoEle.setAttribute(ANNO_DISPLAYNAME_TAG, anno.getDisplayName());
			annoEle.setAttribute(ANNO_URL_TAG, anno.getURL());
			annoEle.setAttribute(ANNO_WEIGHT_TAG, format(anno.getWeight()));
			Element mentionEle = createElement(doc, MENTION_TAG);
			mentionEle.setAttribute(MENTION_LABEL_TAG, String.valueOf(anno.getMention().getTerm()));
			int start = anno.getMention().getPosition().getStart();
			int end = anno.getMention().getPosition().getEnd();
			mentionEle.setAttribute(MENTION_POS_TAG, String.valueOf(start));
			mentionEle.setAttribute(MENTION_LENGTH_TAG, String.valueOf(end - start));
			annoEle.appendChild(mentionEle);
			annosEle.appendChild(annoEle);
		}
		return annosEle;
	}

	private String wikifyAndGatherAnnotations(String source, List<Annotation> annotations, SourceMode sourceMode,
			LinkFormat linkFormat) throws IOException, Exception {

		if (source == null || source.trim().equals(""))
			return "";

		String linkStyle = "";

		String markup;
		if (sourceMode == SourceMode.URL) {

			if (source.matches("(?i)^www\\.(.*)$"))
				source = "http://" + source;

			URL url = new URL(source);

			markup = retriever.getWebContent(url);
		} else {
			markup = source;
		}

		PreprocessedDocument doc = preprocessor.preprocess(markup);
		// for (Article bt: bannedTopicList)
		// doc.banTopic(bt.getId()) ;

		// TODO: find smarter way to resolve this hack, which stops wikifier
		// from detecting "Space (punctuation)" ;
		doc.banTopic(143856);

		RelatednessCache rc = new RelatednessCache(disambiguationUtil.getArticleComparer());
		// retrieve the topics and their references for each different lower-case label text
		// TODO: consider merging the same topics with different label texts
		List<Topic> topics = topicDetector.getTopics(doc, rc, inputLang, nlpModel);
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

		DocumentTagger dt;
		if (linkFormat == LinkFormat.HTML || linkFormat == LinkFormat.HTML_ID
				|| linkFormat == LinkFormat.HTML_ID_WEIGHT)
			dt = new MyHtmlTagger(linkFormat, linkStyle);
		else
			dt = new MyWikiTagger(linkFormat);

		// the overlapped positions not tagged in the wikified text
		Set<Position> overlappedPositions = new HashSet<Position>();
		// the filtered positions not tagged in the wikified text, e.g., there is an annotation already
		// tagged before these positions in the same region
		Set<Position> filteredPositions = new HashSet<Position>();
		String taggedText = dt.tag(doc, detectedTopics, repeatMode, overlappedPositions, filteredPositions);

		Map<Mention, TreeSet<Annotation>> mention2annos = new LinkedHashMap<Mention, TreeSet<Annotation>>();
		for (Topic topic : detectedTopics) {
			for (Position position : topic.getPositions()) {
				if (mentionMode.equals(MentionMode.NON_OVERLAPPED) && overlappedPositions.contains(position))
					continue;
				else if (filteredPositions.contains(position))
					continue;
				else {
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
				}
			}
		}

		if (annotations != null) {
			for (Mention mention : mention2annos.keySet()) {
				TreeSet<Annotation> annos = mention2annos.get(mention);
				Annotation best = annos.last();
				if (responseMode.equals(ResponseMode.BEST)) {
					annotations.add(best);
				} else if (responseMode.equals(ResponseMode.ALL)) {
					for (Annotation anno : annos) {
						annotations.add(anno);
					}
				} else {
					annotations.add(best);
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

	@Override
	public String annotate(String source, List<Annotation> annotations) throws Exception {
		return null;
	}

}
