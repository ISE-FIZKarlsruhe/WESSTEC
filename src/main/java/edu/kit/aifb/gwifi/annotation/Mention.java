package edu.kit.aifb.gwifi.annotation;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import edu.kit.aifb.gwifi.model.Label;
import edu.kit.aifb.gwifi.util.Position;

public class Mention implements Comparable<Mention> {
	public enum Type {
		NI, NO, GI, GO
	}

	private Type type;
	private double freqM;
	private double idfM;
	private double boostM;
	private String term;
	private Position position;

	private List<Label> relatedLabel;
	private List<String> relatedSubTerm;

	public Mention(Position position) {
		this.position = position;
	}

	public List<String> getRelatedSubTerm() {
		return relatedSubTerm;
	}

	public void setRelatedSubTerm(List<String> relatedSubTerm) {
		this.relatedSubTerm = relatedSubTerm;
	}

	public List<Label> getRelatedLabel() {
		return relatedLabel;
	}

	public void setRelatedLabel(List<Label> relateLabel) {
		this.relatedLabel = relateLabel;
	}

	public Type getType() {
		return type;
	}

	public double getFreqM() {
		return freqM;
	}

	public void setFreqM(double freqM) {
		this.freqM = freqM;
	}

	public double getIdfM() {
		return idfM;
	}

	public void setIdfM(double idfM) {
		this.idfM = idfM;
	}

	public double getBoostM() {
		return boostM;
	}

	public void setBoostM(double boostM) {
		this.boostM = boostM;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public Mention(String term) {
		this.term = term;
	}

	public Mention(Position position, String term) {
		this.position = position;
		this.term = term;
	}

	public Mention(Position position, String term, Type type) {
		this.position = position;
		this.term = term;
		this.type = type;
		this.relatedLabel = new ArrayList<Label>();
		this.relatedSubTerm = new ArrayList<String>();
	}

	public void setTerm(String term) {
		this.term = term;
	}

	public void setPosition(Position position) {
		this.position = position;
	}

	public String getTerm() {
		return this.term;
	}

	public Position getPosition() {
		return this.position;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj instanceof Mention) {
			Mention mention = (Mention) obj;
			if ((mention.getPosition() == this.position))
				return true;
			else
				return false;
		}
		return false;
	}

	public String getTag() {
		String tag = "[";
		tag = tag + this.type + "[";
		tag = tag + this.term + "]]";
		return tag;
	}

	public int hashCode() {
		int result = 17;
		result = 37 * result + position.hashCode();
		return result;
	}

	// ordering based on its position
	public int compareTo(Mention mention) {
		int c = this.position.compareTo(mention.getPosition());
		if (c != 0)
			return c;
		return 0;
	}

	public String toString() {
		if (term != null)
			return position + ":" + term;
		else
			return position.toString();
	}

}
