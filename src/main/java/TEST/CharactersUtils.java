package TEST;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;

public class CharactersUtils {
	public static final String[] CHARS = new String[] { "!", "@", "#", "$", "%", "\\^", "&", "\\?", "\\(", "\\)", "'",
			"\"", "’" };

	public static final String[] EvaluatioaniAndDatasetGenerationCHARS = new String[] {"\\."};

	public static final String[] EvaluatioaniAndDatasetGenerationCHARSReplaceWithSpace = new String[] {"\\(", "\\)","\\[", "\\]", "-" ,">", "<","!", "@", "#", "$", "%", "\\^",
			"&", "\\?", "\\(", "\\)", "'","_", "\"", "’", "\\*", ";", ":", "`", ",", "\\|", "\\+", "/"};

	public static final List<String> CHARACTER = Arrays.asList("(", ")","[", "]", "-" ,">", "<","!", "@", "#", "$", "%", "^",
			"&", "?", "(", ")", "'","_", "\"", "’", "*", ";", ":", "`", ",", "|", "+", "/", "...", "\\",".","''","``","--", "'s","="," ");

	public static final String[] STOP_WORDS = { "a", "as", "able", "about", "above", "according", "accordingly",
			"across", "actually", "after", "afterwards", "again", "against", "aint", "all", "allow", "allows", "almost",
			"alone", "along", "already", "also", "although", "always", "am", "among", "amongst", "an", "and", "another",
			"any", "anybody", "anyhow", "anyone", "anything", "anyway", "anyways", "anywhere", "apart", "appear",
			"appreciate", "appropriate", "are", "arent", "around", "as", "aside", "ask", "asking", "associated", "at",
			"available", "away", "awfully", "be", "became", "because", "become", "becomes", "becoming", "been",
			"before", "beforehand", "behind", "being", "believe", "below", "beside", "besides", "best", "better",
			"between", "beyond", "both", "brief", "but", "by", "cmon", "cs", "came", "can", "cant", "cannot", "cant",
			"cause", "causes", "certain", "certainly", "changes", "clearly", "co", "com", "come", "comes", "concerning",
			"consequently", "consider", "considering", "contain", "containing", "contains", "corresponding", "could",
			"couldnt", "course", "currently", "definitely", "described", "despite", "did", "didnt", "different", "do",
			"does", "doesnt", "doing", "dont", "done", "down", "downwards", "during", "each", "edu", "eg", "eight",
			"either", "else", "elsewhere", "enough", "entirely", "especially", "et", "etc", "even", "ever", "every",
			"everybody", "everyone", "everything", "everywhere", "ex", "exactly", "example", "except", "far", "few",
			"ff", "fifth", "first", "five", "followed", "following", "follows", "for", "former", "formerly", "forth",
			"four", "from", "further", "furthermore", "get", "gets", "getting", "given", "gives", "go", "goes", "going",
			"gone", "got", "gotten", "greetings", "had", "hadnt", "happens", "hardly", "has", "hasnt", "have", "havent",
			"having", "he", "hes", "hello", "help", "hence", "her", "here", "heres", "hereafter", "hereby", "herein",
			"hereupon", "hers", "herself", "hi", "him", "himself", "his", "hither", "hopefully", "how", "howbeit",
			"however", "i", "id", "ill", "im", "ive", "ie", "if", "ignored", "immediate", "in", "inasmuch", "inc",
			"indeed", "indicate", "indicated", "indicates", "inner", "insofar", "instead", "into", "inward", "is",
			"isnt", "it", "itd", "itll", "its", "its", "itself", "just", "keep", "keeps", "kept", "know", "knows",
			"known", "last", "lately", "later", "latter", "latterly", "least", "less", "lest", "let", "lets", "like",
			"liked", "likely", "little", "look", "looking", "looks", "ltd", "mainly", "many", "may", "maybe", "me",
			"mean", "meanwhile", "merely", "might", "more", "moreover", "most", "mostly", "much", "must", "my",
			"myself", "name", "namely", "nd", "near", "nearly", "necessary", "need", "needs", "neither", "never",
			"nevertheless", "new", "next", "nine", "no", "nobody", "non", "none", "noone", "nor", "normally", "not",
			"nothing", "novel", "now", "nowhere", "obviously", "of", "off", "often", "oh", "ok", "okay", "old", "on",
			"once", "one", "ones", "only", "onto", "or", "other", "others", "otherwise", "ought", "our", "ours",
			"ourselves", "out", "outside", "over", "overall", "own", "particular", "particularly", "per", "perhaps",
			"placed", "please", "plus", "possible", "presumably", "probably", "provides", "que", "quite", "qv",
			"rather", "rd", "re", "really", "reasonably", "regarding", "regardless", "regards", "relatively",
			"respectively", "right", "said", "same", "saw", "say", "saying", "says", "second", "secondly", "see",
			"seeing", "seem", "seemed", "seeming", "seems", "seen", "self", "selves", "sensible", "sent", "serious",
			"seriously", "seven", "several", "shall", "she", "should", "shouldnt", "since", "six", "so", "some",
			"somebody", "somehow", "someone", "something", "sometime", "sometimes", "somewhat", "somewhere", "soon",
			"sorry", "specified", "specify", "specifying", "still", "sub", "such", "sup", "sure", "ts", "take", "taken",
			"tell", "tends", "th", "than", "thank", "thanks", "thanx", "that", "thats", "thats", "the", "their",
			"theirs", "them", "themselves", "then", "thence", "there", "theres", "thereafter", "thereby", "therefore",
			"therein", "theres", "thereupon", "these", "they", "theyd", "theyll", "theyre", "theyve", "think", "third",
			"this", "thorough", "thoroughly", "those", "though", "three", "through", "throughout", "thru", "thus", "to",
			"together", "too", "took", "toward", "towards", "tried", "tries", "truly", "try", "trying", "twice", "two",
			"un", "under", "unfortunately", "unless", "unlikely", "until", "unto", "up", "upon", "us", "use", "used",
			"useful", "uses", "using", "usually", "value", "various", "very", "via", "viz", "vs", "want", "wants",
			"was", "wasnt", "way", "we", "wed", "well", "were", "weve", "welcome", "well", "went", "were", "werent",
			"what", "whats", "whatever", "when", "whence", "whenever", "where", "wheres", "whereafter", "whereas",
			"whereby", "wherein", "whereupon", "wherever", "whether", "which", "while", "whither", "who", "whos",
			"whoever", "whole", "whom", "whose", "why", "will", "willing", "wish", "with", "within", "without", "wont",
			"wonder", "would", "would", "wouldnt", "yes", "yet", "you", "youd", "youll", "youre", "youve", "your",
			"yours", "yourself", "yourselves", "zero" };

