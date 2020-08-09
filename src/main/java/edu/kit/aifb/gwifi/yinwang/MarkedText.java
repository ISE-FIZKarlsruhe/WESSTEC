package edu.kit.aifb.gwifi.yinwang;

public class MarkedText {
	
	private String doc_id;
	private String doc_text;
	private String is_parallel;
	
	public MarkedText(String doc_id, String doc_text)
	{
		this.doc_id = doc_id;
		this.doc_text = doc_text;
	}
	
	public MarkedText(String doc_id, String doc_text, String is_parallel)
	{
		this.doc_id = doc_id;
		this.doc_text = doc_text;
		this.is_parallel = is_parallel;
	}

	/**
	 * @return the doc_id
	 */
	public String getDoc_id() {
		return doc_id;
	}

	/**
	 * @return the doc_text
	 */
	public String getDoc_text() {
		return doc_text;
	}

	/**
	 * @return the is_parallel
	 */
	public String getIs_parallel() {
		return is_parallel;
	}

	/**
	 * @param doc_id the doc_id to set
	 */
	public void setDoc_id(String doc_id) {
		this.doc_id = doc_id;
	}

	/**
	 * @param doc_text the doc_text to set
	 */
	public void setDoc_text(String doc_text) {
		this.doc_text = doc_text;
	}

	/**
	 * @param is_parallel the is_parallel to set
	 */
	public void setIs_parallel(String is_parallel) {
		this.is_parallel = is_parallel;
	}
	

}
