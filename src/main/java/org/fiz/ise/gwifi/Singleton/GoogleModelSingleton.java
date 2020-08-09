package org.fiz.ise.gwifi.Singleton;

import java.util.concurrent.TimeUnit;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.TimeUtil;

public class GoogleModelSingleton {
	private static GoogleModelSingleton single_instance = null;
	public Word2Vec google_model;
	
	private GoogleModelSingleton()
	{
		try {
			long now = TimeUtil.getStart();
			String ADDRESS_OF_GOOGLE_MODEL = Config.getString("ADDRESS_OF_GOOGLE_MODEL","");
			System.out.println("ADDRESS_OF_GOOGLE_MODEL "+ADDRESS_OF_GOOGLE_MODEL);
			google_model=WordVectorSerializer.readWord2VecModel(ADDRESS_OF_GOOGLE_MODEL);
			System.out.println("Time took to load model minutes :"+ TimeUnit.SECONDS.toMinutes(TimeUtil.getEnd(TimeUnit.SECONDS, now)));
		} catch (Exception e) {
			System.out.println("Exception initializing GOOGLESingleton");
		}
	}
	public static GoogleModelSingleton getInstance()
	{
		if (single_instance == null)
			single_instance = new GoogleModelSingleton();
		return single_instance;
	}
}
