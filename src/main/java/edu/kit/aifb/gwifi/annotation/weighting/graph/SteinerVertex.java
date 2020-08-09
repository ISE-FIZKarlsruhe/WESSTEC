package edu.kit.aifb.gwifi.annotation.weighting.graph;

import java.text.DecimalFormat;

import edu.kit.aifb.gwifi.model.Article;

public class SteinerVertex extends Vertex {

	private Article article;

	public SteinerVertex(Article article) {
		this.article = article;
	}

	public Article getArticle() {
		return article;
	}

	public String toString() {
		return "[Article:" + article.getTitle() + "] : "
				+ new DecimalFormat("#.##").format(weight);
	}

	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof SteinerVertex))
			return false;
		SteinerVertex sv = (SteinerVertex) obj;
		return article.equals(sv.getArticle());
	}

	public int hashCode() {
		return article.hashCode();
	}

}
