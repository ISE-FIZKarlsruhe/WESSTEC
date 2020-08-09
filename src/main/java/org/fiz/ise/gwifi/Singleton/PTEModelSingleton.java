package org.fiz.ise.gwifi.Singleton;

import java.util.concurrent.TimeUnit;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.TimeUtil;

public class PTEModelSingleton {
	private static PTEModelSingleton single_instance = null;
	public Word2Vec pte_model;
	
	private PTEModelSingleton()
	{
		try {
			long now = TimeUtil.getStart();
			String ADDRESS_OF_PTE_MODEL = Config.getString("ADDRESS_OF_PTE_modified","");
			System.out.println("ADDRESS_OF_PTE_MODEL "+ADDRESS_OF_PTE_MODEL);
			pte_model=WordVectorSerializer.readWord2VecModel(ADDRESS_OF_PTE_MODEL);
			System.out.println("Time took to load model minutes :"+ TimeUnit.SECONDS.toMinutes(TimeUtil.getEnd(TimeUnit.SECONDS, now)));
		} catch (Exception e) {
			System.out.println("Exception initializing PTESingleton");
		}
	}
	public static PTEModelSingleton getInstance()
	{
		if (single_instance == null)
			single_instance = new PTEModelSingleton();
		return single_instance;
	}
}
