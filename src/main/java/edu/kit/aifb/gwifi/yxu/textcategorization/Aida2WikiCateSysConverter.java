package edu.kit.aifb.gwifi.yxu.textcategorization;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import edu.kit.aifb.gwifi.model.Wikipedia;

public class Aida2WikiCateSysConverter extends CategorySystemConverter {
	
	private final static String SCR_SYS_NAME = "AIDA";
	private final static String TAR_SYS_NAME = "WIKI";

	protected HashMap<String, HashSet<String>> aida2wikiTitlesMap;
	protected HashMap<String, ArrayList<String>> wiki2aidaHierTitlesMap;
	
	public Aida2WikiCateSysConverter(Wikipedia wikipedia){
		super();
		scrCateSys = new AidaCategoryTree(SCR_SYS_NAME, null);
		tarCateSys = new WikiCategorySystem(TAR_SYS_NAME, wikipedia);
		aida2wikiTitlesMap = new HashMap<String, HashSet<String>>();
		wiki2aidaHierTitlesMap = new HashMap<String, ArrayList<String>>();
		convert();
		((WikiCategorySystem)tarCateSys).buildWikiCateArticleSet();
	}
	
	public void convert(){
		//manually mapping titles
		//0, CCAT
		mapAidaToWikiCate(1, "C11", "Strategic management");
		mapAidaToWikiCate(2, "C12", "Corporate law");
		mapAidaToWikiCate(3, "C13", "Economics of regulation");
		mapAidaToWikiCate(4, "C14", "Stock exchanges");
		mapAidaToWikiCate(4, "C14", "Securities (finance)");
		mapAidaToWikiCate(5, "C15", "Performance measurement");	//article
		mapAidaToWikiCate(6, "C151", "Financial accounting");
		mapAidaToWikiCate(7, "C1511", "Financial statements");
		mapAidaToWikiCate(8, "C152", "Economic forecasting");
		mapAidaToWikiCate(9, "C16", "Insolvency");
		mapAidaToWikiCate(10, "C17", "Financial capital");
		mapAidaToWikiCate(11, "C171", "Share capital");	//article
		mapAidaToWikiCate(12, "C172", "Bonds (finance)");
		mapAidaToWikiCate(13, "C173", "Loans");
		mapAidaToWikiCate(13, "C173", "Credit");
		mapAidaToWikiCate(14, "C174", "Credit rating");
		mapAidaToWikiCate(15, "C18", "Business ownership");
		mapAidaToWikiCate(16, "C181", "Mergers and acquisitions");
		mapAidaToWikiCate(17, "C182", "Financial transaction");	//article
		mapAidaToWikiCate(18, "C183", "Privatization");
		mapAidaToWikiCate(19, "C21", "Production and manufacturing");
		mapAidaToWikiCate(20, "C22", "New Product development");//article
		mapAidaToWikiCate(21, "C23", "Research and development");
		mapAidaToWikiCate(22, "C24", "Productive capacity");	//article
		mapAidaToWikiCate(23, "C31", "Marketing");
		mapAidaToWikiCate(24, "C311", "Domestic market");	//article
		mapAidaToWikiCate(25, "C312", "Export");
		mapAidaToWikiCate(26, "C313", "Market share");	//article
		mapAidaToWikiCate(27, "C32", "Promotion and marketing communications");
		mapAidaToWikiCate(28, "C33", "Contract");	//article
		mapAidaToWikiCate(28, "C33", "Order (business)");	//article
		mapAidaToWikiCate(29, "C331", "Military budgets");
		mapAidaToWikiCate(30, "C34", "Competition (economics)");
		mapAidaToWikiCate(31, "C41", "Management");
		mapAidaToWikiCate(32, "C411", "Change management");
		mapAidaToWikiCate(33, "C42", "Employee relations");
		//34, ECAT
		mapAidaToWikiCate(35, "E11", "Gross domestic product");
		mapAidaToWikiCate(36, "E12", "Monetary policy");
		mapAidaToWikiCate(37, "E121", "Money supply");	//article
		mapAidaToWikiCate(38, "E13", "Inflation");
		mapAidaToWikiCate(39, "E131", "Consumer price index");	//article
		mapAidaToWikiCate(40, "E132", "Wholesale price index");	//article
		mapAidaToWikiCate(41, "E14", "Consumption");
		mapAidaToWikiCate(42, "E141", "Personal income");	//article
		mapAidaToWikiCate(43, "E142", "Hire purchase");		//article	//article
		mapAidaToWikiCate(44, "E143", "Retailing");
		mapAidaToWikiCate(45, "E21", "Government finances");
		mapAidaToWikiCate(46, "E211", "Government budgets");
		mapAidaToWikiCate(47, "E212", "Government debt");
		mapAidaToWikiCate(48, "E31", "Production economics");
		mapAidaToWikiCate(49, "E311", "Industrial production_index");	//article
		mapAidaToWikiCate(50, "E312", "Capacity utilization");	//article
		mapAidaToWikiCate(51, "E313", "Revenue");
		mapAidaToWikiCate(52, "E41", "Employment");
		mapAidaToWikiCate(53, "E411", "Unemployment");
		mapAidaToWikiCate(54, "E51", "Trade");
		mapAidaToWikiCate(55, "E511", "Balance of payments");	//article
		mapAidaToWikiCate(56, "E512", "Balance of trade");	//article
		mapAidaToWikiCate(57, "E513", "Gold reserve");	//article
		mapAidaToWikiCate(57, "E513", "Foreign-exchange reserves");	//article
		mapAidaToWikiCate(57, "E513", "Bank reserves");	//article
		mapAidaToWikiCate(58, "E61", "Housing");
		mapAidaToWikiCate(59, "E71", "Economic indicators");
		//60, GCAT
		mapAidaToWikiCate(61, "G15", "European Union");
		mapAidaToWikiCate(62, "G151", "European Single Market");
		mapAidaToWikiCate(63, "G152", "Industry in the European Union");
		mapAidaToWikiCate(64, "G153", "European Union and agriculture");
		mapAidaToWikiCate(64, "G153", "Fishing in the European Union");
		mapAidaToWikiCate(65, "G154", "Finance in the European Union");
		mapAidaToWikiCate(66, "G155", "Institutions of the European Union");
		mapAidaToWikiCate(67, "G156", "European Union and the environment");
		mapAidaToWikiCate(68, "G157", "European Union competition law");
		mapAidaToWikiCate(69, "G158", "Foreign relations of the European Union");
		//70, G159
		mapAidaToWikiCate(71, "GCRIM", "Crime");
		mapAidaToWikiCate(71, "GCRIM", "Law enforcement");
		mapAidaToWikiCate(72, "GDEF", "Defense");
		mapAidaToWikiCate(73, "GDIP", "International relations");
		mapAidaToWikiCate(74, "GDIS", "Disasters");
		mapAidaToWikiCate(74, "GDIS", "Accidents");
		mapAidaToWikiCate(75, "GENT", "Entertainment");
		mapAidaToWikiCate(76, "GENV", "Natural environment");
		mapAidaToWikiCate(77, "GFAS", "Fashion");
		mapAidaToWikiCate(78, "GHEA", "Health");
		mapAidaToWikiCate(79, "GJOB", "Labor");
		mapAidaToWikiCate(80, "GMIL", "2000");
		mapAidaToWikiCate(81, "GOBIT", "Acknowledgements of death");
		mapAidaToWikiCate(82, "GODD", "Deviance (sociology)");
		mapAidaToWikiCate(83, "GPOL", "Domestic policy");
		mapAidaToWikiCate(84, "GPRO", "Biography (genre)");
		mapAidaToWikiCate(84, "GPRO", "Politicians");
		mapAidaToWikiCate(84, "GPRO", "Celebrity");
		mapAidaToWikiCate(85, "GREL", "Religion");
		mapAidaToWikiCate(86, "GSCI", "Science and technology");
		mapAidaToWikiCate(87, "GSPO", "Sports");
		mapAidaToWikiCate(88, "GTOUR", "Tourism");
		mapAidaToWikiCate(89, "GVIO", "War");
		mapAidaToWikiCate(89, "GVIO", "Civil wars");
		mapAidaToWikiCate(90, "GVOTE", "Elections");
		mapAidaToWikiCate(91, "GWEA", "Weather");
		mapAidaToWikiCate(92, "GWELF", "Welfare");
		mapAidaToWikiCate(92, "GWELF", "Social work");
		//93, MCAT
		mapAidaToWikiCate(94, "M11", "Stock market");
		mapAidaToWikiCate(95, "M12", "Fixed income market");
		mapAidaToWikiCate(96, "M13", "Money market instruments");
		mapAidaToWikiCate(97, "M131", "Interbank foreign exchange market");	//article
		mapAidaToWikiCate(98, "M132", "Foreign exchange market");
		mapAidaToWikiCate(99, "M14", "Commodity markets");
		mapAidaToWikiCate(100, "M141", "Soft commodity");	//article
		mapAidaToWikiCate(101, "M142", "Metal prices");
		mapAidaToWikiCate(102, "M143", "Energy markets");
		
	}
	
