package edu.kit.aifb.gwifi.util;

import java.util.Map;
import java.util.Set;

public interface CategoryAssociation {
	
	public Set<String> getCategories(String topicTitle);
	
	public Map<String, Double> getCategoriesWithWeights(String topicTitle);

}