	public final static List<String> STOP_WORDS_LIST = Arrays.asList(STOP_WORDS);

	public static String convertUmlaut(String text) {
		final String[][] UMLAUT_REPLACEMENTS = { { new String("Ä"), "Ae" }, { new String("Ü"), "Ue" },
				{ new String("Ö"), "Oe" }, { new String("ä"), "ae" }, { new String("ü"), "ue" },
				{ new String("ö"), "oe" }, { new String("ß"), "ss" } };
		String result = text;
		for (int i = 0; i < UMLAUT_REPLACEMENTS.length; i++) {
			result = result.replace(UMLAUT_REPLACEMENTS[i][0], UMLAUT_REPLACEMENTS[i][1]);
		}
		return result;
	}

	/**
	 * this function will be used in dictionary generation
	 * 
	 * @param anchorText
	 * @return
	 */
	public static String removeSpeicalCharacters(String anchorText) {
		String result = new String(anchorText);
		for (String character : CharactersUtils.CHARS) {
			result = result.replaceAll(character, " ");
		}
		return result;
	}

	/**
	 * This function only should be used for dataset generation and evaluation
	 * This function exclude comma and period as special character as they
	 * carry some semantics in a sentence
	 * 
	 * @param anchorText
	 * @return
	 */
	public static String removeSpeicalCharactersForEvaluationAdnDatasetGeneration(String anchorText) {
		String result = new String(anchorText);
		for (String character : CharactersUtils.EvaluatioaniAndDatasetGenerationCHARS) {
			result = result.replaceAll(character, " ");
		}
		return result;
	}

