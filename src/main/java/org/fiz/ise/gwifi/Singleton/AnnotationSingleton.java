package org.fiz.ise.gwifi.Singleton;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.fiz.ise.gwifi.dataset.shorttext.test.HeuristicApproach;
import org.w3c.dom.Element;

import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.annotation.detection.DisambiguationUtil;
import edu.kit.aifb.gwifi.annotation.detection.NLPTopicDetector;
import edu.kit.aifb.gwifi.annotation.detection.Topic;
import edu.kit.aifb.gwifi.annotation.preprocessing.DocumentPreprocessor;
import edu.kit.aifb.gwifi.annotation.preprocessing.HtmlPreprocessor;
import edu.kit.aifb.gwifi.annotation.preprocessing.PreprocessedDocument;
import edu.kit.aifb.gwifi.annotation.preprocessing.WikiPreprocessor;
import edu.kit.aifb.gwifi.annotation.weighting.TopicDisambiguator;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.service.NLPAnnotationService;
import edu.kit.aifb.gwifi.service.Service.DisambiguationModel;
import edu.kit.aifb.gwifi.service.Service.KB;
import edu.kit.aifb.gwifi.service.Service.LinkFormat;
import edu.kit.aifb.gwifi.service.Service.MentionMode;
import edu.kit.aifb.gwifi.service.Service.NLPModel;
import edu.kit.aifb.gwifi.service.Service.RepeatMode;
import edu.kit.aifb.gwifi.service.Service.ResponseFormat;
import edu.kit.aifb.gwifi.service.Service.ResponseMode;
import edu.kit.aifb.gwifi.service.Service.SourceMode;
import edu.kit.aifb.gwifi.util.nlp.Language;

public class AnnotationSingleton {
	private DocumentPreprocessor preprocessor; 
	private static AnnotationSingleton single_instance = null;
	 
	    // variable of type String
	 public NLPAnnotationService service;
	 public NLPTopicDetector topicDetector; 
	 private TopicDisambiguator topicDisambiguator;
	    // private constructor restricted to this class itself
	    private AnnotationSingleton()
	    {
	    	try {
	    		
//	    		service = new NLPAnnotationService("configs/hub-template.xml",
//	    				"configs/wikipedia-template-en.xml", "configs/NLPConfig.properties", Language.EN, Language.EN,
//	    				KB.WIKIPEDIA, NLPModel.NGRAM, DisambiguationModel.PRIOR, MentionMode.NON_OVERLAPPED, ResponseMode.BEST,
//	    				RepeatMode.FIRST);
	    		
//	    		service = new NLPAnnotationService("configs/hub-template.xml",
//	    				"configs/wikipedia-template-en.xml", "configs/NLPConfig.properties", Language.EN, Language.EN,
//	    				KB.WIKIPEDIA, NLPModel.NGRAM, DisambiguationModel.PRIOR, MentionMode.OVERLAPPED, ResponseMode.BEST,
//	    				RepeatMode.ALL);
	    		
	    		service = new NLPAnnotationService("configs/hub-template.xml",
	    				"configs/wikipedia-template-en.xml", "configs/NLPConfig.properties", Language.EN, Language.EN,
	    				KB.WIKIPEDIA, NLPModel.NGRAM, DisambiguationModel.PRIOR, MentionMode.NON_OVERLAPPED, ResponseMode.BEST,
	    				RepeatMode.ALL);
	    		
	    		DisambiguationUtil disambiguationUtil = new DisambiguationUtil(WikipediaSingleton.getInstance().wikipedia);
	    		topicDetector = new NLPTopicDetector(WikipediaSingleton.getInstance().wikipedia, disambiguationUtil, null, "configs/NLPConfig.properties", false, false);
	    		preprocessor = new WikiPreprocessor(WikipediaSingleton.getInstance().wikipedia);
	    		topicDisambiguator = new TopicDisambiguator(disambiguationUtil);
	    	} catch (Exception e) {
				System.out.println("Exception initializing Wikipedia: "+e.getMessage());
				System.exit(1);
			}
	    }
	    // static method to create instance of Singleton class
	    public static AnnotationSingleton getInstance()
	    {
	        if (single_instance == null)
	            single_instance = new AnnotationSingleton();
	        return single_instance;
	    }
	    public  List<Topic> getCandidates(String source) {
	    	if (source == null || source.trim().equals(""))
				return null;
			PreprocessedDocument doc = preprocessor.preprocess(source);
	    	List<Topic> topics;
	    	ArrayList<Topic> detectedTopics = null;
			try {
				topics = topicDetector.getTopics(doc, null, Language.EN, NLPModel.NGRAM);
				topics = topicDisambiguator.getWeightedTopics(topics, null, DisambiguationModel.PRIOR);
				detectedTopics = new ArrayList<Topic>();
				for (Topic topic : topics) {
					int id = topic.getId();
					String title = topic.getTitle();
					String displayName = service.extractCrossDescription(id, title, Language.EN, Language.EN);
					if (displayName == null || displayName.equals(""))
						continue;
					String uri = NLPAnnotationService.getURI(displayName, Language.EN, Language.EN, KB.WIKIPEDIA);
					topic.setURI(uri);
					topic.setDisplayName(displayName);
					
					if (topic.getWeight() >= WikipediaSingleton.getInstance().wikipedia.getConfig().getMinWeight())
						detectedTopics.add(topic);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	return detectedTopics;
		}
}
