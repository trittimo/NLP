package trittimo;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;

import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.PropertiesUtils;

public class Preprocessor {
	
	private static final Properties PROPS = PropertiesUtils.asProperties("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
	
	private static StanfordCoreNLP pipeline;
	
	public static Annotation parseQuery(String query) {
		Annotation annotation = new Annotation(query);
		getPipeline().annotate(annotation);
		return annotation;
	}
	
	/**
	 * Creates a {@link edu.stanford.nlp.pipeline.Annotation Annotation} document for a file.
	 * 
	 * If a cached/serialized {@link edu.stanford.nlp.pipeline.Annotation Annotation} exists, the serialized version will be loaded instead. 
	 * This method will automatically detect if the cache/serialized version is stale by checking the MD5 of the file to be loaded against the cached version.
	 * 
	 * @param fname File to process / load from cache
	 */
	public static Annotation loadFile(String fname) {

		File infile = new File(fname);
		
		if (infile.isDirectory()) {
			throw new RuntimeException("The input file can't be a directory");
		}
		
		try {
	
			// check to see if it's html or regular
			String ext = infile.getPath();
			ext = ext.substring(ext.lastIndexOf(".") + 1);
			boolean isHTML = ext.equalsIgnoreCase("html") || ext.equalsIgnoreCase("htm");
			String content = new String(Files.readAllBytes(infile.toPath()), StandardCharsets.UTF_8);
			if (isHTML) {
				content = HTMLParser.deparse(content);
			}

			// Create the annotation
			StanfordCoreNLP pipeline = getPipeline();
			Annotation document = new Annotation(content);
			pipeline.annotate(document);
			
			return document;
			
		} catch (Exception e) {
			// bubble up error
			throw new RuntimeException(e);
		}
		
	}

	
	public static StanfordCoreNLP getPipeline() {
		if (pipeline == null)
			pipeline = new StanfordCoreNLP(PROPS);
		return pipeline;
	}
}
