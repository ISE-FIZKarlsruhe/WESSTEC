package org.fiz.ise.gwifi.Singleton;

import java.util.concurrent.TimeUnit;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.TimeUtil;

public class Doc2VecModelSingleton {
	private static Doc2VecModelSingleton single_instance = null;
	public Word2Vec doc2vec_model;
	
	private Doc2VecModelSingleton()
	{
		try {
			long now = TimeUtil.getStart();
			String ADDRESS_OF_DOC2VEC_MODEL = Config.getString("ADDRESS_OF_DOC2VEC_MODEL","");
			System.out.println("ADDRESS_OF_DOC2VEC_MODEL "+ADDRESS_OF_DOC2VEC_MODEL);
			doc2vec_model=WordVectorSerializer.readWord2VecModel(ADDRESS_OF_DOC2VEC_MODEL);
			System.out.println("Time took to load model minutes :"+ TimeUnit.SECONDS.toMinutes(TimeUtil.getEnd(TimeUnit.SECONDS, now)));
		} catch (Exception e) {
			System.out.println("Exception initializing GOOGLESingleton");
		}
	}
	public static Doc2VecModelSingleton getInstance()
	{
		if (single_instance == null)
			single_instance = new Doc2VecModelSingleton();
		return single_instance;
	}
}
