package edu.kit.aifb.gwifi.util;

import java.util.Map;

public interface TextCategorizer {
	
	public Map<String, Double> getCategoryWithProbability(String text);

}
