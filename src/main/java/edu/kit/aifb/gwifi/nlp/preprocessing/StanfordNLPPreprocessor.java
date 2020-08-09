package edu.kit.aifb.gwifi.nlp.preprocessing;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import edu.kit.aifb.gwifi.util.Position;
import edu.kit.aifb.gwifi.util.nlp.Language;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.util.Triple;

// support Segmentation for Chinese, NER and POSTagging for English, German and Chinese
public class StanfordNLPPreprocessor implements NLPPreprocessor {

	private CRFClassifier<CoreLabel> classifier;
	private MaxentTagger tagger;
	private CRFClassifier<CoreLabel> segmenter;

	private Language language;

	public StanfordNLPPreprocessor(String configFile, String lang) {
		this(configFile, Language.getLanguage(lang));
	}

	public StanfordNLPPreprocessor(String configFile, Language lang) {
		Properties properties = new Properties();
		try {
			FileInputStream inputFile = new FileInputStream(configFile);
			properties.load(inputFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.language = lang;
		if (language.equals(Language.EN)) {
			classifier = CRFClassifier.getClassifierNoExceptions(properties.getProperty("englishSerializedClassifier"));
			tagger = new MaxentTagger(properties.getProperty("englishTag"));
		} else if (language.equals(Language.DE)) {
			classifier = CRFClassifier.getClassifierNoExceptions(properties.getProperty("germanSerializedClassifier"));
			tagger = new MaxentTagger(properties.getProperty("germanTag"));
		} else if (language.equals(Language.ES)) {
//			classifier = CRFClassifier.getClassifierNoExceptions(properties.getProperty("spanishSerializedClassifier"));
			tagger = new MaxentTagger(properties.getProperty("spanishTag"));
		} else if (language.equals(Language.ZH)) {
			Properties props = new Properties();
			props.setProperty("sighanCorporaDict", properties.getProperty("sighanCorporaDict"));
			props.setProperty("serDictionary", properties.getProperty("chineseDictionary"));
			props.setProperty("inputEncoding", "UTF-8");
			props.setProperty("sighanPostProcessing", "true");
			segmenter = new CRFClassifier<CoreLabel>(props);
			segmenter.loadClassifierNoExceptions(properties.getProperty("segmenterChineseClassifier"), props);

			classifier = CRFClassifier.getClassifierNoExceptions(properties.getProperty("chineseSerializedClassifier"));
			tagger = new MaxentTagger(properties.getProperty("chineseTag"));
		}
	}

	public LinkedHashMap<String, List<Position>> NEREntityAndPositions(String input) {
		LinkedHashMap<String, List<Position>> results = new LinkedHashMap<String, List<Position>>();
		
		List<Triple<String, Integer, Integer>> triples = classifier.classifyToCharacterOffsets(input);
		for (Triple<String, Integer, Integer> triple : triples) {
			Position position = new Position(triple.second(), triple.third());
			String entity = input.substring(position.getStart(), position.getEnd());
			List<Position> positions = results.get(entity);
			if (positions == null) {
				positions = new ArrayList<Position>();
				results.put(entity, positions);
			}
			positions.add(position);
		}

		return results;
	}

	public LinkedHashMap<Position, String> NERPositionAndType(String input) {
		LinkedHashMap<Position, String> results = new LinkedHashMap<Position, String>();

		List<Triple<String, Integer, Integer>> triples = classifier.classifyToCharacterOffsets(input);
		for (Triple<String, Integer, Integer> triple : triples) {
			String strType = triple.first();
			
			Position position = new Position(triple.second(), triple.third());
			String name = input.substring(position.getStart(), position.getEnd());
			
			results.put(position, strType);
		}

		return results;
	}

	public List<Position> NERPosition(String input) {
		List<Position> results = new ArrayList<Position>();

		List<Triple<String, Integer, Integer>> triples = classifier.classifyToCharacterOffsets(input);
		for (Triple<String, Integer, Integer> triple : triples) {
			Position position = new Position(triple.second(), triple.third());
			results.add(position);
		}

		return results;
	}

	public String POSTagging(String input) {
		List<List<HasWord>> sentences = MaxentTagger.tokenizeText(new StringReader(input));
		StringBuilder result = new StringBuilder();

		for (List<HasWord> sentence : sentences) {
			List<TaggedWord> tSentence = tagger.tagSentence(sentence);
			result.append(Sentence.listToString(tSentence, false));
		}
		return result.toString();
	}

	public LinkedHashMap<Position, String> POSTaggingPositionAndTag(String input) {
		LinkedHashMap<Position, String> results = new LinkedHashMap<Position, String>();

		List<List<HasWord>> sentences = MaxentTagger.tokenizeText(new StringReader(input));
		for (List<HasWord> sentence : sentences) {
			List<TaggedWord> tSentence = tagger.tagSentence(sentence);
			for (TaggedWord tWord : tSentence) {
				String tag = tWord.tag();
				int start = tWord.beginPosition();
				int end = tWord.endPosition();
				results.put(new Position(start, end), tag);
			}
		}

		return results;
	}

	public String segmentation(String input) {
		if (language.equals(Language.ZH)) {
			input = segmenter.classifyToString(input);
		}

		return input;
	}

	// now only used for Chinese
	public List<Position> segmentationPosition(String input) {
		if (!language.equals(Language.ZH)) {
			return null;
		}

		List<Position> results = new ArrayList<Position>();
		List<String> segments = segmenter.segmentString(input);

		int currentIndex = 0;
		for (String segment : segments) {
			int start = input.indexOf(segment, currentIndex);
			int end = start + segment.length();
			Position position = new Position(start, end);
			results.add(position);
			currentIndex = end;
		}

		return results;
	}

	public Language getLangauge() {
		return language;
	}

	public static void main(String args[]) {

		StanfordNLPPreprocessor pre = new StanfordNLPPreprocessor("configs/NLPConfig.properties", Language.ZH);
		Scanner scanner = new Scanner(System.in);

		while (true) {
			System.out.println("Please input the source text:");
			String source = scanner.nextLine();

//			source = "中新网新加坡6月1日电 (记者 夏宇华)中国人民解放军副总参谋长王冠中中将1日上午在新加坡香格里拉对话会上围绕“大国在保持亚洲地区和平中的作用”主题作大会发言。他在发言过程中脱离讲稿，对日本首相安倍晋三和美国国防部长哈格尔本次会议期间对中国进行的攻击作出反击，称上述二人的讲话是对中国的一种挑衅。";
			// source = "诺阿是自1988年的乔丹以来首位获得最佳防守球员称号的公牛球员。";
			// source = "Alibaba Jack Ma the Great Wall of China";
			// source = "History of the China’s Republic";
//			source = "1998年:泰森3月5日起诉唐・金,要求索赔1 亿美元,控告对方欺骗鲸吞他数千万美元;泰森3月9日 起诉前经济人,控告那两人合伙串通唐・金欺骗他;";
			source = "中国公司在巴西承包的第一个大型建设项目开工，由中国石化集团公司承包的巴西卡塞内天然气管道项目10日在 维多利亚市举行开工仪式。这是中国公司在巴西承包的第 一个大型建设项目。 ";
			
			if (source.startsWith("exit")) {
				break;
			}

			if (pre.getLangauge().equals(Language.ZH)) {
				source = pre.segmentation(source);
				System.out.println("Segmentation: ");
				System.out.println(source);
				for (Position pos : pre.segmentationPosition(source)) {
					System.out.println(source.substring(pos.getStart(), pos.getEnd()) + " : " + pos);
				}
			}

			System.out.println("\nPart Of Speech Tagging: ");
			System.out.println(pre.POSTaggingPositionAndTag(source) + "\n");
			System.out.println(pre.POSTagging(source) + "\n");

			System.out.println("\nNamed Entity Recognition: ");
			System.out.println(pre.NEREntityAndPositions(source) + "\n");
			System.out.println(pre.NERPositionAndType(source));
			System.out.println("\n");
		}
	}

}
