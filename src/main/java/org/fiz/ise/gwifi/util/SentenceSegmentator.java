package org.fiz.ise.gwifi.util;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import TEST.CharactersUtils;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.LexedTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.WordToSentenceProcessor;

public class SentenceSegmentator {

	public static void main(String[] args) {
		String text = "I need to get a list of all the files in a directory, including files in all the sub-directories. What is the standard way to accomplish directory iteration with Java?";
		wordCount("hello & & & hello hello. hello \",  \"bye\", \"ciao  \", \"bye\", \"ciao");
		//		final ArrayList<String> sentenceList = segment2Sentence(text);
		//
		//		sentenceList.forEach(p -> System.err.println(p));
	}

	public static ArrayList<String> segment2Sentence(String text) {
		final List<CoreLabel> tokens = new ArrayList<CoreLabel>();

		final LexedTokenFactory<CoreLabel> tokenFactory = new CoreLabelTokenFactory();

		final PTBTokenizer<CoreLabel> tokenizer = new PTBTokenizer<CoreLabel>(new StringReader(text), tokenFactory,
				"untokenizable=noneDelete");

		while (tokenizer.hasNext()) {
			tokens.add(tokenizer.next());
		}

		final List<List<CoreLabel>> sentences = new WordToSentenceProcessor<CoreLabel>().process(tokens);
		int end;
		int start = 0;
		final ArrayList<String> sentenceList = new ArrayList<String>();
		for (List<CoreLabel> sentence : sentences) {
			end = sentence.get(sentence.size() - 1).endPosition();
			sentenceList.add(text.substring(start, end).trim());
			start = end;
		}
		return sentenceList;
	}

	public static int findNumberOfSentences(String text) {
		final List<CoreLabel> tokens = new ArrayList<CoreLabel>();

		final LexedTokenFactory<CoreLabel> tokenFactory = new CoreLabelTokenFactory();

		final PTBTokenizer<CoreLabel> tokenizer = new PTBTokenizer<CoreLabel>(new StringReader(text), tokenFactory,
				"untokenizable=noneDelete");

		while (tokenizer.hasNext()) {
			tokens.add(tokenizer.next());
		}

		final List<List<CoreLabel>> sentences = new WordToSentenceProcessor<CoreLabel>().process(tokens);
		int end;
		int start = 0;
		final ArrayList<String> sentenceList = new ArrayList<String>();
		for (List<CoreLabel> sentence : sentences) {
			end = sentence.get(sentence.size() - 1).endPosition();
			sentenceList.add(text.substring(start, end).trim());
			start = end;
		}
		return sentenceList.size();
	}

	public static int wordCount(String text) {
		final List<CoreLabel> tokens = new ArrayList<CoreLabel>();

		final LexedTokenFactory<CoreLabel> tokenFactory = new CoreLabelTokenFactory();

		final PTBTokenizer<CoreLabel> tokenizer = new PTBTokenizer<CoreLabel>(new StringReader(text), tokenFactory,
				"untokenizable=noneDelete");

		while (tokenizer.hasNext()) {
			CoreLabel t = tokenizer.next();
			if (!CharactersUtils.CHARACTER.contains(t.toString())) {
				tokens.add(t);
			}
		}
		return tokens.size();
	}

	public static int wordCount(List<String> ListOfText) {
		final List<CoreLabel> tokens = new ArrayList<CoreLabel>();

		final LexedTokenFactory<CoreLabel> tokenFactory = new CoreLabelTokenFactory();

		for (String text : ListOfText) {
			final PTBTokenizer<CoreLabel> tokenizer = new PTBTokenizer<CoreLabel>(new StringReader(text), tokenFactory,
					"untokenizable=noneDelete");

			while (tokenizer.hasNext()) {
				CoreLabel t = tokenizer.next();
				if (!CharactersUtils.CHARACTER.contains(t.toString())) {
					tokens.add(t);
				}
			}

		}
		return tokens.size();
	}
	public static List<String> tokenizeSentence(String sentence) {
		final List<String> tokens = new ArrayList<String>();
		final LexedTokenFactory<CoreLabel> tokenFactory = new CoreLabelTokenFactory();

		final PTBTokenizer<CoreLabel> tokenizer = new PTBTokenizer<CoreLabel>(new StringReader(sentence), tokenFactory,
				"untokenizable=noneDelete");
		while (tokenizer.hasNext()) {
			tokens.add(tokenizer.next().toString());
		}
		return tokens;
	}

}
