package org.fiz.ise.gwifi.categoryTree;

import java.util.ArrayList;
import java.util.List;

import org.fiz.ise.gwifi.model.SubjectObject;

public class ListOfSubjectObject {
	private static final List<SubjectObject> LIST = new ArrayList<>();
	
	public static List<SubjectObject> getListOfSubjectObjects() {
		return LIST;
	}
}
