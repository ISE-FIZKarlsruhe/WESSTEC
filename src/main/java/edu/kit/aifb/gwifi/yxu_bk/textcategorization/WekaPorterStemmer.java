package edu.kit.aifb.gwifi.yxu_bk.textcategorization;

import weka.core.RevisionUtils;
import weka.core.stemmers.Stemmer;
import edu.kit.aifb.gwifi.util.text.PorterStemmer;

public class WekaPorterStemmer extends PorterStemmer implements Stemmer {

	private static final long serialVersionUID = 1L;

	@Override
	public String getRevision() {
	    return RevisionUtils.extract("$Revision: 5836 $");
	}

	@Override
	public String stem(String word) {
		return processText(word);
	}

}
