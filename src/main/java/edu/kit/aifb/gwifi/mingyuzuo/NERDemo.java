package edu.kit.aifb.gwifi.mingyuzuo;

import edu.stanford.nlp.ie.crf.*;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.AnswerAnnotation;
import edu.stanford.nlp.util.StringUtils;

import java.util.List;
import java.io.IOException;



/** This is a demo of calling CRFClassifier programmatically.
 *  <p>
 *  Usage: <code> java -cp "stanford-ner.jar:." NERDemo [serializedClassifier [fileName]]</code>
 *  <p>
 *  If arguments aren't specified, they default to
 *  ner-eng-ie.crf-3-all2006.ser.gz and some hardcoded sample text.
 *  <p>
 *  To use CRFClassifier from the command line:
 *  java -mx400m edu.stanford.nlp.ie.crf.CRFClassifier -loadClassifier
 *      [classifier] -textFile [file]
 *  Or if the file is already tokenized and one word per line, perhaps in
 *  a tab-separated value format with extra columns for part-of-speech tag,
 *  etc., use the version below (note the 's' instead of the 'x'):
 *  java -mx400m edu.stanford.nlp.ie.crf.CRFClassifier -loadClassifier
 *      [classifier] -testFile [file]
 *
 *  @author Jenny Finkel
 *  @author Christopher Manning
 */

public class NERDemo {

    public static void main(String[] args) throws IOException {

      String serializedClassifier = "/home/zmy/Library/stanford/NERClassifer/chinese.misc.distsim.crf.ser.gz";

      if (args.length > 0) {
        serializedClassifier = args[0];
      }

      AbstractSequenceClassifier classifier = CRFClassifier.getClassifierNoExceptions(serializedClassifier);

      /* For either a file to annotate or for the hardcoded text example,
         this demo file shows two ways to process the output, for teaching
         purposes.  For the file, it shows both how to run NER on a String
         and how to run it on a whole file.  For the hard-coded String,
         it shows how to run it on a single sentence, and how to do this
         and produce an inline XML output format.
      */
//      if (args.length > 1) {
//        String fileContents = StringUtils.slurpFile(args[1]);
//        List<List<CoreLabel>> out = classifier.classify(fileContents);
//        for (List<CoreLabel> sentence : out) {
//          for (CoreLabel word : sentence) {
//            System.out.print(word.word() + '/' + word.get(AnswerAnnotation.class) + ' ');
//          }
//          System.out.println();
//        }
//        out = classifier.classifyFile(args[1]);
//        for (List<CoreLabel> sentence : out) {
//          for (CoreLabel word : sentence) {
//            System.out.print(word.word() + '/' + word.get(AnswerAnnotation.class) + ' ');
//          }
//          System.out.println();
//        }
//
//      } else {
        String s1 = "Good afternoon Rajat Raina, how are you today?";
        String s2 = "Rajat Raina, I go to school at Stanford University, which is located in California.";
        String s3 = "今天,被誉 为“全国中国画、人物画大检阅”的“红船颂”全国中国 画名家作品邀请展在上海正式开幕,从嘉兴南湖来到申城 浦江,23幅精品绘画吸引了众多的观众。\n此次画展由中共嘉兴市市委、市人民政府发起,与中 国美协、浙江省美协联合主办。从今年2月起,来自22 个省、市、自治区和中国人民解放军中的实力派画家积极 响应,共挥毫创作了80幅作品。这些作品反映了党在各 个历史时期的重大事件和人物,塑造了领袖、革命先烈、 普通战士和人民群众丰富多彩的艺术形象。主办方从中选 取了23幅作为巡展的作品。其中,《人民军队的缔造者》 、《延安五老》等作品都在表现形式和笔法上有所创新, 令人印象深刻。";
        System.out.println(classifier.classifyToString(s1));
        System.out.println(classifier.classifyWithInlineXML(s3));
        System.out.println(classifier.classifyToString(s3, "xml", true));
      }
    

}
