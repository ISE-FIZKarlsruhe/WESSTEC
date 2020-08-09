package org.fiz.ise.gwifi.Singleton;

import java.util.concurrent.TimeUnit;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.fiz.ise.gwifi.model.EmbeddingModel;
import org.fiz.ise.gwifi.model.Dataset;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.TimeUtil;

public class LINE_modelSingleton {
    private static LINE_modelSingleton single_instance = null;
    public Word2Vec lineModel;
    private EmbeddingModel LINE_MODEL_NAME= Config.getEnumLine("LINE_MODEL_NAME");
    private LINE_modelSingleton()
    {
    	try {
    		long now = TimeUtil.getStart();
    		String ADDRESS_OF_LINE_MODEL="";
    		if (LINE_MODEL_NAME.equals(EmbeddingModel.LINE_COMBINED_Complex)) {
    			ADDRESS_OF_LINE_MODEL = Config.getString("ADDRESS_OF_LINE_COMBINED_Complex","");
			}
    		else if (LINE_MODEL_NAME.equals(EmbeddingModel.LINE_COMBINED_Complex_normalized)) {
    			ADDRESS_OF_LINE_MODEL = Config.getString("ADDRESS_OF_LINE_COMBINED_Complex_normalized","");
    		}
    		else if (LINE_MODEL_NAME.equals(EmbeddingModel.LINE_COMBINED_2nd)) {
    			ADDRESS_OF_LINE_MODEL = Config.getString("ADDRESS_OF_LINE_COMBINED_2nd","");
			}
    		else if (LINE_MODEL_NAME.equals(EmbeddingModel.LINE_1st_Complex)) {
    			ADDRESS_OF_LINE_MODEL = Config.getString("ADDRESS_OF_LINE_1st_Complex","");
			}
    		else if (LINE_MODEL_NAME.equals(EmbeddingModel.LINE_2nd_Complex)) {
    			ADDRESS_OF_LINE_MODEL = Config.getString("ADDRESS_OF_LINE_2nd_Complex","");
			}
    		else if (LINE_MODEL_NAME.equals(EmbeddingModel.CONLL)) {
    			ADDRESS_OF_LINE_MODEL = Config.getString("ADDRESS_OF_CONLL","");
			}
    		else if (LINE_MODEL_NAME.equals(EmbeddingModel.GOOGLE)) {
    			ADDRESS_OF_LINE_MODEL = Config.getString("ADDRESS_OF_GOOGLE","");
			}
    		else if (LINE_MODEL_NAME.equals(EmbeddingModel.PTE_modified)) {
    			ADDRESS_OF_LINE_MODEL = Config.getString("ADDRESS_OF_PTE_modified","");
			}
    		else if (LINE_MODEL_NAME.equals(EmbeddingModel.RDF2Vec)) {
    			ADDRESS_OF_LINE_MODEL = Config.getString("ADDRESS_OF_RDF2VEC","");
			}
    		else if (LINE_MODEL_NAME.equals(EmbeddingModel.LINE_Ent_Ent)) {
    			ADDRESS_OF_LINE_MODEL = Config.getString("ADDRESS_OF_LINE_Ent_Ent","");
			}
    		
    		
			System.out.println("ADDRESS_OF_LINE_MODEL "+ADDRESS_OF_LINE_MODEL);
    		lineModel=WordVectorSerializer.readWord2VecModel(ADDRESS_OF_LINE_MODEL);
    		System.out.println("Time took to load model minutes :"+ TimeUnit.SECONDS.toMinutes(TimeUtil.getEnd(TimeUnit.SECONDS, now)));
		} catch (Exception e) {
			System.out.println("Exception initializing LINE_modelSingleton");
			System.out.println(e.getMessage());
		}
    }
    public static LINE_modelSingleton getInstance()
    {
        if (single_instance == null)
        	single_instance = new LINE_modelSingleton();
        return single_instance;
    }
}
