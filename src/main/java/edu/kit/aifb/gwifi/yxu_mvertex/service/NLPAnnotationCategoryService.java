package edu.kit.aifb.gwifi.yxu_mvertex.service;

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
import java.util.Collections;
import java.util.HashMap;
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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.sun.org.apache.xerces.internal.dom.DocumentImpl;

import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.annotation.Mention;
import edu.kit.aifb.gwifi.annotation.detection.DisambiguationUtil;
//import edu.kit.aifb.gwifi.annotation.detection.NLPTopicDetector;
//import edu.kit.aifb.gwifi.annotation.detection.NLPTopicDetector;
import edu.kit.aifb.gwifi.annotation.detection.Topic;
import edu.kit.aifb.gwifi.annotation.preprocessing.DocumentPreprocessor;
import edu.kit.aifb.gwifi.annotation.preprocessing.DummyPreprocessor;
import edu.kit.aifb.gwifi.annotation.preprocessing.HtmlPreprocessor;
import edu.kit.aifb.gwifi.annotation.preprocessing.PreprocessedDocument;
import edu.kit.aifb.gwifi.annotation.preprocessing.WikiPreprocessor;
import edu.kit.aifb.gwifi.annotation.tagging.DocumentTagger;
import edu.kit.aifb.gwifi.annotation.tagging.HtmlTagger;
import edu.kit.aifb.gwifi.annotation.tagging.WikiTagger;
import edu.kit.aifb.gwifi.annotation.weighting.TopicDisambiguator;
import edu.kit.aifb.gwifi.comparison.text.TextComparer;
import edu.kit.aifb.gwifi.crosslinking.LanguageLinksSearcher;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.service.A2WService;
import edu.kit.aifb.gwifi.service.HubConfiguration;
import edu.kit.aifb.gwifi.service.NLPDisambiguationService.MyWikiTagger;
import edu.kit.aifb.gwifi.service.Service.LinkFormat;
import edu.kit.aifb.gwifi.service.Service.ResponseFormat;
import edu.kit.aifb.gwifi.service.Service.SourceMode;
import edu.kit.aifb.gwifi.util.Position;
import edu.kit.aifb.gwifi.util.RelatednessCache;
import edu.kit.aifb.gwifi.util.WebContentRetriever;
import edu.kit.aifb.gwifi.util.nlp.Language;
import edu.kit.aifb.gwifi.yxu_mvertex.annotation.detection.NLPTopic;
import edu.kit.aifb.gwifi.yxu_mvertex.annotation.detection.NLPTopicDetector;
import edu.kit.aifb.gwifi.yxu_mvertex.annotation.detection.NLPTopicReference;
import edu.kit.aifb.gwifi.yxu_mvertex.annotation.detection.TopicCategory;
import edu.kit.aifb.gwifi.yxu_mvertex.annotation.weighting.TopicTopicCategoryDisambiguator;

public class NLPAnnotationCategoryService implements A2WService {

	private static boolean DEFAULT_TOOLTIPS = true;
	private static SourceMode DEFAULT_SOURCE_MODE = SourceMode.WIKI;
	private static LinkFormat DEFAULT_LINK_FORMAT = LinkFormat.AUTO;
	private static ResponseFormat DEFAULT_RESPONSE_FORMAT = ResponseFormat.XML;
	private static RepeatMode DEFAULT_REPEAT_MODE = RepeatMode.FIRST;
	private static ResponseMode DEFAULT_RESPONSE_MODE = ResponseMode.BEST;
	private static MentionMode DEFAULT_MENTION_MODE = MentionMode.NON_OVERLAPPED;
	private static NLPModel DEFAULT_NLP_MODEL = NLPModel.NER;
	private static DisambiguationModel DEFAULT_DISAMBIGUATION_MODEL = DisambiguationModel.PAGERANK_HITSHUB;
	private static KB DEFAULT_KB = KB.WIKIPEDIA;
	private static String linkClassName = "wm_wikifiedLink";
	private static Document doc = new DocumentImpl();
	private static DecimalFormat decimalFormat = (DecimalFormat) NumberFormat
			.getInstance(Locale.US);
	private float TALPHA = 0.6f;// TODO 0.85f
	private float CALPHA = 0.8f;// TODO 0.05f
	private float BETA = 0.1f;// TODO 1.0f

