package trittimo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import edu.stanford.nlp.coref.CorefCoreAnnotations;
import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.coref.data.CorefChain.CorefMention;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.util.CoreMap;

@SuppressWarnings("deprecation")
public class Test {
	
	public static void main(String[] args) throws Exception {

		Annotation document = new Annotation("Barack Obama was born in Hawaii. He was the president of the United States. Obama was elected in 2008. Obama fought ISIS.");
		Annotation query = new Annotation("Obama isn't fighting ISIS in Syria");

		System.out.println("---GETPIPELINE");
		StanfordCoreNLP pipeline = Preprocessor.getPipeline();
		System.out.println("---ANNOTATE DOC");
		pipeline.annotate(document);
		System.out.println("---ANNOTATE QUERY");
		pipeline.annotate(query);
		System.out.println("---SEARCH");
		search(document, query, 0.5f);
		
	}	
	
	public static boolean search(Annotation document, Annotation queryDoc, float threshold) {
		
		CoreMap queryScentence = queryDoc
			.get(SentencesAnnotation.class)
			.get(0);
		
		// create graph of query
		SemanticGraph query = queryScentence
			.get(CollapsedCCProcessedDependenciesAnnotation.class);

		HashMap<IndexedWord, Integer> depthMap = getDepthMap(query);
		
		List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
		List<CorefChain> chains = new ArrayList<CorefChain>();
		chains.addAll(document.get(CorefCoreAnnotations.CorefChainAnnotation.class).values());
		HashMap<Integer, String> corefMap = getCorefMap(chains);
		
		float[] scores = new float[sentences.size()];
		int index = 0;
		for (CoreMap docSentence : sentences) {	
			System.out.println("--NEW-SCENTENCE");
			System.out.println(docSentence);
			// Graph the sentence
			SemanticGraph graph = docSentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
			// Rate the "proof" the sentence provides
			scores[index++] = compare(graph, query, corefMap, depthMap);
		}
		System.out.println("Query:\n"+query);
		System.out.println("Scores:");
		int best = 0;
		for (int i = 0; i < scores.length; i++) {
			System.out.printf("[%+.3f] %s%n", scores[i], sentences.get(i));
			System.out.println(sentences.get(i).get(CollapsedCCProcessedDependenciesAnnotation.class));
			if (scores[i] > scores[best]) {
				best = i;
			}
		}
		System.out.printf("Most Positive Proof @ #%d: %f%n\t%s%n", best, scores[best],  sentences.get(best));
		
		System.out.println("Using Threshold: " + threshold + "\n\tSearch Result: " + (scores[best] > threshold));
		return (scores[best] > threshold);
	}
	
	private static HashMap<Integer, String> getCorefMap(List<CorefChain> chains) {
		HashMap<Integer, String> map = new HashMap<Integer, String>();
		for (CorefChain chain : chains) {
			String major = chain.getRepresentativeMention().mentionSpan;
			for (CorefMention ment : chain.getMentionsInTextualOrder()) {
				map.put(ment.headIndex, major);
				System.out.printf("CorefMap: (%s) %d --> %s%n", ment.toString(), ment.headIndex, major);
			}
		}
		return map;
	}

