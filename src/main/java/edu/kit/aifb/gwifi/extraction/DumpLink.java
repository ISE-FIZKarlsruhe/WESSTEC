package edu.kit.aifb.gwifi.extraction;

import java.util.regex.*;

public class DumpLink {
	
	
	private String targetTitle ;
	private String targetSection ;
	private int targetNamespace ;
	private String targetLanguage ;
	
	private String anchor ;
	
	public DumpLink(String targetLanguage, int targetNamespace, String targetTitle, String targetSection, String anchor) {
		
		this.targetLanguage = targetLanguage ;
		this.targetNamespace = targetNamespace ;
		this.targetTitle = targetTitle ;
		this.targetSection = targetSection ;
		this.anchor = anchor ;
	}

	public String getAnchor() {
		return anchor;
	}

	public String getTargetLanguage() {
		return targetLanguage;
	}

	public int getTargetNamespace() {
		return targetNamespace;
	}

	public String getTargetSection() {
		return targetSection;
	}

	public String getTargetTitle() {
		return targetTitle;
	}
}
