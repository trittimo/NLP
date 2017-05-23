package trittimo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.stanford.nlp.coref.CorefCoreAnnotations;
import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.coref.data.CorefChain.CorefMention;
import edu.stanford.nlp.coref.data.Dictionaries;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.util.CoreMap;

public class Test {
	
	public static void main(String[] args) throws Exception {

		Annotation document = new Annotation("Barack Obama was born in Hawaii. He was the president of the United States. Obama was elected in 2008. In Syria, he fought ISIS not Russia.");
		Annotation query = new Annotation("Obama fought ISIS not russia");

		System.out.println("---GETPIPELINE");
		StanfordCoreNLP pipeline = Preprocessor.getPipeline();
		System.out.println("---ANNOTATE DOC");
		pipeline.annotate(document);
		System.out.println("---ANNOTATE QUERY");
		pipeline.annotate(query);
		System.out.println("---SEARCH");
		DocumentSearcher.search(document, query, 0.5f);
		
	}	
	

}
