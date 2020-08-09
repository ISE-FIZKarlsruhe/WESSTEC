package edu.kit.aifb.gwifi.comparison.text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Set;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LetterTokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.miscellaneous.LengthFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.Version;

public class WikipediaZhAnalyzer implements Analyzer {

	/**
	 * An unmodifiable set containing some common English words that are not usually useful for searching.
	 */
	public final CharArraySet ENGLISH_STOP_WORDS_SET;

	public WikipediaZhAnalyzer() throws IOException {
		// read stop words
		InputStream is = WikipediaZhAnalyzer.class.getResourceAsStream("/config/stopwords_en.txt");
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		ArrayList<String> stopWords = new ArrayList<String>(500);

		String line;

		while ((line = br.readLine()) != null) {
			line = line.trim();
			if (!line.equals("")) {
				stopWords.add(line.trim());
			}
		}

		br.close();

		final CharArraySet stopSet = new CharArraySet(Version.LUCENE_48, stopWords.size(), false);
		stopSet.addAll(stopWords);

		ENGLISH_STOP_WORDS_SET = CharArraySet.unmodifiableSet(stopSet);

	}

	public TokenStream tokenStream(String fieldName, Reader reader) {

		Tokenizer tokenizer = new LetterTokenizer(Version.LUCENE_48, reader);

		TokenStream stream = new StandardFilter(Version.LUCENE_48, tokenizer);
		stream = new LengthFilter(Version.LUCENE_48, stream, 3, 100);
		stream = new LowerCaseFilter(Version.LUCENE_48, stream);
		// stream = new StopFilter(true, stream, StopAnalyzer.ENGLISH_STOP_WORDS_SET);
		stream = new StopFilter(Version.LUCENE_48, stream, ENGLISH_STOP_WORDS_SET);
		stream = new PorterStemFilter(stream);
		stream = new PorterStemFilter(stream);
		stream = new PorterStemFilter(stream);

		return stream;
	}
}