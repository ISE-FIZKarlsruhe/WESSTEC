package org.fiz.ise.gwifi.util;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.LexedTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;

public class StringUtil {
	public static boolean isNumeric(final String str) {
        return NumberUtils.isDigits(str);
    }
	public static String removePunctuation(String str)
	{
		return str.replaceAll("[^\\w\\s]", " ").replaceAll("[\\d]", " ");
	}
	public static List<String> tokinizeString(String shortText) {
		List<String> tokensStr = new ArrayList<String>();
		final LexedTokenFactory<CoreLabel> tokenFactory = new CoreLabelTokenFactory();

		final PTBTokenizer<CoreLabel> tokenizer = new PTBTokenizer<CoreLabel>(new StringReader(removePunctuation(shortText)), tokenFactory,
				"untokenizable=noneDelete");
		while (tokenizer.hasNext()) {
			tokensStr.add(tokenizer.next().toString());
		}
		return tokensStr;
	}
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
     * The Levenshtein distance, or edit distance, between two words is the
     * minimum number of single-character edits (insertions, deletions or
     * substitutions) required to change one word into the other.
     *
     * http://en.wikipedia.org/wiki/Levenshtein_distance
     *
     * It is always at least the difference of the sizes of the two strings.
     * It is at most the length of the longer string.
     * It is zero if and only if the strings are equal.
     * If the strings are the same size, the Hamming distance is an upper bound
     * on the Levenshtein distance.
     * The Levenshtein distance verifies the triangle inequality (the distance
     * between two strings is no greater than the sum Levenshtein distances from
     * a third string).
     *
     * Implementation uses dynamic programming (Wagner–Fischer algorithm), with
     * only 2 rows of data. The space requirement is thus O(m) and the algorithm
     * runs in O(mn).
     *
     * @param s1 The first string to compare.
     * @param s2 The second string to compare.
     * @return The computed Levenshtein distance.
     * @throws NullPointerException if s1 or s2 is null.
     */
	
	
    public final static double levenshteinDistance(final String s1, final String s2) {
        if (s1 == null) {
            throw new NullPointerException("s1 must not be null");
        }

        if (s2 == null) {
            throw new NullPointerException("s2 must not be null");
        }

        if (s1.equals(s2)) {
            return 0;
        }

        if (s1.length() == 0) {
            return s2.length();
        }

        if (s2.length() == 0) {
            return s1.length();
        }

        // create two work vectors of integer distances
        int[] v0 = new int[s2.length() + 1];
        int[] v1 = new int[s2.length() + 1];
        int[] vtemp;

        // initialize v0 (the previous row of distances)
        // this row is A[0][i]: edit distance for an empty s
        // the distance is just the number of characters to delete from t
        for (int i = 0; i < v0.length; i++) {
            v0[i] = i;
        }

        for (int i = 0; i < s1.length(); i++) {
            // calculate v1 (current row distances) from the previous row v0
            // first element of v1 is A[i+1][0]
            //   edit distance is delete (i+1) chars from s to match empty t
            v1[0] = i + 1;

            // use formula to fill in the rest of the row
            for (int j = 0; j < s2.length(); j++) {
                int cost = 1;
                if (s1.charAt(i) == s2.charAt(j)) {
                    cost = 0;
                }
                v1[j + 1] = Math.min(
                        v1[j] + 1,              // Cost of insertion
                        Math.min(
                                v0[j + 1] + 1,  // Cost of remove
                                v0[j] + cost)); // Cost of substitution
            }

            // copy v1 (current row) to v0 (previous row) for next iteration
            //System.arraycopy(v1, 0, v0, 0, v0.length);

            // Flip references to current and previous row
            vtemp = v0;
            v0 = v1;
            v1 = vtemp;

        }

        return v0[s2.length()];
    }
	public static String removeUmlaut(String str)
	{
//		String entity = str.replace("\\u0028","(").
//				replace("\\u0029",")").replace("\\u0027","'").replace("\\u00fc","ü").replace("\\u002c",",").
//				replace("\\u0163","ţ").replace("\\u00e1s","á").replace("\\u0159","ř").replace("\\u00e9","é").
//				replace("\\u00ed","í").replace("\\u00e1","á").replace("\\u2013","-").replace("\\u0107","ć").
//				replace("\\u002e",".").replace("\\u00f3","ó").replace("\\u002d","-").replace("\\u00e1","Ž").
//				replace("\\u0160","Š").replace("\\u0105","ą").replace("\\u00eb","ë").replace("\\u017d","Ž").
//				replace("\\u00e7","ç").replace("\\u00f8","ø").replace("\\u0161","š").replace("\\u0107","ć").
//				replace("\\u00f6","ö").replace("\\u010c","Č").replace("\\u00fd","ý").replace("\\u00d6","Ö").
//				replace("\\u00c0","À").replace("\\u0026","&").replace("\\u00df","ß").replace("\\u00ea","ê").
//				replace("\\u017","ž").replace("\\u011b","ě").replace("\\u00f6","ö").replace("\\u00e3","ã").
//				replace("\\u0103","ă").replace("\\u00c1","Á").replace("\\u002f","/").replace("\\u00e4","ä").
//				replace("\\u00c5","Å").replace("\\u0142","ł").replace("\\u0117","ė").replace("\\u00ff","ÿ").
//				replace("\\u00f1","ñ").replace("\\u015f","ş").replace("\\u015e","Ş").replace("\\u0131","ı").
//				replace("\\u0131k","Ç").replace("\\u0144","ń").replace("\\u0119","ę").replace("\\u00c9","É").
//				replace("\\u0111","đ").replace("\\u00e2","â").replace("\\u010d","č").replace("\\u015a","Ś").
//				replace("\\u0141","Ł").replace("\\u00e8","è").replace("\\u00c9","É").replace("\\u00e5","å").
//				replace("\\u014d","ō").replace("\\u00e6","æ").replace("\\u00d3","Ó").replace("\\u00da","Ú").
//				replace("\\u0151","ő").replace("\\u0148","ň").replace("\\u00fa","ú").replace("\\u00ee","î").
//				replace("\\u015b","ś").replace("\\u00c7","Ç").replace("\\u00f4","ô").replace("\\u013d","Ľ").
//				replace("\\u013e","ľ").replace("\\u011f","ğ").replace("\\u00e0","à").replace("\\u00dc","Ü").
//				replace("\\u0021","!").replace("_"," ");
		
//		String entity = str.replace("á","a").replace("ř","r").replace("é","e").
//				replace("í","i").replace("ć","c").replace("ó","o").replace("Ž","Z").replace("Š","S").replace("ą","a").replace("ë","e").replace("Ž","Z").
//				replace("š","s").replace("Č","C").replace("ý","y").replace("À","A").replace("ê","e").
//				replace("ž","z").replace("ě","e").replace("ã","a").
//				replace("ă","a").replace("Á","A").replace("Å","A").replace("ė","e").replace("ÿ","y").
//				replace("ñ","n").replace("ı","i").replace("ń","n").replace("ę","e").replace("É","E").
//				replace("đ","d").replace("â","a").replace("\\u010d","č").replace("Ś","S").
//				replace("è","e").replace("É","E").replace("å","a").
//				replace("ō","ö").replace("Ó","O").replace("Ú","U").
//				replace("ő","ö").replace("ň","n").replace("\\u00fa","ú").replace("î","i").
//				replace("ś","s").replace("ô","o").replace("Ľ","L").
//				replace("ľ","l").replace("à","a");
		
				return StringUtils.stripAccents(str);
	
	}
}