	private static float compare(SemanticGraph doc, SemanticGraph query, HashMap<Integer, String> corefMap, HashMap<IndexedWord, Integer> depthMap) {
		float score = 0;
		
		System.out.println("--SCORING-SCENTENCE");
		
		// Get edges
		List<SemanticGraphEdge> qEdge = query.edgeListSorted();
		List<SemanticGraphEdge> dEdge = doc.edgeListSorted();
		
		// if either has nothing in it, just ignore it. 
		if (qEdge.isEmpty() || dEdge.isEmpty())
			return score;
		
		int q = 0,
			d = 0;
		
		SemanticGraphEdge qe, de;
		
		
		// Attempt to compare matching elements of q and d
		while ((q < qEdge.size()) && (d < dEdge.size())) {
			qe = qEdge.get(q);
			de = dEdge.get(d);

			int depth = depthMap.get(qe.getGovernor());
			float multiplier = 1 / (((float) depth) * depth); // each relationship is 1/n^2 as important, where n is the depth in the tree
			
			// If relevant match
			if (isMatchingRelationship(qe, de)) {
				float val = scoreMatchingRelation(qe, de, corefMap);
				score += multiplier * val;
				System.out.printf("\t%+.3f * %+.3f = %+.3f%n", val, multiplier, val * multiplier);
				// Advance both
				q++; d++;
			}
			// If query < document edge (document did not contain this relation)
			else if (qe.compareTo(de) < 0) {
				score += multiplier * scoreMissingRelation(qe);
				q++;
			}
			// document < query edge (query did not contain this relation)
			else {
				// just ignore it, and move along
				d++;
			}
		}
		
		// If document ended before query did, subtract points for each non match 
		while (q < qEdge.size()) {
			qe = qEdge.get(q++);
			int depth = depthMap.get(qe.getGovernor());
			int multiplier = 1 / (depth * depth); // each relationship is 1/n^2 as important, where n is the depth in the tree
			score += multiplier * scoreMissingRelation(qe);
		}

		// Multiply by 2 if the root (predicate) matches && score is positive
		if (equivalentWords(query.getFirstRoot(), doc.getFirstRoot(), corefMap))
			if (score > 0)
				score *= 0.5f;
		
		return score;
	}

	private static boolean equivalentWords(IndexedWord qroot, IndexedWord droot, HashMap<Integer, String> corefMap) {

		// simple case
		if (qroot.toString().equalsIgnoreCase(droot.toString()))
			return true;
		
		// check if match
		if (corefMap.containsKey(droot.index())) {
			if (qroot.toString().equalsIgnoreCase(corefMap.get(droot.index()))) {
				System.out.printf("Intepreting %s as %s!%n", droot, corefMap.get(droot.index()));
				return true;
			}
		}

		// not a match
		return false;
	}

	private static boolean isMatchingRelationship(SemanticGraphEdge qe, SemanticGraphEdge de) {
		String qer = qe.getRelation().toString();
		String der = de.getRelation().toString();
		// return true if same
		if (qer.equals(der))
			return true;
		// if both are subject, we don't care whether it's passive or not
		else if (qer.startsWith("nsubj") && der.startsWith("nsubj"))
			return true;
		return false;
	}

	private static float scoreMatchingRelation(SemanticGraphEdge qe, SemanticGraphEdge de, HashMap<Integer, String> corefMap) {
		System.out.printf("MATCHING: (%s / %s) --[%s]-> (%s / %s)%n", qe.getGovernor(), de.getGovernor(), qe.getRelation(), qe.getDependent(), de.getDependent());
		boolean match = equivalentWords(qe.getGovernor(), de.getGovernor(), corefMap)
				&& equivalentWords(qe.getDependent(), de.getDependent(), corefMap);
		
		if (qe.getRelation().toString().startsWith("nsubj")) {
			if (match)
				return 0.4f;
			else
				return -0.1f;
		}
		if (qe.getRelation().toString().equals("dobj")) {
			if (match)
				return 0.4f;
			else
				return -0.1f;
		}
		if (qe.getRelation().toString().equals("punct"))
			return 0;
		return 0.05f;
	}

	private static float scoreMissingRelation(SemanticGraphEdge qe) {
		System.out.printf("MISSING: %s --[%s]-> %s%n", qe.getGovernor(), qe.getRelation(), qe.getDependent());
		// if negation isn't present
		if (qe.getRelation().toString().equals("neg")) {
			return -1f;
		}
		return 0f;
	}
	

	/**
	 * Finds the depths of all words in the query SemanticMap 
	 * @param query
	 * @return
	 */
	private static HashMap<IndexedWord, Integer> getDepthMap(SemanticGraph query) {
		HashMap<IndexedWord, Integer> depth = new HashMap<IndexedWord, Integer>();
		depthMapHelper(query, depth, query.getFirstRoot(), 1);
		return depth;
	}

	private static void depthMapHelper(SemanticGraph query, HashMap<IndexedWord, Integer> depth, IndexedWord w, int i) {
		System.out.println("Adding: " + w);
		depth.put(w, i);
		for (IndexedWord d : query.descendants(w)) {
			if (d.equals(w) == false) {
				System.out.printf("%s descends from %s%n", d.toString(), w.toString());
				depthMapHelper(query, depth, d, i + 1);
			}
		}
	}

}
