package edu.oregonstate.example;

import java.util.List;
import java.util.Properties;
import java.util.ArrayList;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

/**
 * From this example, we can see that the token span should not be the same;
 * For example new Span(6, 6)
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class TextSubstring {

	public static void main(String[] args) {
		String text = "Stanford University is a very good university";
		
		Properties props = new Properties();
	    props.put("annotators", "tokenize, ssplit, pos, lemma");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
	    Annotation seAnno = new Annotation(text);
	    pipeline.annotate(seAnno);
	    List<CoreMap> seSentences = seAnno.get(SentencesAnnotation.class);
	    ArrayList<String> sentence = new ArrayList<String>();
	    for(CoreMap ses : seSentences) {
	    	for (int j = 0; j < ses.get(TokensAnnotation.class).size(); j++) {
	    		CoreLabel token = ses.get(TokensAnnotation.class).get(j);
	    		String word = token.get(TextAnnotation.class);
	    		sentence.add(word);
	    	}
	    }
	    
	    String extent = "";
	    for (int i = 6; i < 6; i++) {
	    	extent = extent + sentence.get(i);
	    }
	    
	    System.out.println(extent);
	    System.out.println("Done");
	    
	}
}
