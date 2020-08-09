package org.fiz.ise.gwifi.Singleton;

import java.util.concurrent.TimeUnit;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.fiz.ise.gwifi.model.EmbeddingModel;
import org.fiz.ise.gwifi.model.Dataset;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.TimeUtil;

public class LINE_2modelSingleton {
	private static LINE_2modelSingleton single_instance = null;
	public Word2Vec lineModel_1st;
	public Word2Vec lineModel_2nd;
	private LINE_2modelSingleton()
	{
		try {
			long now = TimeUtil.getStart();
			String ADDRESS_OF_LINE_MODEL_1 = Config.getString("ADDRESS_OF_LINE_1st_Complex","");
			String ADDRESS_OF_LINE_MODEL_2 = Config.getString("ADDRESS_OF_LINE_2nd_Complex","");

			System.out.println("ADDRESS_OF_LINE_MODEL_1 "+ADDRESS_OF_LINE_MODEL_1);
			System.out.println("ADDRESS_OF_LINE_MODEL_2 "+ADDRESS_OF_LINE_MODEL_2);

			lineModel_1st=WordVectorSerializer.readWord2VecModel(ADDRESS_OF_LINE_MODEL_1);
			lineModel_2nd=WordVectorSerializer.readWord2VecModel(ADDRESS_OF_LINE_MODEL_2);
			
			System.out.println("Time took to load model minutes :"+ TimeUnit.SECONDS.toMinutes(TimeUtil.getEnd(TimeUnit.SECONDS, now)));
		} catch (Exception e) {
			System.out.println("Exception initializing LINE_modelSingleton");
		}
	}
	public static LINE_2modelSingleton getInstance()
	{
		if (single_instance == null)
			single_instance = new LINE_2modelSingleton();
		return single_instance;
	}
}
