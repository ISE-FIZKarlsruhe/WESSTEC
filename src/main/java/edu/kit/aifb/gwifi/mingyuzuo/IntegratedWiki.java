package edu.kit.aifb.gwifi.mingyuzuo;

public class IntegratedWiki {

	private Wiki wikiZh2En;
	private Wiki wikiDe2En;
	private Wiki wikiZh2De;
	private Wiki wikiEn2De;
	private Wiki wikiEn2Zh;
	private Wiki wikiDe2Zh;
	
	public IntegratedWiki(String test, String slang, String tlang)
	{
		if(LanguageContains.ZH.equals(slang) && LanguageContains.EN.equals(tlang))
		{
			this.wikiZh2En = new Wiki(test, LanguageContains.ZH, LanguageContains.EN);
		}
		else if(LanguageContains.DE.equals(slang) && LanguageContains.EN.equals(tlang))
		{
			this.wikiDe2En = new Wiki(test, LanguageContains.DE, LanguageContains.EN);
		}
		else if(LanguageContains.ZH.equals(slang) && LanguageContains.DE.equals(tlang))
		{
			this.wikiZh2De = new Wiki(test, LanguageContains.ZH, LanguageContains.DE);
		}
		else if(LanguageContains.EN.equals(slang) && LanguageContains.DE.equals(tlang))
		{
			this.wikiEn2De = new Wiki(test, LanguageContains.EN, LanguageContains.DE);
		}
		else if(LanguageContains.EN.equals(slang) && LanguageContains.ZH.equals(tlang))
		{
			this.wikiEn2Zh = new Wiki(test, LanguageContains.EN, LanguageContains.ZH);
		}
		else if(LanguageContains.DE.equals(slang) && LanguageContains.ZH.equals(tlang))
		{
			this.wikiDe2Zh = new Wiki(test, LanguageContains.DE, LanguageContains.ZH);
		}
	}
	
	
	public Wiki getWikiZh2En() {
		return wikiZh2En;
	}

	public Wiki getWikiDe2En() {
		return wikiDe2En;
	}

	public Wiki getWikiZh2De() {
		return wikiZh2De;
	}

	public Wiki getWikiEn2De() {
		return wikiEn2De;
	}

	public Wiki getWikiEn2Zh() {
		return wikiEn2Zh;
	}

	public Wiki getWikiDe2Zh() {
		return wikiDe2Zh;
	}
}
