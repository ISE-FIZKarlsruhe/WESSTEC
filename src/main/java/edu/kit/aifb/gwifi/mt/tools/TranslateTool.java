package edu.kit.aifb.gwifi.mt.tools;

import java.util.List;
import java.util.Set;

public interface TranslateTool {
	
	public Set<String> getBestTranslate(String source);
	public double getTranslateProbability(String source, String target);

}
