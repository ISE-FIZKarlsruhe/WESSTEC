package edu.kit.aifb.gwifi.mingyuzuo;

public class NILBean
{
	private String query_id;
	private String query_name;	
	private String nilInKbp;
	private String entity_id;
	
	
	public NILBean(String query_id, String query_name, String nilInKbp, String entity_id)
	{
		this.query_id = query_id;
		this.query_name = query_name;
		this.nilInKbp = nilInKbp;
		this.entity_id = entity_id;
	}


	public String getQuery_id()
	{
		return query_id;
	}


	public void setQuery_id(String query_id)
	{
		this.query_id = query_id;
	}


	public String getQuery_name()
	{
		return query_name;
	}


	public void setQuery_name(String query_name)
	{
		this.query_name = query_name;
	}


	public String getNilInKbp()
	{
		return nilInKbp;
	}


	public void setNilInKbp(String nilInKbp)
	{
		this.nilInKbp = nilInKbp;
	}


	public String getEntity_id()
	{
		return entity_id;
	}


	public void setEntity_id(String entity_id)
	{
		this.entity_id = entity_id;
	}
	
}
