package edu.kit.aifb.gwifi.yxu.annotation.detection;

import edu.kit.aifb.gwifi.annotation.detection.Topic;
import edu.kit.aifb.gwifi.annotation.detection.TopicReference;
import edu.kit.aifb.gwifi.model.ILabel;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.service.Service.NLPModel;

public class NLPTopic extends Topic {
	
	private NLPModel nlpModel;
	
	private ILabel label;

	public NLPTopic(Wikipedia wikipedia, int id, double relatednessToContext,
			double commonness, double docLength) {
		super(wikipedia, id, relatednessToContext, commonness, docLength);
	}
	
	public NLPTopic(Wikipedia wikipedia, int id, double relatednessToContext,
			double commonness, double docLength, NLPModel nlpModel) {
		super(wikipedia, id, relatednessToContext, commonness, docLength);
		this.setNlpModel(nlpModel);
	}

	public NLPModel getNlpModel() {
		return nlpModel;
	}

	public void setNlpModel(NLPModel nlpModel) {
		this.nlpModel = nlpModel;
	}

	public ILabel getLabel() {
		return label;
	}

	public void setLabel(ILabel label) {
		this.label = label;
	}
	
	public void refreshNLPModelAndLabel() {
		if(getReferences().size()>0){
			TopicReference topicRef = getReferences().get(0);
			this.label = topicRef.getLabel();
			if(topicRef instanceof NLPTopicReference){
				this.nlpModel = ((NLPTopicReference)topicRef).getNlpModel();
			}
		}
	}

}
