package edu.kit.aifb.gwifi.util.nlp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.kit.aifb.gwifi.util.nlp.Language;

public class TermListField {

	private Language m_language;
	private List<String> m_terms;

	protected TermListField(Language language) {
		m_language = language;
		m_terms = new ArrayList<String>();
	}

	protected TermListField(List<String> terms, Language language) {
		m_language = language;
		m_terms = terms;
	}

	protected TermListField(String[] termArray, Language language) {
		this(language);
		for (String term : termArray) {
			m_terms.add(term);
		}
	}

	public void addTerms(Collection<String> newTerms) {
		m_terms.addAll(newTerms);
	}

	public Language getLanguage() {
		return m_language;
	}

	public void setLanguage(Language language) {
		m_language = language;
	}

	public List<String> getTerms() {
		return m_terms;
	}

}
