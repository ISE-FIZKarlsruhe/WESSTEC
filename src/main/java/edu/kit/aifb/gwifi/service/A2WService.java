package edu.kit.aifb.gwifi.service;

import java.util.List;

import edu.kit.aifb.gwifi.annotation.Annotation;

public interface A2WService extends Service {

	public String annotate(String source, List<Annotation> annotations) throws Exception;

}
