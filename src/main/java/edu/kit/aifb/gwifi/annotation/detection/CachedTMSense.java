package edu.kit.aifb.gwifi.annotation.detection;

import edu.kit.aifb.gwifi.model.Label;

public class CachedTMSense {
     private Label.Sense sense;
     private String targetText;
     private String sourceText;
     
	public String getSourceText() {
		return sourceText;
	}
	public void setSourceText(String sourceText) {
		this.sourceText = sourceText;
	}
	public Label.Sense getSense() {
		return sense;
	}
	public void setSense(Label.Sense sense) {
		this.sense = sense;
	}
	public String getTargetText() {
		return targetText;
	}
	public void setTargetText(String targetText) {
		this.targetText = targetText;
	}
     
}
