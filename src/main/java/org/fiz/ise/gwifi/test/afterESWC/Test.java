package org.fiz.ise.gwifi.test.afterESWC;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.fiz.ise.gwifi.Singleton.AnnotationSingleton;
import org.fiz.ise.gwifi.Singleton.CategorySingleton;
import org.fiz.ise.gwifi.Singleton.LINE_modelSingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.category.Categories;
import org.fiz.ise.gwifi.model.Dataset;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.VectorUtil;

import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.service.NLPAnnotationService;

public class Test {
	private final static Dataset TEST_DATASET_TYPE = Config.getEnum("TEST_DATASET_TYPE");
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	static final Logger resultLog = Logger.getLogger("reportsLogger");

	public static void main(String[] args) throws Exception {
		
		//ADDRESS_OF_LINE_Ent_Ent=
		
//		String shortText="Cristiano Ronaldo stands alone as the most prolific Champions League goalscorer of all time";
		String shortText="LeBron James among NBA stars supporting under-fire ESPN reporter";
		NLPAnnotationService service = AnnotationSingleton.getInstance().service;
		List<Annotation> lstAnnotations = new ArrayList<>();
		service.annotate(shortText, lstAnnotations);//annotate the given text
		List<String> words = new ArrayList<String>();
		
		for(Annotation a : lstAnnotations) {
			words.add(Integer.toString(a.getId()));
		}
		List<String> labels=new ArrayList<String>();
		labels.add(Integer.toString(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Football").getId()));
		labels.add(Integer.toString(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Basketball").getId()));
		
		
		Word2Vec lineModel=WordVectorSerializer.readWord2VecModel("/home/rtue/eclipse-workspace/Resources/Models/afterESWC/EntEnt/vec_all_EntEnt_filtered1_dense.bin");
		//Word2Vec lineModel=WordVectorSerializer.readWord2VecModel("/home/rtue/eclipse-workspace/Resources/Models/GoogleNews-vectors-negative300.bin");

	
		double[] sentenceVector = VectorUtil.getSentenceVector(words, lineModel);
		
		for (String l : labels ) {
			System.out.println(VectorUtil.getSimilarity2Vecs(sentenceVector, lineModel.getWordVector(l)));
		}
		
		
		//		Set<Category> setMainCategories = new HashSet<>(
//				CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setMainCategories); //get predefined cats
		//LINE_modelSingleton.getInstance();
//		//categoriesExistAsEntity(setMainCategories);
//		categoriesExistAsEntityAnMostSimilarEntities(setMainCategories);
	}

	private static void categoriesExistAsEntityAnMostSimilarEntities(Set<Category> setMainCategories) {
		Set<Category> catsAll = new HashSet<>(
				CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setMainCategories);
		for(Category c : catsAll) {
			if (WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(c.getTitle())!=null) {
				String aCId= String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(c.getTitle()).getId());
				Collection<String> wordsNearest = LINE_modelSingleton.getInstance().lineModel.wordsNearest(aCId, 500);
				for(String str :wordsNearest) {
					if (WikipediaSingleton.getInstance().wikipedia.getArticleById(Integer.valueOf(str))!=null) {
						secondLOG.info(c.getTitle()+" "+WikipediaSingleton.getInstance().wikipedia.getArticleById(Integer.valueOf(str)).getTitle());
				}
			}	
		}
		secondLOG.info("\n");
	}
}

public static void categoriesExistAsEntity (Set<Category> cList) {
	Set<Category> catsAll = new HashSet<>(
			CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setAllCategories);
	int total = catsAll.size();
	int countNotExist =0;
	for(Category c : catsAll) {
		for(Category cC : catsAll) {
			System.out.println(c.getTitle()+" "+cC.getTitle()+" "+LINE_modelSingleton.getInstance().lineModel.similarity(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(c.getTitle()).getId()), String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(cC.getTitle()).getId())));
		}
	}


	for(Category c : catsAll) {
		//int aId = WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(c.getTitle()).getId();
		//System.out.println(c+"\t"+LINE_modelSingleton.getInstance().lineModel.hasWord(String.valueOf(aId)));
		System.out.println(c);
		if (WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(c.getTitle())==null) {
			//System.out.println(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(c.getTitle()));
			countNotExist++;
		}
		else
			System.out.println(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(c.getTitle()));

		System.out.println(total+" "+countNotExist);
		//			System.out.println(aId+" "+WikipediaSingleton.getInstance().wikipedia.getArticleById(aId));
		//			Collection<String> wordsNearest = LINE_modelSingleton.getInstance().lineModel.wordsNearest(String.valueOf(aId), 10);
		//			for(String s : wordsNearest) {
		//				System.out.println(WikipediaSingleton.getInstance().wikipedia.getArticleById(Integer.valueOf(s)));
		//			}
		System.out.println();
		System.out.println();
		//			int aId = WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(c.getTitle()).getId();
		//			int id =WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Kansas").getId();
		//			System.out.println(c.getTitle()+"\t"+LINE_modelSingleton.getInstance().lineModel.hasWord(String.valueOf(aId)));
		//			System.out.println("Kansas"+"\t"+LINE_modelSingleton.getInstance().lineModel.hasWord(String.valueOf(id)));
		//			System.out.println(c.getTitle()+"\t"+getWordVector(String.valueOf(aId)));
		//			System.out.println("Kansas"+"\t"+getWordVector(String.valueOf(id)));
		//			System.out.println(c.getTitle()+"\t similarity:"+LINE_modelSingleton.getInstance().lineModel.similarity(String.valueOf(id),String.valueOf(aId)));
		//			System.out.println();
	}

}
private static List<Double> getWordVector(String id) {
	final double[] wordVector = LINE_modelSingleton.getInstance().lineModel.getWordVector(id);
	if(wordVector==null){
		return null;
	}
	return Arrays.asList(ArrayUtils.toObject(wordVector));
}

}
