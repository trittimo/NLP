package trittimo;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import edu.stanford.nlp.coref.CorefCoreAnnotations;
import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.IntPair;

public class Test {
	public static void main(String[] args) throws Exception {

		Annotation document = new Annotation("Barack Obama was born in Hawaii. He is the president of the United States. Obama was elected in 2008.");

		Properties props = new Properties();
		props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner,parse,mention,dcoref");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		pipeline.annotate(document);
		System.out.println("---");
		List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
		Collection<CorefChain> chains = document.get(CorefCoreAnnotations.CorefChainAnnotation.class).values();
		for (CorefChain chain : chains) {
			Map<IntPair,Set<CorefChain.CorefMention>> map = chain.getMentionMap();
			for (IntPair ip : map.keySet()) {
				for (CorefChain.CorefMention cm : map.get(ip)) {
					System.out.printf("(%s) %s - Sentence %d%n", chain.getRepresentativeMention().mentionSpan, cm.mentionSpan, cm.position.elems()[0]);
				}
			}
		}
	}
}
