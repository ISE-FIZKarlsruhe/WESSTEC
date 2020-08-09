package edu.kit.aifb.gwifi.util.nlp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.hankcs.hanlp.HanLP;

public class LabelNormalizer {
	
	private Language _lang;
	
	public LabelNormalizer(String langLabel) {
		_lang = Language.getLanguage(langLabel);
	}
	
	public LabelNormalizer(Language lang) {
		_lang = lang;
	}
	
	public String normalize(String text) {
		text = clean(text);
		text = text.toLowerCase();
		if(_lang.equals(Language.ZH)) {
			text = normalizeChinese(text);
		}
		return text;
	}
	
	public static String clean(String text) {
		text = text.replaceAll("\\(.+\\)", "");
		text = text.replaceAll("\\s+", " ");
		return text;
	}
	
	// remove the non-ideographic characters, convert to simplified Chinese.
	protected static String normalizeChinese(String text) {
		StringBuffer sb = new StringBuffer();
		for (int j = 0; j < text.length(); j++) {
			char c = text.charAt(j);
			if(Character.isIdeographic(c) || Character.isAlphabetic(c)) {
				sb.append(c);
			} 
	    }
		return HanLP.convertToSimplifiedChinese(sb.toString().trim());
	}
	
	public List<String> getSegments(String title) {
		title = normalize(title);
		List<String> keywordsList = Arrays.asList(title.split("\\s"));
		List<String> labels = new ArrayList<String>();
		for (int i = 1; i < keywordsList.size() + 1; i++) {
			// TODO: need to be changed
			if(i != 1 && i != keywordsList.size())
				continue;
			
			for (int j = 0; j < keywordsList.size() - i + 1; j++) {
				String label = "";
				for (int k = j; k < j + i; k++) {
					String keyword = keywordsList.get(k).trim();
					if(!keyword.equals(""))
						label += keyword + " ";
				}
				label = label.trim();
				if(!label.equals(""))
					labels.add(label);
			}
		}
		return labels;
	}

	public static void main(String[] args) throws Exception {	
//		String s = "迈克尔·乔丹 (学者)";
		String s = "台湾IBM";
		LabelNormalizer ln = new LabelNormalizer(Language.ZH);
		System.out.println("normalized text: " + ln.normalize(s));
		System.out.println("segments: ");
		for(String label : ln.getSegments(s))
			System.out.println(label);
		
		s = "Michael I. Jordan";
		ln = new LabelNormalizer(Language.EN);
		System.out.println("normalized text: " + ln.normalize(s));
		System.out.println("segments: ");
		for(String label : ln.getSegments(s))
			System.out.println(label);
	}
}
