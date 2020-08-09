package org.fiz.ise.gwifi.util;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.LexedTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.WordToSentenceProcessor;

public class YovistoDocument {

	/**
	 * This class models yovisto files
	 */
		private String docID;
		private String title;
		private  String date;
		private  String content;
		private  List<String> sentences;
		private Map<String,String> mapAnchorIDURI;
		private Map<String,String> mapAnchorTextAnchorID;

		public YovistoDocument(String id,String title, String content,HashMap<String, String> map) {
			this.docID=id;
			this.title = title;
			this.content=content;
			this.sentences= new LinkedList<>(generateSentences(content));
			mapAnchorIDURI= new HashMap<>(map);
//			assignAnchors()
		}

		
		private List<String> assignAnchors(String content,Map<String, String> anchorEntity,Map<String, String> anchorPosition) {
			final List<String> sentenceList = new LinkedList<String>();
			final Map<String,String> result = new HashMap<>();
			final List<String> anchorsList = new ArrayList<String>();
			
			for(Entry<String,String> e:mapAnchorIDURI.entrySet()) {
				anchorsList.add(e.getValue());
			}
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
					String resultSentence =content.substring(start, end).trim(); 
					for(String a: anchorsList ) {
						if(sentence.contains(a)) {
							result.put(resultSentence, a);
						}
					}
					sentenceList.add(resultSentence);
					start = end;
				}
			
			return sentenceList;
		}
		private List<String> generateSentences(String content) {
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

		public String getTitle() {
			return title;
		}

		public String getContent() {
			return content;
		}

		public List<String> getSentences() {
			return sentences;
		}

		@Override
		public String toString() {
			return title + ", " + content;
		}

//		@Override
//		public String toString() {
//			return "Document [location=" + location + ", title=" + title + ", content=" + content + ", sentences="
//					+ sentences + "]";
//		}

	

}
