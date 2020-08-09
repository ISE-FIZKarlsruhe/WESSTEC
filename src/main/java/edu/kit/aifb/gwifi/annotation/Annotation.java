package edu.kit.aifb.gwifi.annotation;

import edu.kit.aifb.gwifi.util.nlp.Language;

public class Annotation implements Comparable<Annotation> {

	private int id;
	private String title;
	private String url;
	private String displayName;
	private Language lang;
	private double weight;
	private Mention mention;

	public Annotation(int id, String title) {
		this.id = id;
		this.title = title;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setURL(String url) {
		this.url = url;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public void setLanguage(Language lang) {
		this.lang = lang;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}

	public void setMention(Mention mention) {
		this.mention = mention;
	}

	public int getId() {
		return id;
	}

	public String getURL() {
		return url;
	}

	public String getDisplayName() {
		if (displayName == null)
			return title;
		else
			return displayName;
	}

	public Language getLanguage() {
		return lang;
	}

	public double getWeight() {
		return weight;
	}

	public Mention getMention() {
		return mention;
	}

	public String getTitle() {
		return title;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj instanceof Annotation) {
			Annotation anno = (Annotation) obj;
			if (compareTo(anno) == 0)
				return true;
			else
				return false;
		}
		return false;
	}

	public int hashCode() {
		int result = 17;
		result = 37 * result + title.hashCode();
		return result;
	}

	// ordering based on three aspects: 1) position of mention, 2) annotation confidence, 3) entity id
	public int compareTo(Annotation anno) {
		int c = mention.compareTo(anno.getMention());
		if (c != 0)
			return c;
		c = new Double(weight).compareTo(anno.getWeight());
		if (c != 0)
			return c;
		c = new Integer(id).compareTo(anno.getId());
		if (c != 0)
			return c;

		return 0;
	}

	public String toString() {
		return mention + ":" + title;
	}

}
