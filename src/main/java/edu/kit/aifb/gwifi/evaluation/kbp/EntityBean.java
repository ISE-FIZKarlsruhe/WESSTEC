package edu.kit.aifb.gwifi.evaluation.kbp;

public class EntityBean
{
	private String mentionLabel;
	private String entityname;
	private String beg;
	private String end;
	private String type;

	

	public EntityBean(String entityname, String mentionLabel, String beg, String end, String type)
	{
		this.mentionLabel = mentionLabel;
		this.entityname = entityname;
		this.beg = beg;
		this.end = end;
		this.type = type;

	}

	public String getType()
	{
		return type;
	}

	public void setType(String type)
	{
		this.type = type;
	}

	public String getMentionLabel()
	{
		return mentionLabel;
	}

	public void setMentionLabel(String mentionLabel)
	{
		this.mentionLabel = mentionLabel;
	}

	public String getEntityname()
	{
		return entityname;
	}

	public void setEntityname(String entityname)
	{
		this.entityname = entityname;
	}

	public String getBeg()
	{
		return beg;
	}

	public void setBeg(String beg)
	{
		this.beg = beg;
	}

	public String getEnd()
	{
		return end;
	}

	public void setEnd(String end)
	{
		this.end = end;
	}
	
}
