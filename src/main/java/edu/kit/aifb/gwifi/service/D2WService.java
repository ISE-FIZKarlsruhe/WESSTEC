package edu.kit.aifb.gwifi.service;

import java.util.List;
import java.util.Set;

import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.util.Position;

public interface D2WService extends Service {

	public String disambiguate(String source, Set<Position> positions, List<Annotation> annotations) throws Exception;

}
