package org.fiz.ise.gwifi.test.longDocument;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.shorttext.test.TestBasedOnAnnotatedDocument;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.URLUTF8Encoder;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.twelvemonkeys.imageio.stream.URLImageInputStreamSpi;

import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.LexedTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.WordToSentenceProcessor;

public class YovistoParser {

	private static Wikipedia wikipedia = WikipediaSingleton.getInstance().wikipedia;
	private static int NUMBER_OF_SENTENCES_YOVISTO = Config.getInt("NUMBER_OF_SENTENCES_YOVISTO",-1);
	private static String DATASET_TEST_YOVISTO_DATE_TITLE= Config.getString("DATASET_TEST_YOVISTO_DATE_TITLE","");
	private final static String DATASET_TEST_YOVISTO_NIF = Config.getString("DATASET_TEST_YOVISTO_NIF","");
	private Map<String, Set<String>> mapDocIDAnchorID;
	private Map<String, String> mapAnchorIDURI ;
	private Map<String, String> mapAnchorIDAnchorText;
	private Map<String, String> mapDocIDContext ;
	private Map<String, String> mapDocIDDate;
	private Map<String, String> mapDocIDTitle;


	public void initializeVariables() {
		mapDocIDAnchorID= new HashMap<>();
		mapAnchorIDURI = new HashMap<>();
		mapAnchorIDAnchorText = new HashMap<>();
		mapDocIDContext = new HashMap<>();
		mapDocIDDate = new HashMap<>();
		mapDocIDTitle = new HashMap<>();
		int isString =0;
		try {
			Model model=ModelFactory.createDefaultModel();
			model.read(new FileInputStream(DATASET_TEST_YOVISTO_NIF),null,"TTL");
			StmtIterator iter = model.listStatements();
			int counterAnchor=0;
			while (iter.hasNext()) {
				Statement stmt      = iter.nextStatement();  // get next statement
				Resource  subject   = stmt.getSubject();
				if(stmt.getPredicate().toString().contains("taIdentRef")&&subject.toString().contains("queries"))
					isString++;
				if (!subject.toString().contains("queries")) {
					Property  predicate = stmt.getPredicate();   // get the predicate
					RDFNode   object    = stmt.getObject();      // get the object
					String temp=subject.toString().replaceAll("http://yovisto.com/resource/dataset/iswc2015/doc/", "");
					if (temp.contains("#")) {
						String docID =temp.substring(0, temp.indexOf("#"));
						String anchorID=temp.replaceAll(">", "");
						String anchorText ="";
						String anchorURI="";
						String content="";
						String date="";
						if (mapDocIDAnchorID.containsKey(docID)) {
							if (!mapDocIDAnchorID.get(docID).contains(anchorID)) {
								Set<String> lst = new HashSet<>(mapDocIDAnchorID.get(docID));
								lst.add(anchorID);
								mapDocIDAnchorID.put(docID, lst);
							}
						}
						else {
							Set<String> lst = new HashSet<>();
							lst.add(anchorID);
							mapDocIDAnchorID.put(docID, lst);
						}
						if (predicate.toString().contains("anchorOf")) {
							anchorText = object.toString().substring(0, object.toString().indexOf("^^")).trim();
							mapAnchorIDAnchorText.put(anchorID, anchorText);
							counterAnchor++;
						}
						else if (predicate.toString().contains("taIdentRef")) {
							anchorURI = object.toString().replaceAll("http://dbpedia.org/resource/", "").replaceAll(">", "");
							mapAnchorIDURI.put(anchorID, anchorURI);
						}
						else if (predicate.toString().contains("isString")) {
							content = object.toString().substring(0, object.toString().indexOf("^^")).trim();
							mapDocIDContext.put(docID, content);
						}
						else if (predicate.toString().contains("date")) {
							date=object.toString().substring(0, object.toString().indexOf("^^")).trim();
							mapDocIDDate.put(docID, date);
						}
						else if (predicate.toString().contains("title")) {
							mapDocIDTitle.put(docID, object.toString().substring(0, object.toString().indexOf("^^")).trim());
						}
					}
					else {
						System.out.println(temp);
					}
				}
			}
			System.out.println("counterAnchor "+counterAnchor);
			System.out.println(mapDocIDAnchorID.size());
			int notInWiki=0;
			for(Entry<String, String> e:mapAnchorIDURI.entrySet()) {
				if (wikipedia.getArticleByTitle(getUrl(e.getValue()))==null) {
					notInWiki++;
				}
			}
//			System.out.println("size of mapDocIDAnchorID "+mapDocIDAnchorID.size());
//			System.out.println("isString "+isString);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static void main(String[] args) {
		YovistoParser parser = new YovistoParser();
		parser.initializeVariables();
		Map<List<String>, List<Category>> dataset = new HashMap<>(parser.generateDatasetEntities(NUMBER_OF_SENTENCES_YOVISTO));

		System.out.println("dataset initialized "+ dataset.size());
		parser.findAvgEntities(dataset);
		TestBasedOnAnnotatedDocument test = new TestBasedOnAnnotatedDocument();
		test.startProcessingData(dataset);
	}
	private void findAvgEntities(Map<List<String>, List<Category>> dataset) {
		double numberOfEntities=0;
		double numberOfDocs=dataset.size();
		int countSentences =0;
		for (Entry <String, String> e : mapDocIDContext.entrySet()) {
			List<String> sentences = new LinkedList<>(generateSentences(e.getValue()));
			countSentences=countSentences+sentences.size();
		}
		for(Entry<List<String>, List<Category>> e : dataset.entrySet()) {
			numberOfEntities+=e.getKey().size();
		}
		System.out.println("Avg Entities "+numberOfEntities/numberOfDocs);
	}

	public Map<String, List<Category>> generateDataset_gwifi(int countSentences) {
		Map<String, List<Category>> datasetSentences = new HashMap<>();
		Map<String, List<Category>> mapDateCategory = new HashMap<>(getMapDataCategory());
		for(Entry<String, Set<String>> entry:this.mapDocIDAnchorID.entrySet()) {
			String docID = entry.getKey();
			String content = this.mapDocIDContext.get(docID);
			List<String> sentences = new LinkedList<>(generateSentences(content));
			StringBuilder build = new StringBuilder();
			int index= countSentences;
			if (countSentences>sentences.size()) {
				index=sentences.size();
			}
			for(int i=0 ;i<index;i++) {
				String sentence = sentences.get(i);
				build.append(sentence);
			}
			String date = this.mapDocIDDate.get(docID);
			if (date!=null) {
				if (mapDateCategory.containsKey(date)) {
					List<Category> gt = new ArrayList<>(mapDateCategory.get(date));
					datasetSentences.put(build.toString(), gt);
				}
				else {
					System.out.println(docID);
				}
			}
		}
		return datasetSentences;
	}

	public Map<List<String>, List<Category>> generateDatasetEntities(int numberOfSentences) {
		int containsMap=0;
		Map<List<String>, List<Category>> datasetEntities= new HashMap<>();
		Map<String, List<Category>> mapDateCategory = new HashMap<>(getMapDataCategory());
		int numberOfAnchorsFound =0;
		int totalEntities =0;
		int uriNotInWiki=0;
		int totalAnchors=0;
		for(Entry<String, Set<String>> entry:mapDocIDAnchorID.entrySet()) {
			totalAnchors+=entry.getValue().size();
			numberOfAnchorsFound =0;
			String docID = entry.getKey();
			Set<String> setAnchorIDs = new HashSet<>(entry.getValue());
			String content = mapDocIDContext.get(docID);
			List<String> sentences = new LinkedList<>(generateSentences(content));
			StringBuilder build = new StringBuilder();
			List<String> entities = new ArrayList<>();
			int index= numberOfSentences;
			if (numberOfSentences>sentences.size()) {
				index=sentences.size();
			}
			Set<String> containedAnchors = new HashSet<>();
			int sentenceChars=0;
			for(int i=0 ;i<index;i++) {
				String sentence = sentences.get(i);
				build.append(sentence);
				sentenceChars=sentenceChars+sentence.length()+1;
				for(String anchorId: setAnchorIDs) {
					int begIndex = Integer.parseInt(anchorId.substring(anchorId.indexOf("#char=")+6, anchorId.indexOf(",")));
					int endIndex = Integer.parseInt(anchorId.substring(anchorId.indexOf(",")+1, anchorId.length()));
					if (mapAnchorIDAnchorText.containsKey(anchorId)) {
						if (sentenceChars>=endIndex&&begIndex<=sentenceChars) {
							if (!containedAnchors.contains(anchorId)) {
								if(sentence.contains(mapAnchorIDAnchorText.get(anchorId))) {
									numberOfAnchorsFound++;
									containedAnchors.add(anchorId);
									if (wikipedia.getArticleByTitle(getUrl(mapAnchorIDURI.get(anchorId)))!=null) {
										entities.add(wikipedia.getArticleByTitle(getUrl(mapAnchorIDURI.get(anchorId))).getTitle());
										totalEntities++;
									}
									else {
										uriNotInWiki++;
									}
								}
							}
						}
						else {
							//System.out.println(begIndex+" "+endIndex+" "+sentenceChars);
						}
					}
					else {
						//System.out.println(anchorId);
					}
				}
				//				for(Entry<String, String> anchor :mapAnchorsTextAnchorID.entrySet()) {
				//					int begIndex = Integer.parseInt(anchor.getValue().substring(anchor.getValue().indexOf("#char=")+6, anchor.getValue().indexOf(",")));
				//					int endIndex = Integer.parseInt(anchor.getValue().substring(anchor.getValue().indexOf(",")+1, anchor.getValue().length()));
				//					if (sentenceChars>=endIndex&&begIndex<=sentenceChars) {
				//						if (!containedAnchors.contains(anchor.getKey())) {
				//							if(sentence.contains(anchor.getKey())) {
				//								numberOfAnchorsFound++;
				//								containedAnchors.add(anchor.getKey());
				//								if (wikipedia.getArticleByTitle(getUrl(mapAnchorIDURI.get(anchor.getValue())))!=null) {
				//									entities.add(wikipedia.getArticleByTitle(getUrl(mapAnchorIDURI.get(anchor.getValue()))).getTitle());
				//								}
				//								else {
				//									uriNotInWiki++;
				//								}
				//							}
				//						}
				//					}
				//					else {
				//						//System.out.println(begIndex+" "+endIndex+" "+sentenceChars);
				//					}
				//
				//				}

			}
//			for(String contained: setAnchorIDs) {
//				if (!containedAnchors.contains(contained)) {
//					System.out.println(contained);
//				}
//			}
//			if (numberOfAnchorsFound!=setAnchorIDs.size()-1) {
//				System.out.println(numberOfAnchorsFound+ " "+setAnchorIDs.size());
//			}
//			for(String str:setAnchorIDs) {
//				System.out.println(str);
//			}
			
			String date = mapDocIDDate.get(docID);
			if (date!=null) {
				if (mapDateCategory.containsKey(date)) {
					List<Category> gt = new ArrayList<>(mapDateCategory.get(date));
					datasetEntities.put(entities, gt);
				}
				else {
					System.out.println(docID);
				}
			}
		}
		System.out.println("totalAnchors "+totalAnchors);
		System.out.println("Size of mapAnchorIDAnchorText "+mapAnchorIDAnchorText.size());
		System.out.println(datasetEntities.size());
		System.out.println("uriNotInWiki "+uriNotInWiki);
		System.out.println("totalEntities "+totalEntities);
		int numberOfEntities=0;
		for(Entry<List<String>, List<Category>> e : datasetEntities.entrySet()) {
			numberOfEntities+=e.getKey().size();
		}
		System.out.println("numberOfEntities "+numberOfEntities);
		System.out.println("number of Anchor Could not Add to the map "+containsMap);
		return datasetEntities;
	}
	public Map<String, List<Category>> generateDataset(int countSentences) {
		Map<List<String>, List<Category>> datasetEntities= new HashMap<>();
		Map<String, List<Category>> datasetSentences = new HashMap<>();
		try {
			Map<String, List<Category>> mapDateCategory = new HashMap<>(getMapDataCategory());
			Model model=ModelFactory.createDefaultModel();
			model.read(new FileInputStream(DATASET_TEST_YOVISTO_NIF),null,"TTL");
			StmtIterator iter = model.listStatements();
			Map<String, LinkedHashMap<String, String>> result = new HashMap<String, LinkedHashMap<String,String>>();
			int counterAnchor=0;
			while (iter.hasNext()) {
				Statement stmt      = iter.nextStatement();  // get next statement
				Resource  subject   = stmt.getSubject();
				if (!subject.toString().contains("queries")) {
					Property  predicate = stmt.getPredicate();   // get the predicate
					RDFNode   object    = stmt.getObject();      // get the object
					String temp=subject.toString().replaceAll("http://yovisto.com/resource/dataset/iswc2015/doc/", "");
					if (temp.contains("#")) {
						String docID =temp.substring(0, temp.indexOf("#"));
						String anchorID=temp.replaceAll(">", "");
						String anchorText ="";
						String anchorURI="";
						String content="";
						String date="";
						boolean containsKey = mapDocIDAnchorID.containsKey(docID);
						if (containsKey) {
							if (!mapDocIDAnchorID.get(docID).contains(anchorID)) {
								Set<String> lst = new HashSet<>(mapDocIDAnchorID.get(docID));
								lst.add(anchorID);
								mapDocIDAnchorID.put(docID, lst);
							}
						}
						else {
							Set<String> lst = new HashSet<>();
							lst.add(anchorID);
							mapDocIDAnchorID.put(docID, lst);
						}
						if (predicate.toString().contains("anchorOf")) {
							anchorText = object.toString().substring(0, object.toString().indexOf("^^")).trim();
							mapAnchorIDAnchorText.put(anchorID, anchorText);
							counterAnchor++;
						}
						else if (predicate.toString().contains("taIdentRef")) {
							anchorURI = object.toString().replaceAll("http://dbpedia.org/resource/", "").replaceAll(">", "");
							mapAnchorIDURI.put(anchorID, anchorURI);
						}
						else if (predicate.toString().contains("isString")) {
							content = object.toString().substring(0, object.toString().indexOf("^^")).trim();
							mapDocIDContext.put(docID, content);
						}
						else if (predicate.toString().contains("date")) {
							date=object.toString().substring(0, object.toString().indexOf("^^")).trim();
							mapDocIDDate.put(docID, date);
						}
						else if (predicate.toString().contains("title")) {
							mapDocIDTitle.put(docID, object.toString().substring(0, object.toString().indexOf("^^")).trim());
						}
					}
					else {
						System.out.println(temp);
					}
				}
			}
			System.out.println("counterAnchor "+counterAnchor);
			System.out.println(mapDocIDAnchorID.size());
			int notInWiki=0;
			for(Entry<String, String> e:mapAnchorIDURI.entrySet()) {
				if (wikipedia.getArticleByTitle(getUrl(e.getValue()))==null) {
					notInWiki++;
				}
			}
			int numberOfSentences =countSentences;
			int numberOfDocsWithEntitiy=0; 
			System.out.println("size of mapDocIDAnchorID "+mapDocIDAnchorID.size());
			int count=0;
			for(Entry<String, Set<String>> entry:mapDocIDAnchorID.entrySet()) {
				count++;
				String docID = entry.getKey();
				Set<String> setAnchors = new HashSet<>(entry.getValue());
				Map<String, String> mapAnchorsTextAnchorID = new HashMap<>();
				for(String anchorid: setAnchors) {
					if (mapAnchorIDAnchorText.get(anchorid)==null) {
						//System.out.println(anchorid);
					}
					else {
						mapAnchorsTextAnchorID.put(mapAnchorIDAnchorText.get(anchorid), anchorid);
					}
				}
				String content = mapDocIDContext.get(docID);
				List<String> sentences = new LinkedList<>(generateSentences(content));
				StringBuilder build = new StringBuilder();
				List<String> entities = new ArrayList<>();
				int index= numberOfSentences;
				if (numberOfSentences>sentences.size()) {
					index=sentences.size();
				}
				Set<String> containedAnchors = new HashSet<>();
				int sentenceChars=0;
				for(int i=0 ;i<index;i++) {
					String sentence = sentences.get(i);
					build.append(sentence);
					sentenceChars=sentenceChars+sentence.length()+1;
					for(Entry<String, String> anchor :mapAnchorsTextAnchorID.entrySet()) {
						int begIndex = Integer.parseInt(anchor.getValue().substring(anchor.getValue().indexOf("#char=")+6, anchor.getValue().indexOf(",")));
						int endIndex = Integer.parseInt(anchor.getValue().substring(anchor.getValue().indexOf(",")+1, anchor.getValue().length()));
						if (sentenceChars>=endIndex&&begIndex<=sentenceChars) {
							if (!containedAnchors.contains(anchor.getKey())) {
								if(sentence.contains(anchor.getKey())) {
									containedAnchors.add(anchor.getKey());
									if (wikipedia.getArticleByTitle(getUrl(mapAnchorIDURI.get(anchor.getValue())))!=null) {
										entities.add(wikipedia.getArticleByTitle(getUrl(mapAnchorIDURI.get(anchor.getValue()))).getTitle());
									}
								}
							}
						}
						else {
							//System.out.println(begIndex+" "+endIndex+" "+sentenceChars);
						}

					}
				}
				System.out.println("contained anchor "+containedAnchors.size()+" "+mapAnchorsTextAnchorID.size());
				String date = mapDocIDDate.get(docID);
				if (date!=null) {
					if (mapDateCategory.containsKey(date)) {
						List<Category> gt = new ArrayList<>(mapDateCategory.get(date));
						datasetSentences.put(build.toString(), gt);
						datasetEntities.put(entities, gt);
					}
					else {
						System.out.println(docID);
					}
					if (entities.size()>0) {
						numberOfDocsWithEntitiy++;
					}
				}
			}
			System.out.println(numberOfDocsWithEntitiy);
			System.out.println(datasetEntities.size());
			System.out.println(datasetSentences.size());
			System.out.println(count);

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return datasetSentences;
	}
	public static List<String> generateSentences(String content) {
		final List<String> sentenceList = new LinkedList<String>();
		final LexedTokenFactory<CoreLabel> tokenFactory = new CoreLabelTokenFactory();
		final List<CoreLabel> tokens = new ArrayList<CoreLabel>();
		final PTBTokenizer<CoreLabel> tokenizer = new PTBTokenizer<CoreLabel>(new StringReader(content.trim()),
				tokenFactory, "untokenizable=noneDelete");
		while (tokenizer.hasNext()) {
			tokens.add(tokenizer.next());
		}
		final List<List<CoreLabel>> sentences = new WordToSentenceProcessor<CoreLabel>().process(tokens);
		int end =0;
		int start = 0;

		for (List<CoreLabel> sentence : sentences) {
			end = sentence.get(sentence.size() - 1).endPosition();
			sentenceList.add(content.substring(start, end).trim());
			start = end;
		}
		return sentenceList;
	}
	public static Map<String, List<Category>> getMapDataCategory() {
		Map<String, List<Category>> result = new HashMap<>();
		try {
			List<String> lines = FileUtils.readLines(new File(DATASET_TEST_YOVISTO_DATE_TITLE), "utf-8");
			StringBuilder build = new StringBuilder();
			for(String line: lines) {
				if (line.equals("\t")&&build.toString().length()>1) {
					String[] split = build.toString().trim().split("#####");
					//System.out.println(split[2].split(" ")[0]+" "+split[3]); 
					String title = split[1];
					String date = split[2].split(" ")[0];
					List<Category> list = new ArrayList<>();
					for (int i = 3; i < split.length; i++) {
						String category = split[i];
						if (category.equals("explorer")) {
							category="Exploration";
						}
						else if (category.equals("medical science")) {
							category="Medicine";
						}
						else if (category.equals("war/crime")) {
							category="War";
						}
						else if (category.equals("space")) {
							category="Outer space";
						}
						else if (category.equals("art")) {
							category="Arts";
						}
						else if (category.equals("women in science")) {
							category="Women scientists";
						}
						else if (category.equals("inventor")) {
							category="Inventions";
						}
						else if (category.equals("archeology")) {
							category="Archaeology";
						}
						else if (category.equals("transportation")) {
							category="Transport";
						}
						else if (category.equals("computer games")) {
							category="Electronic games";
						}

						Category c = wikipedia.getCategoryByTitle(StringUtils.capitalize(category));
						if (c!=null) {
							list.add(c);	
						}
						if (c==null&&!category.equals("allgemein")) {
							System.out.println(category+" "+line);
						}
					}
					result.put(date, list);
					build = new StringBuilder();
				}
				else
				{
					build.append(line);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Size of result "+result.size());
		Set<Category> set = new HashSet<>();

		for(Entry <String, List<Category>> e: result.entrySet()) {
			for(Category c : e.getValue()) {
				set.add(c);
			}
		}
		for(Category c :set) {
			//System.out.println(c);
		}

		return result;
	}
	public static String getUrl(String url) {
		return URLUTF8Encoder.decodeJavaNative(url);
	}
}
