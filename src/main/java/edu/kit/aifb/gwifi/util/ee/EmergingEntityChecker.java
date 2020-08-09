package edu.kit.aifb.gwifi.util.ee;

import java.io.IOException;
import java.util.Date;

public class EmergingEntityChecker {

	private static final long CACHE_TIME = 24*60*1000;
	
	private WikipediaApiInterface wikiApi;
	private long lastUpdateTime;
	
	public EmergingEntityChecker() {
		wikiApi = new WikipediaApiInterface(null, null);
		lastUpdateTime = (new Date()).getTime();
	} 
	
	public boolean isEmergingEntity(String mention) throws IOException {
		clearCache();
		int id = wikiApi.getIdByTitle(mention);
		if(id == -1)
			return true;
		else 
			return false;
	}
	
	private void clearCache() {
		long elapsedTime = (new Date()).getTime() - lastUpdateTime;
		if(elapsedTime >= CACHE_TIME) {
			wikiApi = new WikipediaApiInterface(null, null);
			lastUpdateTime = (new Date()).getTime();
		}
	}

}