	public static String removeDotsIfTheSizeOfTextIs2(String anchorText) {
		String result = new String(anchorText);
		if (anchorText.length() <= 2) {
			result = result.replaceAll(".", " ");
		}
		return result;
	}

	public static String removeNoneAlphabeticSingleChar(String anchorText) {
		if (anchorText.length() == 1) {
			if (!Character.isLetter(anchorText.charAt(0))) {
				return "";
			}
		}
		return anchorText;
	}

	public static String removeAlphabeticSingleChar(String anchorText) {
		if (anchorText.length() == 1) {
			return "";
		}
		return anchorText;
	}


	public static String ignoreAnchorTextWithSpeicalAlphabeticCharacter(String text) {
		if (Charset.forName("US-ASCII").newEncoder().canEncode(text)) {
			return text;
		} else {
			return "";
		}

	}

	public static String removeGenitiveS(final String anchorText) {
		String result = new String(anchorText);
		result = result.replaceAll("'s", " ");
		return result;
	}

	public static String removeStopWords(String sentence) {
		final String[] words = sentence.split(" ");
		final List<String> wordsList = new ArrayList<String>();

		for (final String word : words) {
			if (!STOP_WORDS_LIST.contains(word)) {
				wordsList.add(word);
			}
		}

		final StringBuilder result = new StringBuilder();
		for (String str : wordsList) {
			result.append(str).append(" ");
		}

		return result.toString().trim();
	}

	/**
	 * This function should be used when we want to normalize train data(wikipedia)
	 * for GroundTruth or test data use normalizeTaggedSentence in {@link ConvertDatasetToFeatureSetForCRFSuite}
	 * @param text
	 * @return
	 */
	public static String normalizeTrainSentence(String text) {
		String result = new String(text);
		result = deAccent(result);
		result = removeGenitiveS(result);
		result = removeSpeicalCharactersForEvaluationAdnDatasetGeneration(result);
		result = removeSpeicalCharactersForEvaluationAdnDatasetGenerationReplaceWithSpace(result);
		result = removeAllStrangeCharacters(result);
		//result = convert2Lemma(result);
		return result;
	}

	public static String removeAllStrangeCharacters(String result) {
		return result.replaceAll("[^\\w\\s,]", " ");
	}

	private static String deAccent(String str) {
		return StringUtils.stripAccents(str);
	}

	private static String removeSpeicalCharactersForEvaluationAdnDatasetGenerationReplaceWithSpace(String anchorText) {
		String result = new String(anchorText);
		for (String character : CharactersUtils.EvaluatioaniAndDatasetGenerationCHARSReplaceWithSpace) {
			result = result.replaceAll(character, " ");
		}
		return result;
	}
	public static boolean isStringNumeric(String string) {
		boolean numeric = true;
		try {
			Double num = Double.parseDouble(string);
		} catch (NumberFormatException e) {
			numeric = false;
		}
		if(numeric)
			return true;
		else
			return false;
	}
	public static String getSaltString() {
		//final String SALTCHARS = "ABCDEFGHIJKLMNOPH".toLowerCase();
		final String SALTCHARS = "123456789".toLowerCase();
		final StringBuilder salt = new StringBuilder();
		final Random rnd = new Random();
		while (salt.length() < 18) { // length of the random string.
			int index = (int) (rnd.nextFloat() * SALTCHARS.length());
			salt.append(SALTCHARS.charAt(index));
		}
		final String saltStr = salt.toString();
		return saltStr;
	}
}