	private boolean mapAidaToWikiCate(Integer aidaID, String aidaTitle, String wikiTitle){
		int wikiID = ((WikiCategorySystem)tarCateSys).addWikiCateWithTitle(wikiTitle);
		if(wikiID == -1) return false; // wiki title doesn't exist in wikipedia database
		if(!mapAidaToWikiTitle(aidaTitle, wikiTitle)) return false; // wiki title is already mapped
		addIDMapTo(aidaID, wikiID, scrID2tarIDsMap);
		addIDMapTo(wikiID, aidaID, tarID2scrIDsMap);
		return true;
	}
	
	private boolean mapAidaToWikiTitle(String aidaTitle, String wikiTitle){
		HashSet<String> wikiTitles = aida2wikiTitlesMap.get(aidaTitle);
		if(wikiTitles == null){
			wikiTitles = new HashSet<String>();
			aida2wikiTitlesMap.put(aidaTitle, wikiTitles);
		}
		wikiTitles.add(wikiTitle);
		return addWikiToAidaHierTitlesMapTo(wikiTitle, aidaTitle);
	}
	
	private boolean addWikiToAidaHierTitlesMapTo(String wikiTitle, String aidaTitle){
		if(wiki2aidaHierTitlesMap.get(wikiTitle)!=null) return false;
		ArrayList<String> aidaHierTitles = ((AidaCategoryTree)scrCateSys).getHierTitlesOfcateTitle(aidaTitle);
		wiki2aidaHierTitlesMap.put(wikiTitle, aidaHierTitles);
		return true;
	}

