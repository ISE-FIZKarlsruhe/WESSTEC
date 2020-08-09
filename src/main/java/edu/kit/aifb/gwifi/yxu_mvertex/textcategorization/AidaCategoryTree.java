package edu.kit.aifb.gwifi.yxu_mvertex.textcategorization;

import java.util.ArrayList;
import java.util.HashMap;

public class AidaCategoryTree extends CategoryTree {
	
	private HashMap<String, ArrayList<String>> cateTitle2HierTitlesMap;

	public AidaCategoryTree(String systemName, String cateFileName) {
		super(systemName);
		this.cateTitle2HierTitlesMap = new HashMap<String, ArrayList<String>>();
		initCategories();
		buildHierTitlesMap(depth);
	}
	
	public void initCategories(){
		//TODO manually initialize
		int ccat = addCate("CCAT");
		
		int c11 = addCate("C11");
		addRel(ccat, c11);
		int c12 = addCate("C12");
		addRel(ccat, c12);
		int c13 = addCate("C13");
		addRel(ccat, c13);
		int c14 = addCate("C14");
		addRel(ccat, c14);
		int c15 = addCate("C15");
		addRel(ccat, c15);
		int c151 = addCate("C151");
		addRel(c15, c151);
		int c1511 = addCate("C1511");
		addRel(c151, c1511);
		int c152 = addCate("C152");
		addRel(c15, c152);
		int c16 = addCate("C16");
		addRel(ccat, c16);
		int c17 = addCate("C17");
		addRel(ccat, c17);
		int c171 = addCate("C171");
		addRel(c17, c171);
		int c172 = addCate("C172");
		addRel(c17, c172);
		int c173 = addCate("C173");
		addRel(c17, c173);
		int c174 = addCate("C174");
		addRel(c17, c174);
		int c18 = addCate("C18");
		addRel(ccat, c18);
		int c181 = addCate("C181");
		addRel(c18, c181);
		int c182 = addCate("C182");
		addRel(c18, c182);
		int c183 = addCate("C183");
		addRel(c18, c183);
		int c21 = addCate("C21");
		addRel(ccat, c21);
		int c22 = addCate("C22");
		addRel(ccat, c22);
		int c23 = addCate("C23");
		addRel(ccat, c23);
		int c24 = addCate("C24");
		addRel(ccat, c24);
		int c31 = addCate("C31");
		addRel(ccat, c31);
		int c311 = addCate("C311");
		addRel(c31, c311);
		int c312 = addCate("C312");
		addRel(c31, c312);
		int c313 = addCate("C313");
		addRel(c31, c313);
		int c32 = addCate("C32");
		addRel(ccat, c32);
		int c33 = addCate("C33");
		addRel(ccat, c33);
		int c331 = addCate("C331");
		addRel(c33, c331);
		int c34 = addCate("C34");
		addRel(ccat, c34);
		int c41 = addCate("C41");
		addRel(ccat, c41);
		int c411 = addCate("C411");
		addRel(c41, c411);
		int c42 = addCate("C42");
		addRel(ccat, c42);

		int ecat = addCate("ECAT");

		int e11 = addCate("E11");
		addRel(ecat, e11);
		int e12 = addCate("E12");
		addRel(ecat, e12);
		int e121 = addCate("E121");
		addRel(e12, e121);
		int e13 = addCate("E13");
		addRel(ecat, e13);
		int e131 = addCate("E131");
		addRel(e13, e131);
		int e132 = addCate("E132");
		addRel(e13, e132);
		int e14 = addCate("E14");
		addRel(ecat, e14);
		int e141 = addCate("E141");
		addRel(e14, e141);
		int e142 = addCate("E142");
		addRel(e14, e142);
		int e143 = addCate("E143");
		addRel(e14, e143);
		int e21 = addCate("E21");
		addRel(ecat, e21);
		int e211 = addCate("E211");
		addRel(e21, e211);
		int e212 = addCate("E212");
		addRel(e21, e212);
		int e31 = addCate("E31");
		addRel(ecat, e31);
		int e311 = addCate("E311");
		addRel(e31, e311);
		int e312 = addCate("E312");
		addRel(e31, e312);
		int e313 = addCate("E313");
		addRel(e31, e313);
		int e41 = addCate("E41");
		addRel(ecat, e41);
		int e411 = addCate("E411");
		addRel(e41, e411);
		int e51 = addCate("E51");
		addRel(ecat, e51);
		int e511 = addCate("E511");
		addRel(e51, e511);
		int e512 = addCate("E512");
		addRel(e51, e512);
		int e513 = addCate("E513");
		addRel(e51, e513);
		int e61 = addCate("E61");
		addRel(ecat, e61);
		int e71 = addCate("E71");
		addRel(ecat, e71);
		
		int gcat = addCate("GCAT");

		int g15 = addCate("G15");
		addRel(gcat, g15);
		int g151 = addCate("G151");
		addRel(g15, g151);
		int g152 = addCate("G152");
		addRel(g15, g152);
		int g153 = addCate("G153");
		addRel(g15, g153);
		int g154 = addCate("G154");
		addRel(g15, g154);
		int g155 = addCate("G155");
		addRel(g15, g155);
		int g156 = addCate("G156");
		addRel(g15, g156);
		int g157 = addCate("G157");
		addRel(g15, g157);
		int g158 = addCate("G158");
		addRel(g15, g158);
		int g159 = addCate("G159");
		addRel(g15, g159);
		int gcrim = addCate("GCRIM");
		addRel(gcat, gcrim);
		int gdef = addCate("GDEF");
		addRel(gcat, gdef);
		int gdip = addCate("GDIP");
		addRel(gcat, gdip);
		int gdis = addCate("GDIS");
		addRel(gcat, gdis);
		int gent = addCate("GENT");
		addRel(gcat, gent);
		int genv = addCate("GENV");
		addRel(gcat, genv);
		int gfas = addCate("GFAS");
		addRel(gcat, gfas);
		int ghea = addCate("GHEA");
		addRel(gcat, ghea);
		int gjob = addCate("GJOB");
		addRel(gcat, gjob);
		int gmil = addCate("GMIL");
		addRel(gcat, gmil);
		int gobit = addCate("GOBIT");
		addRel(gcat, gobit);
		int godd = addCate("GODD");
		addRel(gcat, godd);
		int gpol = addCate("GPOL");
		addRel(gcat, gpol);
		int gpro = addCate("GPRO");
		addRel(gcat, gpro);
		int grel = addCate("GREL");
		addRel(gcat, grel);
		int gsci = addCate("GSCI");
		addRel(gcat, gsci);
		int gspo = addCate("GSPO");
		addRel(gcat, gspo);
		int gtour = addCate("GTOUR");
		addRel(gcat, gtour);
		int gvio = addCate("GVIO");
		addRel(gcat, gvio);
		int gvote = addCate("GVOTE");
		addRel(gcat, gvote);
		int gwea = addCate("GWEA");
		addRel(gcat, gwea);
		int gwelf = addCate("GWELF");
		addRel(gcat, gwelf);
		
		int mcat = addCate("MCAT");
		
		int m11 = addCate("M11");
		addRel(mcat, m11);
		int m12 = addCate("M12");
		addRel(mcat, m12);
		int m13 = addCate("M13");
		addRel(mcat, m13);
		int m131 = addCate("M131");
		addRel(m13, m131);
		int m132 = addCate("M132");
		addRel(m13, m132);
		int m14 = addCate("M14");
		addRel(mcat, m14);
		int m141 = addCate("M141");
		addRel(m14, m141);
		int m142 = addCate("M142");
		addRel(m14, m142);
		int m143 = addCate("M143");
		addRel(m14, m143);
		
		depth = 4;
	}
	
	private void buildHierTitlesMap(int depth){
		for(Integer id: id2categoryTitleGroupMap.keySet()){
			if(!id2inIDsMap.containsKey(id)){
				buildSubcateTitles(id, new ArrayList<String>(), depth);
			}
		}
	}
	
	private void buildSubcateTitles(Integer curID, ArrayList<String> parentHierTitles, int depth){
		if(depth<0) return;
		String curTitle = getCateTitleByID(curID);
		ArrayList<String> curHierTitles = new ArrayList<String>();
		curHierTitles.add(curTitle);
		curHierTitles.addAll(parentHierTitles);
		cateTitle2HierTitlesMap.put(curTitle, curHierTitles);
		if(!id2outIDsMap.containsKey(curID)) return;
		for(Integer subid: id2outIDsMap.get(curID)){
			buildSubcateTitles(subid, curHierTitles, depth-1);
		}
	}
	
	public ArrayList<String> getHierTitlesOfcateTitle(String cateTitle){
		return cateTitle2HierTitlesMap.get(cateTitle);
	}
	
	public static void main(String[] args) {
//		AidaCategoryTree aidaCateTree = new AidaCategoryTree("aida", "");
		
	}

}
