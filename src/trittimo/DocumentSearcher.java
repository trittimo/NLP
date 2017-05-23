package trittimo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Comparator;

import edu.stanford.nlp.coref.CorefCoreAnnotations;
import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.coref.data.Dictionaries;
import edu.stanford.nlp.coref.data.CorefChain.CorefMention;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.util.CoreMap;

@SuppressWarnings("deprecation")
public class DocumentSearcher {
	
	private static final RelationshipComparator COMPARATOR = new RelationshipComparator();

	public static boolean search(Annotation document, Annotation queryDoc, float threshold) {
		System.out.println("Now searching");
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
		HashMap<Integer, Set<String>> corefMap = getCorefMap(chains);
		
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
	
	private static HashMap<Integer, Set<String>> getCorefMap(List<CorefChain> chains) {
		HashMap<Integer, Set<String>> map = new HashMap<Integer, Set<String>>();
		for (CorefChain chain : chains) {
			HashSet<String> major = new HashSet<String>();
			// Ensure we always have at least one mention in the set
			major.add(chain.getRepresentativeMention().mentionSpan.toLowerCase());
			// Link all mentions
			for (CorefMention ment : chain.getMentionsInTextualOrder()) {
				// Store list of relevant identifiers
				map.put(ment.headIndex, major);
				// Add this mention to the list of relevant identifiers as long as it isn't something generic like a pronominal
				if (ment.mentionType != Dictionaries.MentionType.PRONOMINAL) {
					major.add(ment.mentionSpan.toLowerCase());
				}
				System.out.printf("CorefMap: %s [%s] --> %s%n", ment.toString(), ment.mentionType, major);
			}
		}
		return map;
	}

	private static float compare(SemanticGraph doc, SemanticGraph query, HashMap<Integer, Set<String>> corefMap, HashMap<IndexedWord, Integer> depthMap) {
		float score = 0;
		
		
		System.out.println("--SCORING-SCENTENCE");
		
		// Get edges
		List<SemanticGraphEdge> qEdge = query.edgeListSorted();
		List<SemanticGraphEdge> dEdge = doc.edgeListSorted();
		
		// Resort for our own purposes
		qEdge.sort(COMPARATOR);
		dEdge.sort(COMPARATOR);
		
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
			
			System.out.printf("[[%s]]\t%s\t[[%s]]%n", qe, ((qe.compareTo(de) < 0)?"<":">="), de);

			int depth = depthMap.get(qe.getGovernor());
			float multiplier = 1 / (((float) depth) * depth); // each relationship is 1/n^2 as important, where n is the depth in the tree
			
			// If relevant match
			if (isMatchingRelationship(qe, de)) {
				float val = scoreMatchingRelation(qe, de, corefMap);
				score += (multiplier * val);
				System.out.printf("\t%+.3f * %+.3f = %+.3f (T: %.3f)%n", val, multiplier, val * multiplier, score);
				// Advance D
				d++;
			}
			// If query < document edge (document did not contain this relation)
			else if (COMPARATOR.compare(qe, de) < 0) {
				float val =  scoreMissingRelation(qe);
				score += (multiplier * val);
				System.out.printf("\t%+.3f * %+.3f = %+.3f (T: %.3f)%n", val, multiplier, val * multiplier, score);
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
				score *= 2f;
		
		return score;
	}

	private static boolean equivalentWords(IndexedWord qroot, IndexedWord droot, HashMap<Integer, Set<String>> corefMap) {

		// simple case
		if (qroot.word().equalsIgnoreCase(droot.word()))
			return true;
		
		// check if match
		String lookfor = qroot.word().toLowerCase();
		if (corefMap.containsKey(droot.index())) {
			if (corefMap.get(droot.index()).contains(lookfor)) {
				System.out.printf("Intepreting %s as %s!%n", droot, corefMap.get(droot.index()));
				return true;
			}
			else {
				System.out.printf("'%s' %s is not '%s'!%n", droot, corefMap.get(droot.index()), lookfor);
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

	private static float scoreMatchingRelation(SemanticGraphEdge qe, SemanticGraphEdge de, HashMap<Integer, Set<String>> corefMap) {
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
	
	static class RelationshipComparator implements Comparator<SemanticGraphEdge> {
		
		@Override
		public int compare(SemanticGraphEdge a, SemanticGraphEdge b) {
			int result = a.getRelation().compareTo(b.getRelation());
			if (result != 0)
				return result;
			result = a.getGovernor().compareTo(b.getGovernor());
			if (result != 0)
				return result;
			return a.getDependent().compareTo(b.getDependent());
		}
		
	}
}