	public Collection<String> getMappedWikiCateCol(){
		return ((WikiCategorySystem)tarCateSys).getWikiCates().values();
	}

	public Collection<String> getMappedWikiArticleCol(){
		return ((WikiCategorySystem)tarCateSys).getWikiArticles().values();
	}
	
	public int getWikiSysIDByTitle(String wikiTitle){
		return tarCateSys.getIDByCateTitle(wikiTitle);
	}
	
	public String getWikiSysTitleByID(int systemid){
		return tarCateSys.getCateTitleByID(systemid);
	}
	
	public int getAidaSysIDByTitle(String aidaTitle){
		return scrCateSys.getIDByCateTitle(aidaTitle);
	}
	
	public String getAidaSysTitleByID(int systemid){
		return scrCateSys.getCateTitleByID(systemid);
	}
	
	public HashMap<String, Double> revertWikiToAidaTitleWithScore(HashMap<String, Double> tarTitleWithScore){
		HashMap<String, Double> aidaTitleWithScore =  new HashMap<String, Double>();
		ArrayList<String> aidaHierTitles;
		String aidaTitle;
		Double aidaScore;
		for(String tarTitle: tarTitleWithScore.keySet()){
			aidaHierTitles = wiki2aidaHierTitlesMap.get(tarTitle);
			if(aidaHierTitles == null) continue;
			aidaTitle = aidaHierTitles.get(0);
			aidaScore = aidaTitleWithScore.get(aidaTitle);
			if(aidaScore == null) aidaScore = 0.0;
			aidaScore += tarTitleWithScore.get(tarTitle);
			aidaTitleWithScore.put(aidaTitle, aidaScore);
		}
		return aidaTitleWithScore;
	}
	
