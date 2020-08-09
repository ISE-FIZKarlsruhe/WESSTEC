package edu.kit.aifb.gwifi.util.nlp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.kit.aifb.gwifi.util.nlp.ITokenStream;
import edu.kit.aifb.gwifi.util.nlp.Language;

/**
 * Standard implementation of a text document, which also includes methods for
 * setting text.
 * 
 * @author pso
 * 
 */
public class TextDocument implements IDocument {

	// public static final String SPLIT_REGEX = "[\\s,;:!\\.\\?\\(\\)\\[\\]\\{\\}']";
	// public static final String SPLIT_REGEX = "[\\s\\p{P}\\p{Z}]";
	// public static final Pattern PATTERN_SPLIT = Pattern.compile("[\\s\\p{Z}\\p{Ps}\\p{Pe}\\p{Pi}\\p{Pf}]");
	public static final Pattern PATTERN_SPLIT = Pattern.compile("[\\s\\p{Z}\\p{Ps}\\p{Pe}\\p{Pi}\\p{Pf}]|\\p{P}\\p{Z}");
	public static final Pattern PATTERN_TOKEN = Pattern
			.compile("^[^\\p{L}\\p{N}]*([\\p{L}\\p{N}].*[\\p{L}\\p{N}])[^\\p{L}\\p{N}]*$");

	class TextField {
		Language language;
		String text;

		private TextField(Language language) {
			this.language = language;
		}
	}

	private String m_docName;
	private Map<String, TextField> m_fields;

	public TextDocument(String docName) {
		m_docName = docName;
		m_fields = new HashMap<String, TextField>();
	}

	public Set<String> getFields() {
		return m_fields.keySet();
	}
	
	public boolean isEmpty() {
		return getFields().size() ==  0 ? true : false;
	} 

	public Set<String> getFields(Language language) {
		Set<String> fields = new HashSet<String>();

		for (String field : m_fields.keySet()) {
			if (m_fields.get(field).language == language) {
				fields.add(field);
			}
		}

		return fields;
	}

	public Language getLanguage(String field) {
		if (m_fields.containsKey(field)) {
			return m_fields.get(field).language;
		} else {
			return null;
		}
	}

	public Set<Language> getLanguages() {
		Set<Language> languages = new HashSet<Language>();

		for (TextField f : m_fields.values()) {
			languages.add(f.language);
		}

		return languages;
	}

	public String getName() {
		return m_docName;
	}

	private TermListField getTermListField(String fieldName) {
		List<String> tokens = new ArrayList<String>();
		if (m_fields.get(fieldName).text != null) {
			for (String token : PATTERN_SPLIT.split(m_fields.get(fieldName).text)) {
				Matcher m = PATTERN_TOKEN.matcher(token);
				if (m.matches()) {
					tokens.add(m.group(1));
				}
			}
		}

		return new TermListField(tokens, m_fields.get(fieldName).language);
	}

	public ITokenStream getTokens() {
		List<TermListField> termListFields = new ArrayList<TermListField>();
		for (String fieldName : m_fields.keySet()) {
			termListFields.add(getTermListField(fieldName));
		}
		return new TermListTokenStream(termListFields);
	}

	public ITokenStream getTokens(String... fields) {
		List<TermListField> termListFields = new ArrayList<TermListField>();
		for (String field : fields) {
			if (m_fields.containsKey(field)) {
				termListFields.add(getTermListField(field));
			}
		}
		return new TermListTokenStream(termListFields);
	}

	public ITokenStream getTokens(Language language) {
		List<TermListField> fields = new ArrayList<TermListField>();
		for (String field : m_fields.keySet()) {
			if (m_fields.get(field).language == language) {
				fields.add(getTermListField(field));
			}
		}
		return new TermListTokenStream(fields);
	}

	public int compareTo(IDocument o) {
		return m_docName.compareTo(o.getName());
	}

	public void setLanguage(String fieldName, Language language) {
		if (m_fields.containsKey(fieldName)) {
			m_fields.get(fieldName).language = language;
		}
	}

	public Collection<String> getText() {
		List<String> textList = new ArrayList<String>();

		for (TextField field : m_fields.values()) {
			textList.add(field.text);
		}

		return textList;
	}

	public String getText(String field) {
		TextField textField = m_fields.get(field);
		if (textField != null) {
			return textField.text;
		} else {
			return null;
		}
	}

	public Collection<String> getText(Language language) {
		List<String> textList = new ArrayList<String>();

		for (TextField field : m_fields.values()) {
			if (field.language == language) {
				textList.add(field.text);
			}
		}

		return textList;
	}

	private TextField getField(String fieldName) {
		if (m_fields.containsKey(fieldName)) {
			return m_fields.get(fieldName);
		} else {
			TextField field = new TextField(Language.UNKNOWN);
			m_fields.put(fieldName, field);
			return field;
		}
	}

	public void setText(String fieldName, String text) {
		getField(fieldName).text = text;
	}

	public void setText(String fieldName, Language language, String text) {
		TextField field = getField(fieldName);
		field.language = language;
		field.text = text;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("name=").append(m_docName);
		sb.append("[");
		boolean first = true;
		for (String fieldName : m_fields.keySet()) {
			if (first) {
				first = false;
			} else {
				sb.append(";");
			}
			TextField field = m_fields.get(fieldName);
			sb.append(fieldName).append("@").append(field.language);
			sb.append("=").append(field.text);
		}
		sb.append("]");
		return sb.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof IDocument) {
			return m_docName.equals(((IDocument) o).getName());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return m_docName.hashCode();
	}

}