	public final static String KB_WIKIPEDIA = "wikipedia";
	public final static String KB_DBPEDIA = "dbpedia";
	private final static String WIKIPEDIA_URL = ".wikipedia.org/wiki/";
	private final static String DBPEDIA_URL = ".dbpedia.org/resource/";

	// public final static String ITEM_TAG = "item";
	public final static String ITEMS_TAG = "items";
	public final static String ITEM_TAG = "item";
	public final static String TEXT_TAG = "text";
	public final static String DB_ID_KEY = "_id";
	public final static String DB_CONTENT_KEY = "content";
	public final static String CATES_TAG = "Categories";
	public final static String CATE_TAG = "Category";
	public final static String CATE_ID_TAG = "id";
	public final static String CATE_DISPLAYNAME_TAG = "displayName";
	public final static String CATE_WEIGHT_TAG = "weight";

	private Wikipedia wikipedia;
	private DisambiguationUtil disambiguationUtil;
	private NLPTopicDetector topicDetector;
	private TopicDisambiguator topicDisambiguator;
	private TopicTopicCategoryDisambiguator ttcDisambiguator;
	private TextComparer searcher;
	// private TopicRefiner topicRefiner;
	private WebContentRetriever retriever;
	private DocumentPreprocessor preprocessor;
	// TODO local testb
	// protected LanguageLinksSearcher langlinks;
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
	private float cateWeightThreshold = 0.5f;
	private int cateMaxNum = 3;
	private String customizedCategoriesFilename;