	public Collection<String> getHierAidaTitleCol(Collection<String> aidaTitleCol){
		HashSet<String> hierAidaTitles = new HashSet<String>();
		ArrayList<String> hierAidaTitleList;
		for(String aidaTitle: aidaTitleCol){
			hierAidaTitleList = ((AidaCategoryTree)scrCateSys).getHierTitlesOfcateTitle(aidaTitle);
			hierAidaTitles.addAll(hierAidaTitleList);
		}
		return hierAidaTitles;
	}

	private static int textid = 0;
	
	public static void convertFile(String aidafilename, String wikiFilename) throws IOException{
		int SP_DOC_LEN = 3;
		String SP_DOC = ":\t";
		String SP_CATE = ",";
		textid = 0;
		PrintWriter wikiFilePW = new PrintWriter(createFileWithPW(wikiFilename));
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				new FileInputStream(aidafilename), Charset.forName("UTF-8")));
		String aidaLine;
		String[] aidaDocArray;
		String[] aidaCateArray;
		String wikiLine;
		while ((aidaLine = reader.readLine()) != null) {
			aidaDocArray = aidaLine.split(SP_DOC);
			if (aidaDocArray.length != SP_DOC_LEN) continue;
			textid++;
			wikiLine = aidaDocArray[0] + SP_DOC + aidaDocArray[1] + SP_DOC;
			//TODO remove duplicated cate
			aidaCateArray = aidaDocArray[2].split(SP_CATE);
			for (String wikiCate: convertAida2WikiTitles(aidaCateArray)) {
				wikiLine += wikiCate + SP_CATE;
			}
			wikiFilePW.println(wikiLine);
		}
		reader.close();
		wikiFilePW.flush();
		wikiFilePW.close();
	}
	
	private static HashSet<String> convertAida2WikiTitles(String[] aidaTitles){
		String rootTiltPrefix = "#";
		HashSet<String> wikiTitles = new HashSet<String>();
		HashSet<String> rootTitles = new HashSet<String>();
		String aidaTitle;
		String wikiTitle;
		for (int i = 0; i < aidaTitles.length; i++) {
			aidaTitle = aidaTitles[i].trim();
			if (aidaTitle != null && aidaTitle.length() > 0) {
				wikiTitle = convertAida2WikiTitle(aidaTitle, rootTiltPrefix);
				if(wikiTitle == null) continue;
				if(wikiTitle.startsWith(rootTiltPrefix)) {
					wikiTitle = wikiTitle.substring(rootTiltPrefix.length());
					rootTitles.add(wikiTitle);
				}else{
					wikiTitles.add(wikiTitle);
				}
			}
		}
		mergeRootTitles(aidaTitles, rootTitles, wikiTitles);
		return wikiTitles;
	}
	
	private static void mergeRootTitles(String[] aidaTitles, 
			HashSet<String> rootTitles, HashSet<String> wikiTitles){
		boolean isMerging = true;
		for(String rootTitle: rootTitles){
			isMerging = true;
			for(String aidaTitle: aidaTitles){
				if(!rootTitle.equalsIgnoreCase(aidaTitle)){
					if(rootTitle.charAt(0)==aidaTitle.charAt(0)){
						isMerging = false;
						break;
					}
				}
			}
			if(isMerging){
				System.out.println(textid + "\t merge root title!");
				wikiTitles.addAll(convertRootAida2WikiTitle(rootTitle));
			}
		}
	}
	
	private static HashSet<String> convertRootAida2WikiTitle(String rootAidaTitle){
		HashSet<String> wikiTitles = new HashSet<String>();
		switch(rootAidaTitle){
		case "MCAT":
		case "ECAT":
			wikiTitles.add("Economy");
			break;
		case "CCAT":
			wikiTitles.add("Economy");
			wikiTitles.add("Industry");
			break;
		case "GCAT":
			wikiTitles.add("Politics");
			wikiTitles.add("Society");
			break;
		}
		return wikiTitles;
	}
	
	private static String convertAida2WikiTitle(String aidaTitle, String rootTiltPrefix){
		String wikiTitle = "";
		switch(aidaTitle){
		case "C11":
		case "C12":
		case "C13":
		case "C14":
		case "C15":
		case "C151":
		case "C1511":
		case "C152":
		case "C16":
		case "C17":
		case "C171":
		case "C172":
		case "C173":
		case "C174":
		case "C18":
		case "C181":
		case "C182":
		case "C183":
		case "C31":
		case "C311":
		case "C312":
		case "C313":
		case "C32":
		case "C33":
		case "C331":
		case "C34":
		case "C41":
		case "C411":
		case "C42":
		case "E11":
		case "E12":
		case "E121":
		case "E13":
		case "E131":
		case "E132":
		case "E14":
		case "E141":
		case "E142":
		case "E143":
		case "E21":
		case "E211":
		case "E212":
		case "E31":
		case "E311":
		case "E312":
		case "E313":
		case "E41":
		case "E411":
		case "E51":
		case "E511":
		case "E512":
		case "E513":
		case "E61":
		case "E71":
//		case "ECAT":
			
		case "GJOB":
			
		case "M11":
		case "M12":
		case "M13":
		case "M131":
		case "M132":
		case "M14":
		case "M141":
		case "M142":
		case "M143":
//		case "MCAT":
			wikiTitle = "Economy";
			break;
		case "C21":
		case "C22":
		case "C23":
		case "C24":
			wikiTitle = "Industry";
			break;
//		case "CCAT":
//			wikiTitle = "Industry";//Economy
//			break;
		case "G11":
		case "G112":
		case "G15":
		case "G151":
		case "G152":
		case "G153":
		case "G154":
		case "G155":
		case "G156":
		case "G157":
		case "G158":
		case "G159":
			wikiTitle = "Politics";
			break;
//		case "GCAT":
//			wikiTitle = "Politics";//Society
//			break;
		case "GCRIM":
			wikiTitle = "Society";
			break;
		case "G131":
		case "GDEF":
			wikiTitle = "Politics";
			break;
		case "G13":
		case "GDIP":
			wikiTitle = "Politics";
			break;
		case "GDIS":
			wikiTitle = "Society";
			break;
		case "G113":
		case "GEDU":
			wikiTitle = "Society";
			break;
		case "GENT":
			wikiTitle = "Culture";
			break;
		case "G14":
		case "GENV":
			wikiTitle = "Nature";
			break;
		case "GFAS":
			wikiTitle = "Culture";
			break;
		case "G111":
		case "GHEA":
			wikiTitle = "Health";
			break;
		case "GMIL":
		case "GOBIT":
		case "GODD":
			wikiTitle = "Society";
			break;
		case "G12":
		case "GPOL":
			wikiTitle = "Politics";
			break;
		case "GPRO":
			wikiTitle = "People";
			break;
		case "GREL":
			wikiTitle = "Religion";
			break;
		case "GSCI":
			wikiTitle = "Science and technology";
			break;
		case "GSPO":
			wikiTitle = "Sports";
			break;
		case "GTOUR":
			wikiTitle = "Culture";
			break;
		case "GVIO":
			wikiTitle = "Society";
			break;
		case "GVOTE":
			wikiTitle = "Politics";
			break;
		case "GWEA":
			wikiTitle = "Nature";
			break;
		case "GWELF":
			wikiTitle = "Society";
			break;
		default:
			wikiTitle = rootTiltPrefix + aidaTitle;
		}
		return wikiTitle;
	}
	
	private static File createFileWithPW(String filename)
			throws FileNotFoundException {
		File file = new File(filename);
		if (!file.exists()) {
			file.getParentFile().mkdirs();
		}
		return file;
	}
	
	public static void main(String[] args) throws IOException {
//		File databaseDirectory = new File("configs/wikipedia-template-en.xml");
//		Wikipedia wikipedia = null;
//		try {
//			wikipedia = new Wikipedia(databaseDirectory, false);
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		Aida2WikiCateSysConverter aida2wiki = new Aida2WikiCateSysConverter(wikipedia);
		convertFile(args[0], args[1]);
	}
}
