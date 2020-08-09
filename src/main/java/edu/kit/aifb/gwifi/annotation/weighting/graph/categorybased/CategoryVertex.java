package edu.kit.aifb.gwifi.annotation.weighting.graph.categorybased;

import java.text.DecimalFormat;

public class CategoryVertex extends Vertex {

	private String category;

	public CategoryVertex(String category) {
		this.category = category;
	}

	public String getCategory() {
		return category;
	}

	public String toString() {
		return "[Category:" + category + "] : " + new DecimalFormat("#.##").format(weight);
	}

	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof CategoryVertex))
			return false;
		CategoryVertex cv = (CategoryVertex) obj;
		return category.equals(cv.getCategory());
	}

	public int hashCode() {
		return category.hashCode();
	}

}
