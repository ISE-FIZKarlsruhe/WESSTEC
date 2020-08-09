package edu.kit.aifb.gwifi.yxu.annotation.detection;

import edu.kit.aifb.gwifi.annotation.detection.TopicReference;
import edu.kit.aifb.gwifi.model.ILabel;
import edu.kit.aifb.gwifi.service.Service.NLPModel;
import edu.kit.aifb.gwifi.util.Position;
import edu.kit.aifb.gwifi.util.nlp.Language;

public class NLPTopicReference extends TopicReference {

	private NLPModel nlpModel;

	public NLPTopicReference(ILabel label, int topicId, Position position,
			NLPModel nlpModel) {
		super(label, topicId, position);
		this.nlpModel = nlpModel;
	}

	public NLPTopicReference(ILabel label, int topicId, Position position,
			String source, Language language, NLPModel nlpModel) {
		super(label, topicId, position, source, language);
		this.nlpModel = nlpModel;
	}

	public NLPTopicReference(ILabel label, Position position, NLPModel nlpModel) {
		super(label, position);
		this.nlpModel = nlpModel;
	}

	public NLPModel getNlpModel() {
		return nlpModel;
	}

	public void setNlpModel(NLPModel nlpModel) {
		this.nlpModel = nlpModel;
	}
}
