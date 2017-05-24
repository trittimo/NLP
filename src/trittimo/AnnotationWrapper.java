package trittimo;

import edu.stanford.nlp.pipeline.Annotation;

public class AnnotationWrapper {
	public Annotation annotation;
	public boolean annotated = false;
	public AnnotationWrapper(Annotation annotation) {
		this.annotation = annotation;
	}
}
