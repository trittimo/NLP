package trittimo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;

public class NLP {
	public static void main(String[] args) throws IOException {
		if (args.length < 2) {
			System.err.println("Usgae: NLP <input folder> <search phrases...>");
			System.err.println("Example: NLP data/presidents \"Lincoln birthday\" \"George Washington Age\"");
			return;
		}
		File infolder = new File(args[0]);
		
		if (!infolder.isDirectory()) {
			System.err.println("The input folder must exist and be a directory");
			return;
		}
		
		List<Tuple<String, Annotation>> raw = new ArrayList<>();
		for (File f : infolder.listFiles()) {
			raw.add(new Tuple<>(f.getName(), Preprocessor.loadFile(f.toPath().toString())));
		}
		
		List<Annotation> queries = new ArrayList<>();
		for (int i = 1; i < args.length; i++) {
			queries.add(Preprocessor.parseQuery(args[i]));
		}
		
		// subject/verb match
		// or subject/object match
		// subject must always match
		
		for (Tuple<String, Annotation> datum : raw) {
			List<CoreMap> sentences = datum.second.get(SentencesAnnotation.class);
//			System.out.println(sentences.get(1).get(TreeAnnotation.class));
			for (CoreMap sentence : datum.second.get(SentencesAnnotation.class)) {
				Tree tree = sentence.get(TreeAnnotation.class);
				System.out.println(tree);
			}
		}
	}
	

	public static void mute() {
		
	}
	
	public static void unmute() {
		
	}
}