	public NLPAnnotationCategoryService(String hubconfig, String wikiconfig,
			String NLPconfig, Language inputLang, Language outputLang, KB kb,
			NLPModel nlpModel, DisambiguationModel disambiguationModel,
			MentionMode mentionMode, ResponseMode responseMode,
			RepeatMode repeatMode) throws Exception {
		HubConfiguration config = new HubConfiguration(new File(hubconfig));
		retriever = new WebContentRetriever(config);
		customizedCategoriesFilename = config.getCategoriesPath();
		// TODO local testb
		// langlinks = new LanguageLinksSearcher(config.getLanglinksPath());
		File databaseDirectory = new File(wikiconfig);
		wikipedia = new Wikipedia(databaseDirectory, false);
		disambiguationUtil = new DisambiguationUtil(wikipedia);
		topicDetector = new NLPTopicDetector(wikipedia, disambiguationUtil,
				searcher, NLPconfig, false, false);
		topicDisambiguator = new TopicDisambiguator(disambiguationUtil);
		ttcDisambiguator = new TopicTopicCategoryDisambiguator(wikipedia,
				disambiguationUtil, customizedCategoriesFilename);

		// topicRefiner = new TopicRefiner();

		this.sourceMode = DEFAULT_SOURCE_MODE;
		this.linkFormat = DEFAULT_LINK_FORMAT;
		this.responseFormat = DEFAULT_RESPONSE_FORMAT;
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
	
	public void setAlpha(float talpha, float calpha, float beta){
		this.TALPHA = talpha;
		this.CALPHA = calpha;
		this.BETA = beta;
	}
	
	public void setCateThreshold(float cateThreshold){
		this.cateWeightThreshold = cateThreshold;
		this.ttcDisambiguator.setAidaWeightThresFactor(cateThreshold);
	}
	
	public void setLanguage(Language lang){
		this.inputLang = lang;
		this.outputLang = lang;
	}
	
	public Language getLanguage(){
		return this.inputLang;
	}
	
	public void setWikiDocumentPreprocessor(){
		preprocessor = new WikiPreprocessor(wikipedia);
	}

	public Document getDoc(){
		return this.doc;
	}
	@Override
	public String annotate(String source, List<Annotation> annotations)
			throws Exception {
		if (source == null || source.trim().length() == 0) {
			System.out.println("You must specify a source document to wikify");
		}
		SourceMode sourceMode;
		LinkFormat linkFormat;
		ResponseFormat responseFormat;
		if (this.sourceMode == SourceMode.AUTO)
			sourceMode = resolveSourceMode(source);
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
		if (sourceMode == SourceMode.WIKI)
			preprocessor = new WikiPreprocessor(wikipedia);
		else
			preprocessor = new HtmlPreprocessor();

		List<TopicCategory> categories = new ArrayList<TopicCategory>();
		if (responseFormat == ResponseFormat.DIRECT) {
			String wikifiedDoc = wikifyAndGatherAnnotations(source, null,
					annotations, categories, null, sourceMode, linkFormat);
			return wikifiedDoc;
		} else if (responseFormat == ResponseFormat.XML) {
			if (annotations == null)
				annotations = new ArrayList<Annotation>();
			String wikifiedDoc = wikifyAndGatherAnnotations(source, null,
					annotations, categories, null, sourceMode, linkFormat);
			Element xmlResponse = buildXMLResponse(doc, wikifiedDoc,
					annotations, categories);
			Transformer transformer = TransformerFactory.newInstance()
					.newTransformer();
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(
					"{http://xml.apache.org/xslt}indent-amount", "2");
			StringWriter writer = new StringWriter();
			StreamResult result = new StreamResult(writer);
			DOMSource dom = new DOMSource(xmlResponse);
			transformer.transform(dom, result);
			return writer.toString();
		} else {
			return source;
		}
	}

	public String disambiguate(String source, Set<Position> positions,
			List<Annotation> annotations, Set<String> initCategory) throws Exception {
		if (source == null || source.trim().length() == 0) {
			System.out.println("You must specify a source document to wikify");
		}
		preprocessor = new WikiPreprocessor(wikipedia);
		List<TopicCategory> categories = new ArrayList<TopicCategory>();
		if (annotations == null)
			annotations = new ArrayList<Annotation>();
		String wikifiedDoc = wikifyAndGatherAnnotations(source, positions,
				annotations, categories, initCategory, DEFAULT_SOURCE_MODE,
				DEFAULT_LINK_FORMAT);

		Element xmlResponse = buildXMLResponse(doc, wikifiedDoc, annotations,
				categories);

		Transformer transformer = TransformerFactory.newInstance()
				.newTransformer();
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(
				"{http://xml.apache.org/xslt}indent-amount", "2");

		StringWriter writer = new StringWriter();
		StreamResult result = new StreamResult(writer);
		DOMSource dom = new DOMSource(xmlResponse);
		transformer.transform(dom, result);
		return writer.toString();
	}
	
	public Element annotateContent(String id, String content) throws Exception {
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
		List<TopicCategory> categories = new ArrayList<TopicCategory>();
		if (responseFormat == ResponseFormat.DIRECT) {
			String wikifiedDoc = wikifyAndGatherAnnotations(content, null,
					annotations, categories, null, sourceMode, linkFormat);
			Element xmlItem = buildXMLItem(doc, id, content, wikifiedDoc, null,
					null);
			return xmlItem;
		} else if (responseFormat == ResponseFormat.XML) {
			String wikifiedDoc = wikifyAndGatherAnnotations(content, null,
					annotations, categories, null, sourceMode, linkFormat);
			Element xmlItem = buildXMLItem(doc, id, content, wikifiedDoc,
					annotations, categories);
			return xmlItem;
		} else {
			Element xmlItem = buildXMLItem(doc, id, content, null, null, null);
			return xmlItem;
		}
	}

	public Element buildXMLItem(Document doc, String id, String content,
			String wikifiedDoc, Collection<Annotation> annotations,
			Collection<TopicCategory> categories) throws Exception {
		Element xmlItem = createElement(doc, ITEM_TAG);
		xmlItem.setAttribute(DB_ID_KEY, id);
		Element xmlContent = createCDATAElement(doc, DB_CONTENT_KEY, content);
		xmlItem.appendChild(xmlContent);
		if (wikifiedDoc == null)
			return xmlItem;
		Element xmlWikifiedDoc = createCDATAElement(doc, WIKIFIED_DOC_TAG,
				wikifiedDoc);
		xmlItem.appendChild(xmlWikifiedDoc);
		if (annotations == null)
			return xmlItem;
		Element xmlDetectedTopics = createAnnosElement(doc, ANNOS_TAG,
				annotations);
		xmlItem.appendChild(xmlDetectedTopics);
		//TODO append category information
		 Element xmlDetectedCategories = createCatesElement(doc, CATES_TAG,
		 categories);
		 xmlItem.appendChild(xmlDetectedCategories);
		return xmlItem;
	}

	public Element buildXMLResponse(Document doc, String wikifiedDoc,
			Collection<Annotation> annotations,
			Collection<TopicCategory> categories) throws Exception {
		Element xmlResponse = createElement(doc, RESPONSE_TAG);
		Element xmlWikifiedDoc = createCDATAElement(doc, WIKIFIED_DOC_TAG,
				wikifiedDoc);
		xmlResponse.appendChild(xmlWikifiedDoc);
		Element xmlDetectedTopics = createAnnosElement(doc, ANNOS_TAG,
				annotations);
		xmlResponse.appendChild(xmlDetectedTopics);
		Element xmlDetectedCategories = createCatesElement(doc, CATES_TAG,
				categories);
		xmlResponse.appendChild(xmlDetectedCategories);
		return xmlResponse;
	}

	protected Element createAnnosElement(Document doc, String tagName,
			Collection<Annotation> annotations) {
		Element annosEle = createElement(doc, ANNOS_TAG);
		for (Annotation anno : annotations) {
			Element annoEle = createElement(doc, ANNO_TAG);
			annoEle.setAttribute(ANNO_ID_TAG, String.valueOf(anno.getId()));
			annoEle.setAttribute(ANNO_DISPLAYNAME_TAG, anno.getDisplayName());
			annoEle.setAttribute(ANNO_URL_TAG, anno.getURL());
			annoEle.setAttribute(ANNO_WEIGHT_TAG, format(anno.getWeight()));
			Element mentionEle = createElement(doc, MENTION_TAG);
			mentionEle.setAttribute(MENTION_LABEL_TAG,
					String.valueOf(anno.getMention().getTerm()));
			int start = anno.getMention().getPosition().getStart();
			int end = anno.getMention().getPosition().getEnd();
			mentionEle.setAttribute(MENTION_POS_TAG, String.valueOf(start));
			mentionEle.setAttribute(MENTION_LENGTH_TAG,
					String.valueOf(end - start));
			annoEle.appendChild(mentionEle);
			annosEle.appendChild(annoEle);
		}
		return annosEle;
	}

	protected Element createCatesElement(Document doc, String tagName,
			Collection<TopicCategory> categories) {
		Element catesEle = createElement(doc, CATES_TAG);
		for (TopicCategory cate : categories) {
			Element cateEle = createElement(doc, CATE_TAG);
			cateEle.setAttribute(CATE_ID_TAG, String.valueOf(cate.getId()));
			//TODO print wiki cate title or aida cate title
			cateEle.setAttribute(CATE_DISPLAYNAME_TAG, cate.getTitle());//cate.getAidaTitle()
			cateEle.setAttribute(ANNO_WEIGHT_TAG, format(cate.getWeight()));
			catesEle.appendChild(cateEle);
		}
		return catesEle;
	}

	protected String wikifyAndGatherAnnotations(String source,
			Set<Position> positions, List<Annotation> annotations,
			List<TopicCategory> categories, Set<String> initCategories, SourceMode sourceMode,
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
		// TODO: find smarter way to resolve this hack, which stops wikifier
		// from detecting "Space (punctuation)" ;
		// for (Article bt: bannedTopicList)
		// doc.banTopic(bt.getId()) ;
		doc.banTopic(143856);

		RelatednessCache rc = new RelatednessCache(
				disambiguationUtil.getArticleComparer());
		// retrieve the topics and their references for each different
		// lower-case label text
		// TODO: *****consider merging the same topics with different label
		// texts

		List<Topic> topics;
		if (positions != null) {
//			 topics = topicDetector.getTopics(doc, positions, rc, inputLang,
//			 nlpModel);
			topics = topicDetector.getTopicsWithPOSandNER(doc, positions, rc,
					inputLang);
		} else {
//			topics = topicDetector.getTopics(doc, rc, inputLang, nlpModel);
			topics = topicDetector.getTopicsWithPOSandNER(doc, rc, inputLang);
		}

		// System.out.println("origin topics:");
		// for(Topic t:topics){
		// System.out.println(t.getPositions() + "\t" + t.getTitle()+ "\t"
		// +((NLPTopicReference)t.getReferences().get(0)).getNlpModel());
		// }
		List<TopicCategory> weightedCategories = new ArrayList<TopicCategory>();

		if (topics.size() > 0) {
			ttcDisambiguator.getWeightedTopicTopicCategory(topics, rc,
					disambiguationModel, weightedCategories, initCategories,TALPHA, CALPHA, BETA);
			// topics = topicDisambiguator.getWeightedTopics(topics, rc,
			// disambiguationModel);
			Collections.sort(topics);
			Collections.sort(weightedCategories);
		}
		// topics = topicRefiner.getRefinedTopics(topics, rc);

		ArrayList<Topic> detectedTopics = new ArrayList<Topic>();
		filterWeightedTopicList(topics, detectedTopics, wikipedia.getConfig().getMinWeight());
		//TODO filter wiki cate or pass aida cate
		filterWeightedCateList(weightedCategories, categories);

		DocumentTagger dt;
		if (linkFormat == LinkFormat.HTML || linkFormat == LinkFormat.HTML_ID
				|| linkFormat == LinkFormat.HTML_ID_WEIGHT)
			dt = new MyHtmlTagger(linkFormat, linkStyle);
		else
			dt = new MyWikiTagger(linkFormat);

		// the overlapped positions not tagged in the wikified text
		Set<Position> overlappedPositions = new HashSet<Position>();
		// the filtered positions not tagged in the wikified text, e.g., there
		// is an annotation already tagged before these positions in the same
		// region
		Set<Position> filteredPositions = new HashSet<Position>();
		String taggedText = dt.tag(doc, detectedTopics, repeatMode,
				overlappedPositions, filteredPositions);

		//TODO salient
//		Map<Integer, Topic> id2Topic = new HashMap<Integer, Topic>();
		Map<Mention, TreeSet<Annotation>> mention2annos = new LinkedHashMap<Mention, TreeSet<Annotation>>();
		for (Topic topic : detectedTopics) {
//			id2Topic.put(topic.getId(), topic);
			for (Position position : topic.getPositions()) {
				if (mentionMode.equals(MentionMode.NON_OVERLAPPED)
						&& overlappedPositions.contains(position))
					continue;
				else if (filteredPositions.contains(position))
					continue;
				else {
					String term = source.substring(position.getStart(),
							position.getEnd());
					Mention mention = new Mention(position, term);
					TreeSet<Annotation> annos = mention2annos.get(mention);
					if (annos == null) {
						annos = new TreeSet<Annotation>();
						mention2annos.put(mention, annos);
					}
					Annotation anno = new Annotation(topic.getId(),
							topic.getTitle());
					anno.setMention(mention);
					anno.setWeight(topic.getWeight());
					anno.setDisplayName(topic.getDisplayName());
					anno.setURL(topic.getURI());
					annos.add(anno);
				}
			}
		}

		
		if (annotations != null) {
			// TODO find annotation order in one mention
//			System.out.println("mention to annotations:");
			for (Mention mention : mention2annos.keySet()) {
//				System.out.println(mention.getPosition());
				if (positions == null || positions.size() == 0
						|| positions.contains(mention.getPosition())) {
					TreeSet<Annotation> annos = mention2annos.get(mention);
					Annotation best = annos.last();
					if (responseMode.equals(ResponseMode.BEST)) {
						//TODO sum all annos to the best anno
//						double totalWeight = 0.0;
//						for (Annotation anno : annos) {
//							totalWeight += anno.getWeight();
//						}
//						if(totalWeight< wikipedia.getConfig().getMinSalience())
//							continue;
//						best.setWeight(totalWeight);
						annotations.add(best);
//						System.out.println("best:\t" + best.getId() + "\t:"
//								+ best.getDisplayName() + "\t:"
//								+ best.getWeight());
					} else if (responseMode.equals(ResponseMode.ALL)) {
						for (Annotation anno : annos) {
							annotations.add(anno);
						}
					} else {
						annotations.add(best);
					}
//					for (Annotation anno : annos) {
//						System.out.println(anno.getId() + "\t:"
//								+ anno.getDisplayName() + "\t:"
//								+ anno.getWeight());
//					}
				}
			}
			//TODO salient
//			List<Topic> disambiguatedTopic = new ArrayList<Topic>();
//			for(Annotation anno: annotations){
//				int id = anno.getId();
//				disambiguatedTopic.add(id2Topic.get(anno.getId()));
//			}
//			
//			ttcDisambiguator.getWeightedTopicTopicCategory(disambiguatedTopic, rc,
//					DisambiguationModel.PAGERANK_HITSHUB, weightedCategories, initCategories,TALPHA, CALPHA, BETA);
//			Collections.sort(disambiguatedTopic);
//			detectedTopics = new ArrayList<Topic>();
//			filterWeightedTopicList(disambiguatedTopic, detectedTopics, wikipedia.getConfig().getMinWeight());
//			boolean isSalient = false;
//			List<Annotation> salientAnno = new ArrayList<Annotation>();
//			for(Annotation anno: annotations){
//				int annoId = anno.getId();
//				for(Topic topic: detectedTopics){
//					int topicId = topic.getId();
//					if(annoId == topicId)
//						isSalient = true;
//						continue;
//				}
//				if(isSalient){
//					salientAnno.add(anno);
//					isSalient = false;
//				}
//			}
//			annotations.clear();
//			annotations.addAll(salientAnno);
		}

		if (sourceMode == SourceMode.URL) {
			taggedText = taggedText.replaceAll("(?i)<html", "<base href=\""
					+ source + "\" target=\"_top\"/><html");

			if (DEFAULT_TOOLTIPS) {

				// String basePath = getBasePath(request);
				String basePath = "";

				if (!basePath.endsWith("/"))
					basePath = basePath + "/";

				StringBuffer newHeaderStuff = new StringBuffer();
				newHeaderStuff
						.append("<link type=\"text/css\" rel=\"stylesheet\" href=\""
								+ basePath + "/css/tooltips.css\"/>\n");
				newHeaderStuff
						.append("<link type=\"text/css\" rel=\"stylesheet\" href=\""
								+ basePath
								+ "/css/jquery-ui/jquery-ui-1.8.14.custom.css\"/>\n");

				if (linkStyle != null && linkStyle.trim().length() > 0)
					newHeaderStuff.append("<style type='text/css'> ."
							+ linkClassName + "{" + linkStyle + ";}</style>\n");

				newHeaderStuff.append("<script type=\"text/javascript\" src=\""
						+ basePath + "/js/jquery-1.5.1.min.js\"></script>\n");
				newHeaderStuff.append("<script type=\"text/javascript\" src=\""
						+ basePath + "/js/tooltips.js\"></script>\n");
				newHeaderStuff.append("<script type=\"text/javascript\"> \n");
				newHeaderStuff
						.append("  var wm_host=\"" + basePath + "\" ; \n");
				newHeaderStuff.append("  $(document).ready(function() { \n");
				newHeaderStuff
						.append("    wm_addDefinitionTooltipsToAllLinks(null, \""
								+ linkClassName + "\") ; \n");
				newHeaderStuff.append("  });\n");
				newHeaderStuff.append("</script>\n");

				taggedText = taggedText.replaceAll("(?i)\\</head>",
						Matcher.quoteReplacement(newHeaderStuff.toString())
								+ "</head>");
			}

		}

		return taggedText;
	}

	private int filterWeightedTopicList(Collection<Topic> weightedTopics,
			List<Topic> detectedTopics, double thresh) {
		// TODO keep the first n topic, valid in POS
		// int n = 50;
		// int m = 300;
		// int i = 0;
		// int j = 0;
		for (Topic topic : weightedTopics) {
			if (topic.getWeight() < thresh)
				break;
			// if (((NLPTopic) topic).getNlpModel()==null||isValidTopic(topic,
			// NLPModel.NER)) {
			// if (i >= n)
			// break;
			// i++;
			// // System.out.println("NER: "+i+"\t"+topic.getId() + " : " +
			// topic.getTitle() + " : "
			// // + topic.getWeight());
			// } else if (isValidTopic(topic, NLPModel.POS)) {
			// if (j >= m)
			// continue;
			// j++;
			// // System.out.println("POS: "+j+"\t"+topic.getId() + " : " +
			// topic.getTitle() + " : "
			// // + topic.getWeight());
			// }
			String title = topic.getTitle();
			// TODO hidden for local testb
			// int id = topic.getId();
			String displayName = title;// extractCrossDescription(id, title,
										// inputLang,outputLang);
			if (displayName == null || displayName.equals(""))
				continue;
			String uri = getURI(displayName, inputLang, outputLang, kb);
			topic.setURI(uri);
			topic.setDisplayName(displayName);
			detectedTopics.add(topic);
//			System.out.println(topic.getId() + " : " + topic.getTitle() + " : "
//					+ topic.getWeight());
		}
		return 0;
	}

	private int filterWeightedCateList(Collection<TopicCategory> weightedCates,
			List<TopicCategory> detectedCates) {
		// TODO keep the first n categories
		int i = 0;
		for (TopicCategory tc : weightedCates) {
			if (i >= cateMaxNum)
				break;
			// the category with weight less than the threshold will not
			// be added in the detected category list
			if (tc.getWeight() <= cateWeightThreshold)
				continue;
			i++;
			detectedCates.add(tc);
//			 System.out.println(tc.getIndex() + " : " + tc.getTitle() + " : "
//			 + tc.getWeight());
		}
		return 0;
	}

	private boolean isValidTopic(Topic t, NLPModel nlpModel) {
		if (nlpModel.equals(NLPModel.NER)) {
			return (!(t instanceof NLPTopic))
					|| ((NLPTopic) t).getNlpModel().equals(NLPModel.NER);
		} else if (nlpModel.equals(NLPModel.POS)) {
			return (!(t instanceof NLPTopic))
					|| ((NLPTopic) t).getNlpModel().equals(NLPModel.NER)
					|| ((NLPTopic) t).getNlpModel().equals(NLPModel.POS);
		} else if (nlpModel.equals(NLPModel.NGRAM)) {
			return true;
		} else {
			return true;
		}
	}

	protected static SourceMode resolveSourceMode(String source)
			throws MalformedURLException {

		// try to parse source as url
		if (source.matches("(?i)^www\\.(.*)$")) {
			// fix omitted http prefix
			// source = "http://" + source;
			// URL url = new URL(source);
			return SourceMode.URL;
		}
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

	public static Element createCDATAElement(Document doc, String tagName,
			String data) {
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

			if (linkFormat == LinkFormat.HTML_ID
					|| linkFormat == LinkFormat.HTML_ID_WEIGHT)
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

			if (linkFormat == LinkFormat.WIKI_ID
					|| linkFormat == LinkFormat.WIKI_ID_WEIGHT) {
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

	// TODO hidden for local testb
	// public String extractCrossDescription(int pageId, String title,
	// Language input, Language output) {
	// if (input.equals(output))
	// return title;
	// String displayName = null;
	// try {
	// if (!input.equals(output)) {
	// displayName = langlinks.getDescription(pageId,
	// input.toString(), output.toString());
	// }
	//
	// return displayName;
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// return null;
	// }

	public static String getURI(String title, Language input, Language output,
			KB kb) {
		String uri = null;
		if (kb.equals(KB.DBPEDIA)) {
			if (output.equals(Language.EN))
				uri = "http://" + DBPEDIA_URL.substring(1)
						+ title.replace(" ", "_");
			else
				uri = "http://" + output.toString() + DBPEDIA_URL
						+ title.replace(" ", "_");
		} else if (kb.equals(KB.WIKIPEDIA)) {
			uri = "http://" + output.toString() + WIKIPEDIA_URL
					+ title.replace(" ", "_");
		} else {
			uri = "http://" + output.toString() + WIKIPEDIA_URL
					+ title.replace(" ", "_");
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

	public DOMSource getDOMSource(String source, String outputLangLabel,
			String kbLabel) throws Exception {
		return null;
	}

	public static String extractText(Document doc) {
		NodeList textNodeList = doc.getElementsByTagName(TEXT_TAG);
		String text = textNodeList.item(0).getFirstChild().getNodeValue();
		return text;
	}

	public static void main(String[] args) throws Exception {
//		NLPAnnotationCategoryService service = new NLPAnnotationCategoryService("configs/hub-template.xml",
//			    "configs/wikipedia-template-en.xml", "configs/NLPConfig.properties", Language.EN, Language.EN,
//			    KB.WIKIPEDIA, NLPModel.NER, DisambiguationModel.PAGERANK_HITSHUB, MentionMode.OVERLAPPED,
//			    ResponseMode.BEST, RepeatMode.ALL);
//
//			  service.setAlpha(0.5f, 1.0f, 0.1f);
//			  service.setCateThreshold(0.1f);
//
//			  File file = new File("res/49960");
//			  BufferedReader reader = new BufferedReader(new FileReader(file));
//			  String line, source = "";
//			  while((line = reader.readLine()) != null) {
//			   source += line + " ";
//			  }
//			  reader.close();
//			  String result = service.annotate(source, null);
//			  System.out.println(result);
			  
		NLPAnnotationCategoryService service = new NLPAnnotationCategoryService(
				"configs/hub-template.xml",
				"configs/wikipedia-template-en.xml",
				"configs/NLPConfig.properties", Language.EN, Language.EN,
				KB.WIKIPEDIA, NLPModel.NER, DisambiguationModel.PAGERANK_HITSHUB,
				MentionMode.OVERLAPPED, ResponseMode.BEST, RepeatMode.ALL);

		service.setAlpha(0.6f,1.0f,0.1f);//0.5f,1.0f,0.2f
		service.setCateThreshold(0.7f);

		Scanner scanner = new Scanner(System.in);
		
		Set<String> initCates = new HashSet<String>();
		initCates.add(TopicCategory.TC_SOCIETY);
		initCates.add(TopicCategory.TC_SPORTS);
		while (true) {
			System.out.println("Please input the source text:");
			String source = scanner.nextLine();

			if (source.startsWith("exit")) {
				break;
			}
//			String result = service.annotate(source, null);
			 String result = service.disambiguate(source, new
			 HashSet<Position>(), null, initCates);
			 System.out.println(result);
		}
		scanner.close();
	}

}
