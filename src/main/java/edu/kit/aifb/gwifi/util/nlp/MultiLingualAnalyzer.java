package edu.kit.aifb.gwifi.util.nlp;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.tartarus.snowball.SnowballProgram;
import org.tartarus.snowball.ext.CatalanStemmer;
import org.tartarus.snowball.ext.DanishStemmer;
import org.tartarus.snowball.ext.DutchStemmer;
import org.tartarus.snowball.ext.EnglishStemmer;
import org.tartarus.snowball.ext.FinnishStemmer;
import org.tartarus.snowball.ext.FrenchStemmer;
import org.tartarus.snowball.ext.GermanStemmer;
import org.tartarus.snowball.ext.ItalianStemmer;
import org.tartarus.snowball.ext.NorwegianStemmer;
import org.tartarus.snowball.ext.PortugueseStemmer;
import org.tartarus.snowball.ext.RussianStemmer;
import org.tartarus.snowball.ext.SpanishStemmer;
import org.tartarus.snowball.ext.SwedishStemmer;

public class MultiLingualAnalyzer implements ITokenAnalyzer {
	static Log logger = LogFactory.getLog(MultiLingualAnalyzer.class);

	static Map<Language, Class<? extends SnowballProgram>> STEMMER_CLASSES = new HashMap<Language, Class<? extends SnowballProgram>>();
	static {
		STEMMER_CLASSES.put(Language.EN, EnglishStemmer.class);
		STEMMER_CLASSES.put(Language.DE, GermanStemmer.class);
		STEMMER_CLASSES.put(Language.ES, SpanishStemmer.class);
		STEMMER_CLASSES.put(Language.CA, CatalanStemmer.class);
		
		STEMMER_CLASSES.put(Language.FR, FrenchStemmer.class);
		STEMMER_CLASSES.put(Language.IT, ItalianStemmer.class);
		STEMMER_CLASSES.put(Language.PT, PortugueseStemmer.class);
		STEMMER_CLASSES.put(Language.RU, RussianStemmer.class);
		
		STEMMER_CLASSES.put(Language.DK, DanishStemmer.class);
		STEMMER_CLASSES.put(Language.NL, DutchStemmer.class);
		STEMMER_CLASSES.put(Language.FI, FinnishStemmer.class);
		STEMMER_CLASSES.put(Language.NO, NorwegianStemmer.class);
		STEMMER_CLASSES.put(Language.SE, SwedishStemmer.class);
	}

	private Map<Language, Set<String>> stopwordSets;

	private boolean stemming = true;
	private int minTokenLength = 3;

	public MultiLingualAnalyzer() {
		stopwordSets = new HashMap<Language, Set<String>>();
	}

	public void setStemming(boolean stemming) {
		this.stemming = stemming;
		logger.info("Setting stemming to " + stemming);
	}

	public void setMinTokenLength(int minTokenLength) {
		this.minTokenLength = minTokenLength;
		logger.info("Setting minimal token length " + minTokenLength);
	}

	public void setStopwordFile(String stopWordFile) {
		String[] split = stopWordFile.split(":", 2);
		Language l = Language.getLanguage(split[0]);
		if (l == null) {
			logger.error("Could not read stop word file " + stopWordFile + ": Language is not supported!");
		} else {
			try {
				logger.info("Reading stop word file: " + split[1]);
				stopwordSets.put(l, importStopwords(new BufferedReader(new FileReader(split[1]))));
			} catch (Exception e) {
				logger.error(e);
			}
		}
	}

	public void setStopwordFiles(Collection<String> stopWordFiles) {
		for (String stopWordFile : stopWordFiles) {
			setStopwordFile(stopWordFile);
		}
	}

	public void setStopwordResource(String stopWordResource) {
		String[] split = stopWordResource.split(":", 2);
		Language l = Language.getLanguage(split[0]);
		if (l == null) {
			logger.error("Could not read stop word resource " + stopWordResource + ": Language is not supported!");
		} else {
			try {
				logger.info("Reading stop word resource: " + split[1]);
				InputStream in = MultiLingualAnalyzer.class.getResourceAsStream(split[1]);

				stopwordSets.put(l, importStopwords(new BufferedReader(new InputStreamReader(in))));
			} catch (Exception e) {
				logger.error(e);
			}
		}
	}

	public void setStopwordResources(Collection<String> stopWordResources) {
		for (String stopWordResource : stopWordResources) {
			setStopwordResource(stopWordResource);
		}
	}

	private Set<String> importStopwords(BufferedReader fileReader) throws Exception {
		StringBuffer temp = new StringBuffer();

		String line = fileReader.readLine();
		while (line != null) {
			if (line.indexOf('|') >= 0)
				line = line.substring(0, line.indexOf('|'));

			if (line.length() > 0) {
				temp.append(line);
				temp.append(' ');
			}

			line = fileReader.readLine();
		}
		fileReader.close();

		Set<String> stopwordSet = new HashSet<String>();

		StringTokenizer st = new StringTokenizer(temp.toString());
		while (st.hasMoreTokens()) {
			String token = st.nextToken().toLowerCase();
			if (token != null && token.length() > 0)
				stopwordSet.add(token);
		}

		return stopwordSet;
	}

	public ITokenStream getAnalyzedTokenStream(ITokenStream ts) {
		return new MultiLingualTokenStream(ts);
	}

	private class MultiLingualTokenStream implements ITokenStream {

		private ITokenStream tokenStream;
		private String currentToken;
		private Language currentLanguage;

		private Map<Language, SnowballProgram> stemmers;

		protected MultiLingualTokenStream(ITokenStream ts) {
			tokenStream = ts;
			stemmers = new HashMap<Language, SnowballProgram>();
		}

		private SnowballProgram getStemmer(Language lang) {
			if (stemmers.containsKey(lang)) {
				return stemmers.get(lang);
			} else if (STEMMER_CLASSES.containsKey(lang)) {
				try {
					logger.debug("Initializing stemmer for language " + lang);
					SnowballProgram stemmer;
					stemmer = STEMMER_CLASSES.get(lang).newInstance();
					stemmers.put(lang, stemmer);
					return stemmer;
				} catch (Exception e) {
					logger.error(e);
				}
			}
			return null;
		}

		public Language getLanguage() {
			return currentLanguage;
		}

		public String getToken() {
			return currentToken;
		}

		public boolean next() {
			while (tokenStream.next()) {

				String token = tokenStream.getToken();
				Language lang = tokenStream.getLanguage();

				// delete all punctuation, spaces and numbers
				// token = token.replaceAll( "[=~\\p{Z}\\p{P}\\p{N}]", "" );

				token = token.toLowerCase();

				if (token.length() >= minTokenLength && token.length() <= 35) {

					if (!Pattern.matches("^\\p{L}+$", token)) {
						continue;
					}

					if (stopwordSets.containsKey(lang) && stopwordSets.get(lang).contains(token)) {
						continue;
					}

					if (stemming) {
						SnowballProgram stemmer = getStemmer(lang);
						if (stemmer != null) {
							stemmer.setCurrent(token);
							stemmer.stem();
							token = stemmer.getCurrent();
						}
					}

					if (token.length() > 0) {
						currentToken = token;
						currentLanguage = lang;
						return true;
					}
				}
			}
			return false;
		}

		public void reset() {
			tokenStream.reset();
		}

	}

}
