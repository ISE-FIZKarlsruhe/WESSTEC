//package edu.kit.aifb.gwifi.annotation.detection;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.LinkedHashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Map.Entry;
//import java.util.Set;
//import java.util.TreeSet;
//import java.util.Vector;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//import edu.kit.aifb.gwifi.annotation.Mention;
//import edu.kit.aifb.gwifi.annotation.preprocessing.PreprocessedDocument;
//import edu.kit.aifb.gwifi.concept.searcher.Searcher;
//import edu.kit.aifb.gwifi.mingyuzuo.MongoLabel;
//import edu.kit.aifb.gwifi.model.Article;
//import edu.kit.aifb.gwifi.model.Label;
//import edu.kit.aifb.gwifi.model.Page.PageType;
//import edu.kit.aifb.gwifi.model.Wikipedia;
//import edu.kit.aifb.gwifi.nlp.preprocessing.NLPPreprocessor;
//import edu.kit.aifb.gwifi.nlp.preprocessing.OpenNLPPreprocessor;
//import edu.kit.aifb.gwifi.nlp.preprocessing.StanfordNLPPreprocessor;
//import edu.kit.aifb.gwifi.service.Service.NLPModel;
//import edu.kit.aifb.gwifi.util.Position;
//import edu.kit.aifb.gwifi.util.RelatednessCache;
//import edu.kit.aifb.gwifi.util.nlp.Language;
//
//public class NLPTopicDetector {
//
//	private Wikipedia wikipedia;
//	private DisambiguationUtil disambiguator;
//	private Searcher searcher;
//	private Map<Language, NLPPreprocessor> lang2preprocessor;
//
//	private String NLPConfigFile;
//	private NewMentionDetector newMentionDetector;
//	private boolean strictDisambiguation;
//	private boolean allowDisambiguations;
//
//	private int maxTopicsForRelatedness = 25;
//	private float disambigConfidenceThreshold = 0.0f;
//
//	private final static String NP1 = "(NN|NNP|NNS|NNPS)+";
//	private final static String NP2 = NP1 + "(CD)*";
//	private final static String DP1 = "(RB|RBR|RBS)*(JJ|JJS|JJR)+";
//	private final static String DP2 = "(VBG|VBN)+";
//	private final static String DP = "(" + DP1 + "|" + DP2 + ")";
//	private final static String NP3 = DP + "*" + NP2;
//	private final static String CP = "(IN)";
//	private final static String NE1 = NP3 + CP + ".*(POS)*" + NP3;
//	private final static String NE2 = NP3 + "(POS)*" + NP3;
//	private final static String MP = "(" + NP3 + "|" + NE1 + "|" + NE2 + ")";
//	// private final static String MP="("+NP3+"|"+NE2+")";
//
//	private final static String rule2 = "(JJ|JJS)+" + NP1;
//	private final static String rule3 = NP1 + "of.*" + NP1;
//	private final static String rule4 = NP1 + "POS" + NP1;
//	private final static String finalRule = "(" + NP1 + "|" + rule2 + "|" + rule3 + "|" + rule4 + ")";
//
//	// private final static String POSNOUNTAGS = ".*(NN|NR|NNP|NNPS|NNS)$";
//	// private final static String POSNOUNTAGS=namedEntityRegex;
//	// private final static String POSNOUNTAGS=finalRule;
//	private final static String POSNOUNTAGS = MP;
//
//	private final static String SPLITTER = "[\\s\\{\\}\\(\\)\"\'\\.\\,\\;\\:\\-\\_\\/\\[\\]“”’!…‘’?？–%]";
//
//	private final static int CONTEXT_WINDOW_SIZE = 100;
//
//	/**
//	 * Initializes a new topic detector.
//	 * 
//	 * @param wikipedia
//	 *            an initialized instance of Wikipedia
//	 * @param disambiguator
//	 *            a trained
//	 * @param stopwordFile
//	 *            an optional (may be null) file containing
//	 * @param strictDisambiguation
//	 *            consider only topics with the largest prior probability
//	 * @param allowDisambiguations
//	 *            allow disambiguation pages to be linked
//	 * @throws IOException
//	 */
//	public NLPTopicDetector(Wikipedia wikipedia, DisambiguationUtil disambiguator, Searcher searcher, String NLPConfig,
//			boolean strictDisambiguation, boolean allowDisambiguations) throws IOException {
//		this.wikipedia = wikipedia;
//		this.disambiguator = disambiguator;
//		this.searcher = searcher;
//
//		this.strictDisambiguation = strictDisambiguation;
//		this.allowDisambiguations = allowDisambiguations;
//
//		this.lang2preprocessor = new HashMap<Language, NLPPreprocessor>();
//
//		this.NLPConfigFile = NLPConfig;
//	}
//
//	public NLPTopicDetector(Wikipedia wikipedia, DisambiguationUtil disambiguator, String NLPConfig, boolean strictDisambiguation,
//			boolean allowDisambiguations) throws IOException {
//		this(wikipedia, disambiguator, null, NLPConfig, strictDisambiguation, allowDisambiguations);
//	}
//	
//	public NLPTopicDetector(Wikipedia wikipedia, DisambiguationUtil disambiguator, boolean strictDisambiguation,
//			boolean allowDisambiguations) throws IOException {
//		this(wikipedia, disambiguator, null, null, strictDisambiguation, allowDisambiguations);
//	}
//
//	public Map<Language, NLPPreprocessor> getLang2preprocessor() {
//		return lang2preprocessor;
//	}
//
//	public NewMentionDetector getNewMentionDetector() {
//		return newMentionDetector;
//	}
//
//	public void setNewMentionDetector(NewMentionDetector newMentionDetector) {
//		this.newMentionDetector = newMentionDetector;
//	}
//
//	public void setLang2preprocessor(Map<Language, NLPPreprocessor> lang2preprocessor) {
//		this.lang2preprocessor = lang2preprocessor;
//	}
//
//	/**
//	 * Gathers a collection of topics from the given document.
//	 * 
//	 * @param doc
//	 *            a document that has been preprocessed so that markup (html, mediawiki, etc) is safely ignored.
//	 * @param rc
//	 *            a cache in which relatedness measures will be saved so they aren't repeatedly calculated. This may be
//	 *            null.
//	 * @return a vector of topics that were mined from the document.
//	 * @throws Exception
//	 */
//	// the order of the retrieved topics according to 1) the appearance order of mentions and 2) the topic weights
//	public List<Topic> getTopics(PreprocessedDocument doc, RelatednessCache rc, Language lang, NLPModel model)
//			throws Exception {
//
//		long start = System.currentTimeMillis();
//
//		if (rc == null)
//			rc = new RelatednessCache(disambiguator.getArticleComparer());
//
//		// changed by dong
//		// Vector<TopicReference> references =
//		// getReferences(doc.getPreprocessedText());
//		// Vector<TopicReference> references =
//		// getStanNERChineseReferences(doc.getPreprocessedText());
//		Vector<TopicReference> references;
//		if (model.equals(NLPModel.POS)) {
//			references = getReferencesByPOS(doc.getPreprocessedText(), lang);
//		} else if (model.equals(NLPModel.NER)) {
//			references = getReferencesByNER(doc.getPreprocessedText(), lang);
//		} else if (model.equals(NLPModel.NGRAM)) {
//			references = getReferencesByNGram(doc.getPreprocessedText(), lang);
//		} else {
//			references = getReferencesByNGram(doc.getPreprocessedText(), lang);
//		}
//		long end = System.currentTimeMillis();
//		System.out.println("Time for getting references: " + (end - start) + " ms");
//
//		long newStart = System.currentTimeMillis();
//
//		Collection<Topic> temp = getTopicsByLocalCompatibility(references, doc.getContextText(),
//				doc.getOriginalText().length(), rc);
//
//		end = System.currentTimeMillis();
//		System.out.println("Time for getting topics: " + (end - newStart) + " ms");
//
//		// calculateRelatedness(temp, rc);
//
//		List<Topic> topics = new ArrayList<Topic>();
//		int i = 0;
//		for (Topic topic : temp) {
//			if (!doc.isTopicBanned(topic.getId())) {
//				topic.setIndex(i++);
//				topics.add(topic);
//			}
//		}
//
//		end = System.currentTimeMillis();
//		System.out.println("Total time for topic detection: " + (end - start) + " ms\n");
//
//		return topics;
//	}
//
//	// public Vector<Topic> getTopics(PreprocessedDocument doc, RelatednessCache
//	// rc) throws Exception {
//	//
//	// if (rc == null)
//	// rc = new RelatednessCache(disambiguator.getArticleComparer());
//	//
//	// long start = System.currentTimeMillis();
//	// // changed by dong
//	// // Vector<TopicReference> references =
//	// // getReferences(doc.getPreprocessedText());
//	// // Vector<TopicReference> references =
//	// // getStanNERChineseReferences(doc.getPreprocessedText());
//	// Vector<TopicReference> references;
//	//
//	// references = getNonChineseNgramReferences(doc.getPreprocessedText());
//	//
//	// long end = System.currentTimeMillis();
//	// System.out.println("Time for getting references: " + (end - start) +
//	// " ms");
//	//
//	// long newStart = System.currentTimeMillis();
//	//
//	// Collection<Topic> temp = getTopicsByLocalCompatibility(references,
//	// doc.getContextText(), doc.getOriginalText()
//	// .length(), rc);
//	//
//	// end = System.currentTimeMillis();
//	// System.out.println("Time for getting topics: " + (end - newStart) +
//	// " ms");
//	//
//	// // calculateRelatedness(temp, rc);
//	//
//	// Vector<Topic> topics = new Vector<Topic>();
//	// int i = 0;
//	// for (Topic topic : temp) {
//	// if (!doc.isTopicBanned(topic.getId())) {
//	// topic.setIndex(i++);
//	// topics.add(topic);
//	// }
//	// }
//	//
//	// end = System.currentTimeMillis();
//	// System.out.println("Total time for topic detection: " + (end - start) +
//	// " ms\n");
//	//
//	// return topics;
//	// }
//
//	/**
//	 * Gathers a collection of topics from the given document.
//	 * 
//	 * @param text
//	 *            text to mine topics from. This must be plain text, without any form of markup.
//	 * @param rc
//	 *            a cache in which relatedness measures will be saved so they aren't repeatedly calculated. This may be
//	 *            null.
//	 * @return a collection of topics that were mined from the document.
//	 * @throws Exception
//	 */
//	// public Collection<Topic> getTopics(String text, RelatednessCache rc)
//	// throws Exception {
//	//
//	// if (rc == null)
//	// rc = new RelatednessCache(disambiguator.getArticleComparer());
//	//
//	// Vector<TopicReference> references = getNonChineseNgramReferences(text);
//	//
//	// Collection<Topic> topics = getTopics(references, "", text.length(), rc);
//	//
//	// calculateRelatedness(topics, rc);
//	//
//	// int i = 0;
//	// for (Topic topic : topics) {
//	// topic.setIndex(i++);
//	// }
//	//
//	// return topics;
//	// }
//
//	/**
//	 * Gathers a collection of topics from the given document.
//	 * 
//	 * @param text
//	 *            text to mine topics from. This must be plain text, without any form of markup.
//	 * @param rc
//	 *            a cache in which relatedness measures will be saved so they aren't repeatedly calculated. This may be
//	 *            null.
//	 * @param posistions
//	 *            a set of positions of the mentions
//	 * @return a collection of topics that were mined from the document.
//	 * @throws Exception
//	 */
//	public List<Topic> getTopics(PreprocessedDocument doc, RelatednessCache rc, Set<Position> positions)
//			throws Exception {
//
//		if (rc == null)
//			rc = new RelatednessCache(disambiguator.getArticleComparer());
//
//		long start = System.currentTimeMillis();
//		Vector<TopicReference> references = getReferencesByPosition(doc.getPreprocessedText(), positions);
//
//		long end = System.currentTimeMillis();
//		System.out.println("Time for getting references: " + (end - start) + " ms");
//
//		long newStart = System.currentTimeMillis();
//		Collection<Topic> temp = getTopicsByLocalCompatibility(references, null, doc.getPreprocessedText().length(),
//				rc);
//
//		end = System.currentTimeMillis();
//		System.out.println("Time for getting topics: " + (end - newStart) + " ms");
//
//		// calculateRelatedness(temp, rc);
//
//		List<Topic> topics = new ArrayList<Topic>();
//		int i = 0;
//		for (Topic topic : temp) {
//			topic.setIndex(i++);
//			topics.add(topic);
//		}
//
//		end = System.currentTimeMillis();
//		System.out.println("Total time for topic detection: " + (end - start) + " ms\n");
//
//		return topics;
//	}
//
//	public List<Topic> getTopics(PreprocessedDocument doc, Set<Position> positions, RelatednessCache rc, Language lang,
//			NLPModel model) throws Exception {
//
//		long start = System.currentTimeMillis();
//
//		if (rc == null)
//			rc = new RelatednessCache(disambiguator.getArticleComparer());
//
//		Vector<TopicReference> references = getReferencesByPosition(doc.getPreprocessedText(), positions);
//
//		Vector<TopicReference> additinalReferences;
//		if (model.equals(NLPModel.POS)) {
//			additinalReferences = getReferencesByPOS(doc.getPreprocessedText(), lang);
//		} else if (model.equals(NLPModel.NER)) {
//			additinalReferences = getReferencesByNER(doc.getPreprocessedText(), lang);
//		} else if (model.equals(NLPModel.NGRAM)) {
//			additinalReferences = getReferencesByNGram(doc.getPreprocessedText(), lang);
//		} else {
//			additinalReferences = getReferencesByNGram(doc.getPreprocessedText(), lang);
//		}
//
//		for (TopicReference reference : additinalReferences) {
//			Position pos = reference.getPosition();
//			if (!positions.contains(pos)) {
//				references.add(reference);
//			}
//		}
//
//		Collections.sort(references);
//
//		long end = System.currentTimeMillis();
//		System.out.println("Time for getting references: " + (end - start) + " ms");
//
//		long newStart = System.currentTimeMillis();
//
//		Collection<Topic> temp = getTopicsByLocalCompatibility(references, doc.getContextText(),
//				doc.getOriginalText().length(), rc);
//
//		end = System.currentTimeMillis();
//		System.out.println("Time for getting topics: " + (end - newStart) + " ms");
//
//		// calculateRelatedness(temp, rc);
//
//		List<Topic> topics = new ArrayList<Topic>();
//		int i = 0;
//		for (Topic topic : temp) {
//			if (!doc.isTopicBanned(topic.getId())) {
//				topic.setIndex(i++);
//				topics.add(topic);
//			}
//		}
//
//		end = System.currentTimeMillis();
//		System.out.println("Total time for topic detection: " + (end - start) + " ms\n");
//
//		return topics;
//	}
//
//	private NLPPreprocessor getNLPPreprocessor(Language lang) {
//		NLPPreprocessor preprocessor = lang2preprocessor.get(lang);
//		if (preprocessor == null) {
//			if (lang.equals(Language.EN) || lang.equals(Language.DE) || lang.equals(Language.ZH))
//				preprocessor = new StanfordNLPPreprocessor(NLPConfigFile, lang);
//			else if (lang.equals(Language.ES))
//				preprocessor = new OpenNLPPreprocessor(NLPConfigFile, lang);
//			lang2preprocessor.put(lang, preprocessor);
//		}
//
//		return preprocessor;
//	}
//
//	public Vector<TopicReference> getNERReferencesWithoutContext(String text, Language lang) {
//		Vector<TopicReference> references = new Vector<TopicReference>();
//
//		NLPPreprocessor preprocessor = getNLPPreprocessor(lang);
//		String segmentedText = preprocessor.segmentation(text);
//		List<Position> positions = preprocessor.NERPosition(segmentedText);
//
//		int currentIndex = 0;
//
//		for (Position position : positions) {
//			String entity = segmentedText.substring(position.getStart(), position.getEnd());
//			if (lang.equals(Language.ZH)) {
//				entity = entity.replace(" ", "");
//			}
//			MongoLabel label = new MongoLabel(entity);
//
//			if (label.exists() && label.getLinkProbability() >= disambiguator.getMinLinkProbability()) {
//				if (lang.equals(Language.ZH)) {
//					int start = text.indexOf(entity, currentIndex);
//					int end = start + entity.length();
//					position = new Position(start, end);
//					currentIndex = end;
//				}
//				TopicReference ref = new TopicReference(label, position);
//				references.add(ref);
//
//				// System.out.println(" - ref: " + ngram +
//				// label.getLinkProbability()) ;
//			}
//		}
//
//		return references;
//	}
//
//	public Vector<TopicReference> getReferencesByNER(String text, Language lang) {
//		Vector<TopicReference> references = new Vector<TopicReference>();
//
//		NLPPreprocessor preprocessor = getNLPPreprocessor(lang);
//		String segmentedText = preprocessor.segmentation(text);
//		List<Position> positions = preprocessor.NERPosition(segmentedText);
//
//		String s = "$ " + segmentedText + " $";
//
//		// would just match all non-word chars, but we don't want to match utf
//		// chars
//		Pattern p = Pattern.compile(SPLITTER);
//		Matcher m = p.matcher(s);
//
//		Vector<Integer> matchIndexes = new Vector<Integer>();
//
//		while (m.find())
//			matchIndexes.add(m.start());
//
//		HashMap<Integer, Integer> startPosition2index = new HashMap<Integer, Integer>();
//		HashMap<Integer, Integer> endPosition2index = new HashMap<Integer, Integer>();
//
//		for (int i = 0; i < matchIndexes.size(); i++) {
//			// first + 1, then - 2 (due to the added head "$ ")
//			int startPosition = matchIndexes.elementAt(i) - 1;
//			// - 2 (due to the added head "$ ")
//			int endPosition = matchIndexes.elementAt(i) - 2;
//
//			startPosition2index.put(startPosition, i);
//			endPosition2index.put(endPosition, i);
//		}
//		int currentIndex = 0;
//		for (Position position : positions) {
//			String entity = segmentedText.substring(position.getStart(), position.getEnd());
//			if (lang.equals(Language.ZH)) {
//				entity = entity.replace(" ", "");
//			}
//			MongoLabel label = new MongoLabel(entity);
//
//			if (label.exists() && label.getLinkProbability() >= disambiguator.getMinLinkProbability()) {
//				Integer i = startPosition2index.get(position.getStart());
//				Integer j = endPosition2index.get(position.getEnd());
//
//				String context = "";
//				if (!(i == null) && !(j == null)) {
//					int matchIndex = i - CONTEXT_WINDOW_SIZE / 2;
//					if (matchIndex < 0)
//						matchIndex = 0;
//					int contextStartIndex = matchIndexes.elementAt(matchIndex) + 1;
//					matchIndex = j + CONTEXT_WINDOW_SIZE / 2;
//					if (matchIndex > matchIndexes.size() - 1)
//						matchIndex = matchIndexes.size() - 1;
//					int contextEndIndex = matchIndexes.elementAt(matchIndex) + 1;
//					context = s.substring(contextStartIndex, contextEndIndex);
//				}
//
//				if (lang.equals(Language.ZH)) {
//					int start = text.indexOf(entity, currentIndex);
//					int end = start + entity.length();
//
//					position = new Position(start, end, position.getType());
//					currentIndex = end;
//				}
//
//				TopicReference ref = new TopicReference(label, position);
//				ref.setContext(context);
//				references.add(ref);
//				if (this.newMentionDetector != null) {
//					Mention mention = new Mention(position, entity, Mention.Type.NI);
//					getSubTerm(mention);
//					this.newMentionDetector.addMention(mention);
//				}
//				// System.out.println(" - ref: " + ngram +
//				// label.getLinkProbability()) ;
//			} else {
//				if (this.newMentionDetector != null) {
//					Mention mention = new Mention(position, entity, Mention.Type.NO);
//					getSubTerm(mention);
//					this.newMentionDetector.addMention(mention);
//				}
//			}
//		}
//
//		return references;
//	}
//
//	// public Vector<TopicReference> getPOSReferences(String text, Language
//	// lang) {
//	// Vector<TopicReference> references = new Vector<TopicReference>();
//	// NLPPreprocessor pre = getNLPPreprocessor(lang);
//	// String entities = pre.POSTagging(text);
//	// int n = 3;
//	// for (int i = 0; i < entities.split(" ").length; i++) {
//	// String labeltext = "";
//	// for (int j = 0; j < Math.min(n, entities.split(" ").length - i); j++) {
//	// String entity = entities.split(" ")[i + j];
//	// if (entity.split("/").length < 2
//	// || (!entity.split("/")[1].contains("N") &&
//	// !entity.split("/")[1].contains("名"))) {
//	// break;
//	// }
//	// labeltext = labeltext + entities.split(" ")[i + j].split("/")[0];
//	// Label label = new Label(wikipedia.getEnvironment(), labeltext,
//	// disambiguator.getTextProcessor());
//	//
//	// if (label.exists() && label.getLinkProbability() >=
//	// disambiguator.getMinLinkProbability()) {
//	// int start = text.indexOf(labeltext);
//	// int end = start + labeltext.length();
//	// Position pos = new Position(start, end);
//	// String context = labeltext;
//	// TopicReference ref = new TopicReference(label, pos);
//	// ref.setContext(context);
//	// if (start != -1) {
//	// references.add(ref);
//	// }
//	//
//	// // System.out.println(" - ref: " + ngram +
//	// // label.getLinkProbability()) ;
//	// }
//	// }
//	//
//	// }
//	// return references;
//	//
//	// }
//
//	public Set<Mention> getNewMentions() {
//		if (this.newMentionDetector != null) {
//			return this.newMentionDetector.getMentions();
//		} else {
//			return null;
//		}
//	}
//
//	public Vector<TopicReference> getReferencesByPOS(String text, Language lang) {
//		if (lang.equals(Language.ZH))
//			return getReferencesOfChineseByPOS(text);
//		else
//			return getReferencesOfNonChineseByPOS(text, lang);
//	}
//
//	public Set<Mention> getMentions(String text, Language lang, String[] models) {
//		// this.getNERReferences(text, lang);
//		// this.getPOSReferences(text, lang);
//		// //this.getNgramReferences(text, lang);
//		this.newMentionDetector.removeAllMentions();
//		for (String model : models) {
//			if (model.equals(NLPModel.NER.toString())) {
//				this.getReferencesByNER(text, lang);
//			} else if (model.equals(NLPModel.NGRAM.toString())) {
//				this.getReferencesByNGram(text, lang);
//			} else if (model.equals(NLPModel.POS.toString())) {
//				this.getReferencesByPOS(text, lang);
//			}
//		}
//		return this.newMentionDetector.getMentions();
//
//	}
//
//	public Vector<TopicReference> getReferencesOfNonChineseByPOS(String text, Language lang) {
//		Vector<TopicReference> references = new Vector<TopicReference>();
//		NLPPreprocessor preprocessor = getNLPPreprocessor(lang);
//		LinkedHashMap<Position, String> pos2type = preprocessor.POSTaggingPositionAndTag(text);
//		Pattern tagPattern = Pattern.compile(POSNOUNTAGS);
//		// if(this.newMentionDetector!=null){
//		// this.newMentionDetector.removeAllMentions();
//		// }
//		ArrayList<Entry<Position, String>> pos2typeArray = new ArrayList<Entry<Position, String>>();
//		for (Entry<Position, String> unit : pos2type.entrySet()) {
//			pos2typeArray.add(unit);
//		}
//
//		Pattern p = Pattern.compile(SPLITTER);
//		for (int i = 0; i < pos2typeArray.size(); i++) {
//			Position startPosition = pos2typeArray.get(i).getKey();
//			String tag = "";
//			int startIndex = startPosition.getStart();
//
//			if (p.matcher(text.substring(startIndex, startPosition.getEnd())).matches())
//				continue;
//
//			for (int j = i; j < pos2typeArray.size(); j++) {
//				if ((j - i) > disambiguator.getMaxLabelLength())
//					break;
//				// ----of start
//				// Position tempPos=pos2typeArray.get(j).getKey();
//				// if(text.substring(tempPos.getStart(),tempPos.getEnd()).equals("of")){
//				// tag=tag+"of";
//				// }else{
//				// tag=tag+pos2typeArray.get(j).getValue();
//				// }
//				// ----of end;
//				tag = tag + pos2typeArray.get(j).getValue();
//				if (tagPattern.matcher(tag).matches()) {
//					Position endPosition = pos2typeArray.get(j).getKey();
//					int endIndex = endPosition.getEnd();
//					String ngram = text.substring(startIndex, endIndex);
//
//					if (!ngram.trim().equals("") && !wikipedia.getConfig().isStopword(ngram)
//							&& !p.matcher(ngram).matches()) {
//
//						// TODO: test if we need escapes here
//						MongoLabel label = new MongoLabel(ngram);
//
//						if (label.exists() && label.getLinkProbability() >= disambiguator.getMinLinkProbability()) {
//							Position position = new Position(startIndex, endIndex);
//							int matchStartIndex = i - CONTEXT_WINDOW_SIZE / 2;
//							if (matchStartIndex < 0)
//								matchStartIndex = 0;
//							int matchEndIndex = j + CONTEXT_WINDOW_SIZE / 2;
//							if (matchEndIndex > pos2typeArray.size() - 1)
//								matchEndIndex = pos2typeArray.size() - 1;
//							StringBuilder sb = new StringBuilder();
//							while (matchStartIndex <= matchEndIndex) {
//								Position pos = pos2typeArray.get(matchStartIndex).getKey();
//								String s = text.substring(pos.getStart(), pos.getEnd());
//								if (!p.matcher(s).matches())
//									sb.append(s).append(" ");
//								matchStartIndex++;
//							}
//							String context = sb.toString();
//
//							TopicReference ref = new TopicReference(label, position);
//							ref.setContext(context);
//							references.add(ref);
//							if (this.newMentionDetector != null) {
//								Mention mention = new Mention(position, ngram, Mention.Type.GI);
//								mention.getRelatedLabel().add(label);
//								this.newMentionDetector.addMention(mention);
//
//							}
//
//						} else if (this.newMentionDetector != null && this.newMentionDetector.isNewMention(ngram)) {
//							Position position = new Position(startIndex, endIndex);
//							this.newMentionDetector.addMention(new Mention(position, ngram, Mention.Type.GO));
//						}
//					}
//				}
//			}
//		}
//
//		//
//		return references;
//	}
//
//	private void getSubTerm(Mention mention) {
//		String term = mention.getTerm();
//		String[] words = term.split(" ");
//		for (int i = 0; i < words.length; i++) {
//			String ngram = "";
//			for (int j = i; j < words.length; j++) {
//				ngram = ngram + words[j] + " ";
//				ngram = ngram.trim();
//				MongoLabel label = new MongoLabel(ngram);
//				if (label.exists()) {
//					mention.getRelatedLabel().add(label);
//				} else if (this.newMentionDetector.isNewMention(ngram)) {
//					mention.getRelatedSubTerm().add(ngram);
//				}
//
//			}
//		}
//
//	}
//
//	public Vector<TopicReference> getReferencesOfChineseByPOS(String text) {
//		Vector<TopicReference> references = new Vector<TopicReference>();
//
//		NLPPreprocessor preprocessor = getNLPPreprocessor(Language.ZH);
//		String segmentedText = preprocessor.segmentation(text);
//		// List<Position> positions = preprocessor.segmentationPosition(text);
//		Pattern p = Pattern.compile(POSNOUNTAGS);
//		LinkedHashMap<Position, String> pos2type = preprocessor.POSTaggingPositionAndTag(segmentedText);
//
//		int currentIndex = 0;
//		ArrayList<Entry<Position, String>> pos2typeArray = new ArrayList<Entry<Position, String>>();
//		for (Entry<Position, String> unit : pos2type.entrySet()) {
//			pos2typeArray.add(unit);
//		}
//		Pattern tagPattern = Pattern.compile(POSNOUNTAGS);
//		for (int i = 0; i < pos2typeArray.size(); i++) {
//			Position startPosition = pos2typeArray.get(i).getKey();
//			String tag = "";
//			StringBuilder originWords = new StringBuilder();
//			int startIndex = startPosition.getStart();
//
//			if (p.matcher(segmentedText.substring(startIndex, startPosition.getEnd())).matches())
//				continue;
//
//			for (int j = i; j < pos2typeArray.size(); j++) {
//
//				if ((j - i) > disambiguator.getMaxLabelLength())
//					break;
//				tag = tag + pos2typeArray.get(j).getValue();
//				Position wordPosition = pos2typeArray.get(j).getKey();
//				originWords = originWords
//						.append(segmentedText.substring(wordPosition.getStart(), wordPosition.getEnd()));
//				if (tagPattern.matcher(tag).matches()) {
//					// Position endPosition = pos2typeArray.get(j).getKey();
//					// int endIndex = endPosition.getEnd();
//
//					int start = text.indexOf(originWords.toString(), currentIndex);
//					if (start == -1) {
//						break;
//					}
//
//					int end = start + originWords.toString().length();
//
//					String ngram = text.substring(start, end);
//
//					if (!ngram.trim().equals("") && !wikipedia.getConfig().isStopword(ngram)
//							&& !p.matcher(ngram).matches()) {
//
//						// TODO: test if we need escapes here
//						MongoLabel label = new MongoLabel(ngram);
//
//						if (label.exists() && label.getLinkProbability() >= disambiguator.getMinLinkProbability()) {
//
//							Position position = new Position(start, end);
//							int matchStartIndex = i - CONTEXT_WINDOW_SIZE / 2;
//							if (matchStartIndex < 0)
//								matchStartIndex = 0;
//							int matchEndIndex = j + CONTEXT_WINDOW_SIZE / 2;
//							if (matchEndIndex > pos2typeArray.size() - 1)
//								matchEndIndex = pos2typeArray.size() - 1;
//							StringBuilder sb = new StringBuilder();
//							while (matchStartIndex <= matchEndIndex) {
//								Position pos = pos2typeArray.get(matchStartIndex).getKey();
//								String s = segmentedText.substring(pos.getStart(), pos.getEnd());
//								if (!p.matcher(s).matches())
//									sb.append(s).append(" ");
//								matchStartIndex++;
//							}
//							String context = sb.toString();
//							// if(label.getText().equals("记者"))
//							// System.out.println(ngram);
//							TopicReference ref = new TopicReference(label, position);
//							ref.setContext(context);
//							references.add(ref);
//						}
//					}
//				}
//
//			}
//
//			int nextCurrent = startPosition.getEnd() - startPosition.getStart();
//			currentIndex = currentIndex + nextCurrent;
//			// currentIndex++;
//		}
//
//		//
//		return references;
//		// for(Entry<Position, String> unit :pos2type.entrySet()){
//		//
//		//
//		// if(p.matcher(unit.getValue()).matches()){
//		// ngramPositions.add(unit.getKey());
//		//
//		// }else{
//		// //references.addAll(getPOSNgram(ngramPositions,segmentedText));
//		// int start=-1;
//		// int end=-1;
//		// for(TopicReference
//		// cachedRef:getPOSNgram(ngramPositions,segmentedText)){
//		// start = text.indexOf(cachedRef.getLabel().getText(), currentIndex);
//		// end = start + cachedRef.getLabel().getText().length();
//		// Position position = new Position(start, end);
//		// TopicReference ref =new
//		// TopicReference(cachedRef.getLabel(),position);
//		// ref.setContext(cachedRef.getContext());
//		// references.add(ref);
//		// }
//		// currentIndex = end;
//		// ngramPositions.clear();
//		// }
//		//
//		//
//		//
//		// }
//		// for(TopicReference
//		// cachedRef:getPOSNgram(ngramPositions,segmentedText)){
//		// int start = text.indexOf(cachedRef.getLabel().getText(),
//		// currentIndex);
//		// int end = start + cachedRef.getLabel().getText().length();
//		// Position position = new Position(start, end);
//		// currentIndex = end;
//		// TopicReference ref =new
//		// TopicReference(cachedRef.getLabel(),position);
//		// ref.setContext(cachedRef.getContext());
//		// references.add(ref);
//		// }
//		// return references;
//	}
//
//	public Vector<TopicReference> getReferencesByNGram(String text, Language lang) {
//		if (lang.equals(Language.ZH))
//			return getReferencesOfChineseByNGram(text);
//		else
//			return getReferencesOfNonChineseByNGram(text);
//	}
//
//	public Vector<TopicReference> getReferencesOfChineseByNGram(String text) {
//		Vector<TopicReference> references = new Vector<TopicReference>();
//
//		NLPPreprocessor preprocessor = getNLPPreprocessor(Language.ZH);
//		List<Position> positions = preprocessor.segmentationPosition(text);
//
//		Pattern p = Pattern.compile(SPLITTER);
//
//		for (int i = 0; i < positions.size(); i++) {
//			Position startPosition = positions.get(i);
//			int startIndex = startPosition.getStart();
//
//			if (p.matcher(text.substring(startIndex, startPosition.getEnd())).matches())
//				continue;
//
//			for (int j = Math.min(i + disambiguator.getMaxLabelLength(), positions.size() - 1); j >= i; j--) {
//				Position endPosition = positions.get(j);
//				int endIndex = endPosition.getEnd();
//				String ngram = text.substring(startIndex, endIndex);
//
//				if (!ngram.trim().equals("") && !wikipedia.getConfig().isStopword(ngram)
//						&& !p.matcher(ngram).matches()) {
//
//					// TODO: test if we need escapes here
//					MongoLabel label = new MongoLabel(ngram);
//
//					if (label.exists() && label.getLinkProbability() >= disambiguator.getMinLinkProbability()) {
//						Position position = new Position(startIndex, endIndex);
//						int matchStartIndex = i - CONTEXT_WINDOW_SIZE / 2;
//						if (matchStartIndex < 0)
//							matchStartIndex = 0;
//						int matchEndIndex = j + CONTEXT_WINDOW_SIZE / 2;
//						if (matchEndIndex > positions.size() - 1)
//							matchEndIndex = positions.size() - 1;
//						StringBuilder sb = new StringBuilder();
//						while (matchStartIndex <= matchEndIndex) {
//							Position pos = positions.get(matchStartIndex);
//							String s = text.substring(pos.getStart(), pos.getEnd());
//							if (!p.matcher(s).matches())
//								sb.append(s).append(" ");
//							matchStartIndex++;
//						}
//						String context = sb.toString();
//
//						TopicReference ref = new TopicReference(label, position);
//						ref.setContext(context);
//						references.add(ref);
//
//						// System.out.println(" - ref: " + ngram +
//						// label.getLinkProbability()) ;
//					}
//				}
//			}
//		}
//
//		return references;
//	}
//
//	public Vector<TopicReference> getReferencesOfNonChineseByNGram(String text) {
//
//		Vector<TopicReference> references = new Vector<TopicReference>();
//
//		String s = "$ " + text + " $";
//
//		// would just match all non-word chars, but we don't want to match utf
//		// chars
//		Pattern p = Pattern.compile(SPLITTER);
//
//		Matcher m = p.matcher(s);
//
//		Vector<Integer> matchIndexes = new Vector<Integer>();
//
//		while (m.find())
//			matchIndexes.add(m.start());
//
//		for (int i = 0; i < matchIndexes.size(); i++) {
//
//			int startIndex = matchIndexes.elementAt(i) + 1;
//
//			if (Character.isWhitespace(s.charAt(startIndex)))
//				continue;
//
//			for (int j = Math.min(i + disambiguator.getMaxLabelLength(), matchIndexes.size() - 1); j > i; j--) {
//				int endIndex = matchIndexes.elementAt(j);
//				String ngram = s.substring(startIndex, endIndex);
//
//				if (!(ngram.length() == 1 && s.substring(startIndex - 1, startIndex).equals("'"))
//						&& !ngram.trim().equals("") && !wikipedia.getConfig().isStopword(ngram)) {
//
//					// TODO: test if we need escapes here
//					MongoLabel label = new MongoLabel(ngram);
//
//					if (label.exists() && label.getLinkProbability() >= disambiguator.getMinLinkProbability()) {
//						// minus 2 for the added head "$ "
//						Position pos = new Position(startIndex - 2, endIndex - 2);
//						int matchIndex = i - CONTEXT_WINDOW_SIZE / 2;
//						if (matchIndex < 0)
//							matchIndex = 0;
//						int contextStartIndex = matchIndexes.elementAt(matchIndex) + 1;
//						matchIndex = j + CONTEXT_WINDOW_SIZE / 2;
//						if (matchIndex > matchIndexes.size() - 1)
//							matchIndex = matchIndexes.size() - 1;
//						int contextEndIndex = matchIndexes.elementAt(matchIndex) + 1;
//						String context = s.substring(contextStartIndex, contextEndIndex);
//						TopicReference ref = new TopicReference(label, pos);
//						ref.setContext(context);
//						references.add(ref);
//						if (this.newMentionDetector != null) {
//							this.newMentionDetector.addMention(new Mention(pos, ngram, Mention.Type.GI));
//						}
//
//						// System.out.println(" - ref: " + ngram +
//						// label.getLinkProbability()) ;
//					}
//				}
//			}
//		}
//		return references;
//	}
//
//	public Vector<TopicReference> getReferencesByPosition(String text, Set<Position> positions) {
//
//		Vector<TopicReference> references = new Vector<TopicReference>();
//
//		for (Position pos : positions) {
//
//			int startIndex = pos.getStart();
//			int endIndex = pos.getEnd();
//			String ngram = text.substring(startIndex, endIndex);
//
//			// if (!(ngram.length() == 1 && s.substring(startIndex - 1,
//			// startIndex).equals("'"))
//			// && !ngram.trim().equals("") &&
//			// !wikipedia.getConfig().isStopword(ngram)) {
//
//			// For AIDA evaluation
//			ngram = ngram.replace("-", " ");
//			if (isAllUpper(ngram)) {
//				String[] terms = ngram.split(" ");
//				StringBuffer sb = new StringBuffer();
//				for (String term : terms) {
//					sb.append(Character.toUpperCase(term.charAt(0)));
//					sb.append(term.substring(1).toLowerCase());
//					sb.append(" ");
//				}
//				ngram = sb.toString().trim();
//			}
//
//			// TODO: test if we need escapes here
//			MongoLabel label = new MongoLabel( ngram);
//
//			// TODO: add context
//			if (label.exists() && label.getLinkProbability() >= disambiguator.getMinLinkProbability()) {
//				// // minus 2 for the added head "$ "
//				// Position pos = new Position(startIndex - 2, endIndex - 2);
//				// int matchIndex = i - CONTEXT_WINDOW_SIZE / 2;
//				// if (matchIndex < 0)
//				// matchIndex = 0;
//				// int contextStartIndex = matchIndexes.elementAt(matchIndex) +
//				// 1;
//				// matchIndex = j + CONTEXT_WINDOW_SIZE / 2;
//				// if (matchIndex > matchIndexes.size() - 1)
//				// matchIndex = matchIndexes.size() - 1;
//				// int contextEndIndex = matchIndexes.elementAt(matchIndex) + 1;
//				// String context = s.substring(contextStartIndex,
//				// contextEndIndex);
//				TopicReference ref = new TopicReference(label, pos);
//				// ref.setContext(context);
//				references.add(ref);
//
//				// System.out.println(" - ref: " + ngram +
//				// label.getLinkProbability()) ;
//			}
//			// }
//		}
//		return references;
//	}
//
//	private static boolean isAllUpper(String s) {
//		for (char c : s.toCharArray()) {
//			if (Character.isLetter(c) && Character.isLowerCase(c)) {
//				return false;
//			}
//		}
//		return true;
//	}
//
//	// retrieving the topics and their references for each different lower-case label text
//	private Collection<Topic> getTopicsByLocalCompatibility(Vector<TopicReference> references, String contextText,
//			int docLength, RelatednessCache cache) throws Exception {
//		ArrayList<Topic> chosenTopics = new ArrayList<Topic>();
//
//		// get context articles from unambiguous Labels
//		// Vector<Label> unambigLabels = new Vector<Label>();
//		// for (TopicReference ref : references) {
//		// Label label = ref.getLabel();
//		//
//		// Label.Sense[] senses = label.getSenses();
//		// if (senses.length > 0) {
//		// if (senses.length == 1 || senses[0].getPriorProbability() > 1 -
//		// disambiguator.getMinSenseProbability())
//		// unambigLabels.add(label);
//		// }
//		// }
//
//		// get context articles from additional context text
//		// for (TopicReference ref : getReferences(contextText)) {
//		// Label label = ref.getLabel();
//		// Label.Sense[] senses = label.getSenses();
//		// if (senses.length > 0) {
//		// if (senses.length == 1 || senses[0].getPriorProbability() > 1 -
//		// disambiguator.getMinSenseProbability()) {
//		// unambigLabels.add(label);
//		// }
//		// }
//		// }
//
//		// Context contains top unambiguous articles and their weights based on
//		// label LinkProbability and relatedness to other unambiguous articles
//		Context context;
//		// if (cache == null)
//		// context = new Context(unambigLabels, new
//		// RelatednessCache(disambiguator.getArticleComparer()),
//		// disambiguator.getMaxContextSize());
//		// else
//		// context = new Context(unambigLabels, cache,
//		// disambiguator.getMaxContextSize());
//		// unambigLabels = null;
//
//		// now disambiguate all references
//		// unambig references are still processed here, because we need to
//		// calculate relatedness to context anyway.
//
//		// build a cache of valid senses for each phrase, since the same phrase
//		// may occur more than once, but will always be disambiguated the same
//		// way except the context similarity
//		// lower-case label text -> senses
//		HashMap<String, ArrayList<CachedSense>> senseCache = new LinkedHashMap<String, ArrayList<CachedSense>>();
//		// lower-case label text -> topic references
//		HashMap<String, ArrayList<TopicReference>> referenceCache = new HashMap<String, ArrayList<TopicReference>>();
//
//		for (TopicReference ref : references) {
//			// System.out.println("disambiguating ref: " +
//			// ref.getLabel().getText()) ;
//
//			ArrayList<TopicReference> cachedReferences = referenceCache.get(ref.getLabel().getText().toLowerCase());
//			if (cachedReferences == null) {
//				cachedReferences = new ArrayList<TopicReference>();
//				referenceCache.put(ref.getLabel().getText().toLowerCase(), cachedReferences);
//			}
//			cachedReferences.add(ref);
//
//			ArrayList<CachedSense> cachedSenses = senseCache.get(ref.getLabel().getText().toLowerCase());
//			if (cachedSenses == null) {
//				// we haven't seen this label in this document before
//				cachedSenses = new ArrayList<CachedSense>();
//
//				for (MongoLabel.MongoSense sense : ref.getLabel().getSenses()) {
//
//					if (sense.getPriorProbability() < disambiguator.getMinSenseProbability())
//						break;
////
////					if (!allowDisambiguations && sense.getType() == PageType.disambiguation)
////						continue;
//
//					// relatedness to all context anchors
//					// double relatedness = context.getRelatednessTo(sense);
//					double relatedness = 0;
//
//					// the probability that the label goes to this sense
//					double commonness = sense.getPriorProbability();
//
//					// TODO: the context similarity between the reference and
//					// this sense
//					double similarity = 0;
//
//					// double disambigConfidence =
//					// disambiguator.getProbabilityOfSense(commonness,
//					// relatedness,
//					// similarity, context);
//					double disambigConfidence = disambiguator.getProbabilityOfSense(commonness, relatedness, similarity,
//							null);
//					// System.out.println(" - sense " + sense + ", " +
//					// disambigConfidence) ;
//
//					if (disambigConfidence > disambigConfidenceThreshold) {
//						// there is at least a chance that this is a valid sense
//						// for the link (there may be more than one)
//
//						// CachedSense cs = new CachedSense(sense.getId(),
//						// commonness, relatedness, similarity,
//						// disambigConfidence);
//						CachedSense cs = new CachedSense(sense.getId(), commonness, relatedness, similarity,
//								disambigConfidence);
//						cachedSenses.add(cs);
//					}
//				}
//				Collections.sort(cachedSenses);
//
//				senseCache.put(ref.getLabel().getText().toLowerCase(), cachedSenses);
//			} else {
//				// TODO: there might be problem when both "apple" and "Apple" appear in text
//				for (CachedSense sense : cachedSenses) {
//					double relatedness = sense.relatedness;
//					double commonness = sense.commonness;
//					double maxSimilarity = sense.maxSimilarity;
//					double maxDisambigConfidence = sense.maxDisambigConfidence;
//
//					// TODO: the context similarity between the reference and
//					// this sense
//					double newSimilarity = 0;
//
//					// double newDisambigConfidence =
//					// disambiguator.getProbabilityOfSense(commonness,
//					// relatedness,
//					// newSimilarity, context);
//					double newDisambigConfidence = disambiguator.getProbabilityOfSense(commonness, relatedness,
//							newSimilarity, null);
//
//					if (newSimilarity > maxSimilarity)
//						sense.maxSimilarity = newSimilarity;
//					if (newDisambigConfidence > maxDisambigConfidence)
//						sense.maxDisambigConfidence = newDisambigConfidence;
//					sense.totalSimilarity += newSimilarity;
//					sense.totalDisambigConfidence += newDisambigConfidence;
//				}
//				Collections.sort(cachedSenses);
//			}
//		}
//
//		for (String labelText : senseCache.keySet()) {
//			ArrayList<CachedSense> cachedSenses = senseCache.get(labelText);
//			ArrayList<TopicReference> cachedReferences = referenceCache.get(labelText);
//			if (strictDisambiguation) {
//				// just get top sense
//				if (!cachedSenses.isEmpty()) {
//					CachedSense sense = cachedSenses.get(0);
//					Topic topic = new Topic(wikipedia, sense.id, sense.relatedness, sense.commonness, docLength);
//					topic.setMaxSimilarity(sense.maxSimilarity);
//					topic.setMaxDisambigConfidence(sense.maxDisambigConfidence);
//					topic.setTotalSimilarity(sense.totalSimilarity);
//					topic.setTotalDisambigConfidence(sense.totalDisambigConfidence);
//					for (TopicReference ref : cachedReferences) {
//						topic.addReference(ref);
//					}
//					chosenTopics.add(topic);
//				}
//			} else {
//				// get all senses
//				for (CachedSense sense : cachedSenses) {
//					Topic topic = new Topic(wikipedia, sense.id, sense.relatedness, sense.commonness, docLength);
//					topic.setMaxSimilarity(sense.maxSimilarity);
//					topic.setMaxDisambigConfidence(sense.maxDisambigConfidence);
//					topic.setTotalSimilarity(sense.totalSimilarity);
//					topic.setTotalDisambigConfidence(sense.totalDisambigConfidence);
//					for (TopicReference ref : cachedReferences) {
//						topic.addReference(ref);
//					}
//					chosenTopics.add(topic);
//				}
//			}
//		}
//
//		return chosenTopics;
//	}
//
///*	private Collection<Topic> getTopics(Vector<TopicReference> references, String contextText, int docLength,
//			RelatednessCache cache) throws Exception {
//		ArrayList<Topic> chosenTopics = new ArrayList<Topic>();
//
//		// get context articles from unambiguous Labels
//		Vector<Label> unambigLabels = new Vector<Label>();
//		for (TopicReference ref : references) {
//			MongoLabel label = ref.getLabel();
//
//			MongoLabel.Sense[] senses = label.getSenses();
//			if (senses.length > 0) {
//				if (senses.length == 1 || senses[0].getPriorProbability() > 1 - disambiguator.getMinSenseProbability())
//					unambigLabels.add(label);
//			}
//		}
//
//		// get context articles from additional context text
//		for (TopicReference ref : getReferencesOfNonChineseByNGram(contextText)) {
//			MongoLabel label = ref.getLabel();
//			MongoLabel.Sense[] senses = label.getSenses();
//			if (senses.length > 0) {
//				if (senses.length == 1
//						|| senses[0].getPriorProbability() > 1 - disambiguator.getMinSenseProbability()) {
//					unambigLabels.add(label);
//				}
//			}
//		}
//
//		// Context contains top unambiguous articles and their weights based on
//		// label LinkProbability and relatedness to other unambiguous articles
//		Context context;
//		if (cache == null)
//			context = new Context(unambigLabels, new RelatednessCache(disambiguator.getArticleComparer()),
//					disambiguator.getMaxContextSize());
//
//		else
//			context = new Context(unambigLabels, cache, disambiguator.getMaxContextSize());
//		unambigLabels = null;
//
//		// now disambiguate all references
//		// unambig references are still processed here, because we need to
//		// calculate relatedness to context anyway.
//
//		// build a cache of valid senses for each phrase, since the same phrase
//		// may occur more than once, but will always be disambiguated the same
//		// way except the context similarity
//		HashMap<String, ArrayList<CachedSense>> senseCache = new LinkedHashMap<String, ArrayList<CachedSense>>();
//		HashMap<String, ArrayList<TopicReference>> referenceCache = new HashMap<String, ArrayList<TopicReference>>();
//
//		for (TopicReference ref : references) {
//			// System.out.println("disambiguating ref: " +
//			// ref.getLabel().getText()) ;
//
//			ArrayList<TopicReference> cachedReferences = referenceCache.get(ref.getLabel().getText().toLowerCase());
//			if (cachedReferences == null) {
//				cachedReferences = new ArrayList<TopicReference>();
//				referenceCache.put(ref.getLabel().getText().toLowerCase(), cachedReferences);
//			}
//			cachedReferences.add(ref);
//
//			ArrayList<CachedSense> cachedSenses = senseCache.get(ref.getLabel().getText().toLowerCase());
//			if (cachedSenses == null) {
//				// we havent seen this label in this document before
//				cachedSenses = new ArrayList<CachedSense>();
//
//				for (MongoLabel.Sense sense : ref.getLabel().getSenses()) {
//
//					if (sense.getPriorProbability() < disambiguator.getMinSenseProbability())
//						break;
//
////					if (!allowDisambiguations && sense.getType() == PageType.disambiguation)
////						continue;
//
//					// relatedness to all context anchors
//					double relatedness = context.getRelatednessTo(sense);
//					// the probability that the label goes to this sense
//					double commonness = sense.getPriorProbability();
//					// TODO: the context similarity between the reference and
//					// this sense
//					double similarity = 0;
//					double disambigConfidence = disambiguator.getProbabilityOfSense(commonness, relatedness, similarity,
//							context);
//					// System.out.println(" - sense " + sense + ", " +
//					// disambigConfidence) ;
//
//					if (disambigConfidence > 0.1) {
//						// there is at least a chance that this is a valid sense
//						// for the link (there may be more than one)
//
//						CachedSense cs = new CachedSense(sense.getId(), commonness, relatedness, similarity,
//								disambigConfidence);
//						cachedSenses.add(cs);
//					}
//				}
//				Collections.sort(cachedSenses);
//
//				senseCache.put(ref.getLabel().getText().toLowerCase(), cachedSenses);
//			} else {
//				for (CachedSense sense : cachedSenses) {
//					double relatedness = sense.relatedness;
//					double commonness = sense.commonness;
//					double maxSimilarity = sense.maxSimilarity;
//					double maxDisambigConfidence = sense.maxDisambigConfidence;
//
//					// TODO
//					double newSimilarity = 0;
//					double newDisambigConfidence = disambiguator.getProbabilityOfSense(commonness, relatedness,
//							newSimilarity, context);
//
//					if (newSimilarity > maxSimilarity)
//						sense.maxSimilarity = newSimilarity;
//					if (newDisambigConfidence > maxDisambigConfidence)
//						sense.maxDisambigConfidence = newDisambigConfidence;
//					sense.totalSimilarity += newSimilarity;
//					sense.totalDisambigConfidence += newDisambigConfidence;
//				}
//				Collections.sort(cachedSenses);
//			}
//		}
//
//		for (String labelText : senseCache.keySet()) {
//			ArrayList<CachedSense> cachedSenses = senseCache.get(labelText);
//			ArrayList<TopicReference> cachedReferences = referenceCache.get(labelText);
//			if (strictDisambiguation) {
//				// just get top sense
//				if (!cachedSenses.isEmpty()) {
//					CachedSense sense = cachedSenses.get(0);
//					Topic topic = new Topic(wikipedia, sense.id, sense.relatedness, sense.commonness, docLength);
//					topic.setMaxSimilarity(sense.maxSimilarity);
//					topic.setMaxDisambigConfidence(sense.maxDisambigConfidence);
//					topic.setTotalSimilarity(sense.totalSimilarity);
//					topic.setTotalDisambigConfidence(sense.totalDisambigConfidence);
//					for (TopicReference ref : cachedReferences) {
//						topic.addReference(ref);
//					}
//					chosenTopics.add(topic);
//				}
//			} else {
//				// get all senses
//				for (CachedSense sense : cachedSenses) {
//					Topic topic = new Topic(wikipedia, sense.id, sense.relatedness, sense.commonness, docLength);
//					topic.setMaxSimilarity(sense.maxSimilarity);
//					topic.setMaxDisambigConfidence(sense.maxDisambigConfidence);
//					topic.setTotalSimilarity(sense.totalSimilarity);
//					topic.setTotalDisambigConfidence(sense.totalDisambigConfidence);
//					for (TopicReference ref : cachedReferences) {
//						topic.addReference(ref);
//					}
//					chosenTopics.add(topic);
//				}
//			}
//		}
//
//		return chosenTopics;
//	}
//*/
//	/**
//	 * For each topic, calculate and set its relatedness to all other topics.
//	 * 
//	 * @param topics
//	 *            all topics detected in the document
//	 * @param cache
//	 *            a cache in which relatedness measures will be saved so they aren't repeatedly calculated. This may be
//	 *            null.
//	 */
//	private void calculateRelatedness(Collection<Topic> topics, RelatednessCache cache) throws Exception {
//
//		TreeSet<Article> weightedTopics = new TreeSet<Article>();
//
//		for (Topic topic : topics) {
//			if (topic.getType() != PageType.article)
//				continue;
//
//			Article art = (Article) wikipedia.getPageById(topic.getId());
//
//			art.setWeight(topic.getAverageLinkProbability() * topic.getOccurances());
//			weightedTopics.add(art);
//		}
//
//		for (Topic topic : topics) {
//
//			double totalWeight = 0;
//			double totalWeightedRelatedness = 0;
//
//			int count = 0;
//
//			for (Article art : weightedTopics) {
//				if (count++ > maxTopicsForRelatedness)
//					break;
//
//				double weightedRelatedness = art.getWeight() * cache.getRelatedness(topic, art);
//
//				totalWeight = totalWeight + art.getWeight();
//				totalWeightedRelatedness = totalWeightedRelatedness + weightedRelatedness;
//
//			}
//
//			topic.setRelatednessToOtherTopics((float) (totalWeightedRelatedness / totalWeight));
//		}
//	}
//
//	private class CachedSense implements Comparable<CachedSense> {
//
//		int id;
//		double commonness;
//		double relatedness;
//		double totalSimilarity;
//		double totalDisambigConfidence;
//		double maxSimilarity;
//		double maxDisambigConfidence;
//
//		/**
//		 * Initializes a new CachedSense
//		 * 
//		 * @param id
//		 *            the id of the article that represents this sense
//		 * @param commonness
//		 *            the prior probability of this sense given a source ngram (label)
//		 * @param relatedness
//		 *            the relatedness of this sense to the surrounding unambiguous topics
//		 * @param disambigConfidence
//		 *            the probability that this sense is valid, as defined by the disambiguator.
//		 */
//		public CachedSense(int id, double commonness, double relatedness, double similarity,
//				double disambigConfidence) {
//			this.id = id;
//			this.commonness = commonness;
//			this.relatedness = relatedness;
//			this.totalSimilarity = similarity;
//			this.totalDisambigConfidence = disambigConfidence;
//			this.maxSimilarity = similarity;
//			this.maxDisambigConfidence = disambigConfidence;
//		}
//
//		public int compareTo(CachedSense sense) {
//			return -1 * Double.valueOf(maxDisambigConfidence).compareTo(Double.valueOf(sense.maxDisambigConfidence));
//		}
//	}
//}
