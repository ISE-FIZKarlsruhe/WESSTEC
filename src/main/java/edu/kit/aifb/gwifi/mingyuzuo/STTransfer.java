//package edu.kit.aifb.gwifi.mingyuzuo;
//
//import com.hankcs.hanlp.HanLP;
//import com.spreada.utils.chinese.ZHConverter;
//
//public class STTransfer
//{
//	public static void main(String[] args)
//	{
//		//第一种方法：导入jar包用ZHConverter方法，链接如下
//		//https://code.google.com/archive/p/java-zhconverter/
//		ZHConverter converter = ZHConverter.getInstance(ZHConverter.SIMPLIFIED);
//		String simplifiedStr = converter.convert("有背光的機械式鍵盤");
//		System.out.println(simplifiedStr);
//		String traplifiedStr = ZHConverter.convert("“以后等你当上皇后，就能买草莓庆祝了”机", ZHConverter.TRADITIONAL);
//		System.out.println(traplifiedStr);
//		
//		System.out.println("--------------------------");
//		
//		//第二中方法：用maven导入的包，且带词库，但是词库可能比较小，方法如下
//		//http://www.hankcs.com/nlp/java-chinese-characters-to-pinyin-and-simplified-conversion-realization.html
//	    HanLP.Config.enableDebug();
//	    System.out.println(HanLP.convertToSimplifiedChinese("「以後等妳當上皇后，就能買士多啤梨慶祝了」"));
//	    System.out.println(HanLP.convertToTraditionalChinese("“以后等你当上皇后，就能买草莓庆祝了”"));
//		
//	}
//	
//	
//	
//}
