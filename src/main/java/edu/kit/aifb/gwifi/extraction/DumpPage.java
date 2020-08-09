package edu.kit.aifb.gwifi.extraction;

import java.util.Date;

import edu.kit.aifb.gwifi.model.Page.PageType;

public class DumpPage {

	private int id ;
	private int namespace ;
	private PageType type ;

	private String title ;
	private String markup ;
	private String target ;
	private Date lastEdited ;
	
	public DumpPage(int id, int namespace, PageType type, String title, String markup, String target, Date lastEdited) {
		
		this.id = id ;
		this.namespace = namespace ;
		
		this.type = type ;
		
		this.title = title ;
		this.markup = markup ;
		this.target = target ;
		
		this.lastEdited = lastEdited ;
	}

	public int getId() {
		return id;
	}

	public String getMarkup() {
		return markup;
	}

	public int getNamespace() {
		return namespace;
	}

	public String getTitle() {
		return title;
	}

	public PageType getType() {
		return type;
	}
	
	public String getTarget() {
		return target ;
	}
	
	public Date getLastEdited() {
		return lastEdited ;
	}
	
	@Override
	public String toString() {
		return id + ":" + title ;
	}
}
