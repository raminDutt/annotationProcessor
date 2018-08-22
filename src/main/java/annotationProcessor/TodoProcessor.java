package annotationProcessor;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import annotations.Todo;

import com.google.auto.service.AutoService;

@SupportedAnnotationTypes({
	"annotationProcessor.Todo",
	"annotationProcessor.Todo.Todos" })
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class TodoProcessor extends AbstractProcessor {

    public static int count = 0;

    @Override
    public boolean process(Set<? extends TypeElement> annotations,
	    RoundEnvironment roundEnv) {

	System.out.println("****" + this.getClass().getCanonicalName()
		+ "*******");
	if (annotations.isEmpty())
	    return true;

	// ch11Q5(roundEnv);
	ch11Q6A(roundEnv);
	// ch11Q6B(annotations, roundEnv);

	return true;
    }

    private void ch11Q6B(Set<? extends TypeElement> annotations,
	    RoundEnvironment roundEnv) {
	Set<? extends Element> rootElemets = roundEnv.getRootElements();
	// Set<? extends Element> annotatedElement = roundEnv
	// .getElementsAnnotatedWith(Todo.class);
	Set<Todo> todos = rootElemets
		.stream()
		.flatMap(
			e -> {
			    Stream<Todo> annotatedRootElement = Arrays.stream(e
				    .getAnnotationsByType(Todo.class));
			    List<? extends Element> enclosedElements = e
				    .getEnclosedElements();
			    Stream<Todo> enclosedAnnotatedElements = enclosedElements
				    .stream()
				    .flatMap(
					    enclosedElement -> Arrays.stream(enclosedElement
						    .getAnnotationsByType(Todo.class)));
			    Stream<Todo> annotatedElement = Stream.concat(
				    annotatedRootElement,
				    enclosedAnnotatedElements);
			    return annotatedElement;
			}).collect(Collectors.toSet());

	try (Writer writer = processingEnv
		.getFiler()
		.createResource(StandardLocation.SOURCE_OUTPUT,
			"annotationProcessor",
			this.getClass().getSimpleName() + ".txt").openWriter();) {
	    for (Todo todo : todos) {
		String str = "Message: " + todo.message() + "\nDescription: "
			+ todo.description() + "\n\n";
		writer.write(str);
	    }
	} catch (IOException e1) {
	    e1.printStackTrace();
	}

    }

    private void ch11Q6A(RoundEnvironment roundEnv) {
	FileObject fileObject = null;
	try {
	    fileObject = processingEnv.getFiler().createResource(
		    StandardLocation.SOURCE_OUTPUT, "annotationProcessor",
		    this.getClass().getSimpleName() + ".txt");
	    Set<? extends Element> set1 = roundEnv
		    .getElementsAnnotatedWith(Todo.class);
	    Set<? extends Element> set2 = roundEnv
		    .getElementsAnnotatedWith(Todo.Todos.class);
	    Set<Element> annotatedElements = new HashSet<Element>(set1);
	    annotatedElements.addAll(set2);
	    try (Writer writer = fileObject.openWriter()) {
		for (Element annotatedElement : annotatedElements) {

		    Todo[] todos = annotatedElement
			    .getAnnotationsByType(Todo.class);
		    for (Todo todo : todos) {
			writer.write(annotatedElement + "==> " + todo.message()
				+ "\n");
		    }
		}
	    }

	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    private void ch11Q5(RoundEnvironment roundEnv) {
	FileObject fileObject;
	try {
	    fileObject = processingEnv.getFiler().createResource(
		    StandardLocation.SOURCE_OUTPUT, "annotationProcessor",
		    this.getClass().getSimpleName() + ".txt");

	    try (Writer writer = fileObject.openWriter();) {

		System.out.println("fileObject: " + fileObject.getName());
		Set<? extends Element> elements = roundEnv
			.getElementsAnnotatedWith(Todo.class);
		for (Element element : elements) {
		    if (element instanceof ExecutableElement) {
			ExecutableElement methode = (ExecutableElement) element;
			Todo annotation = methode.getAnnotation(Todo.class);
			writer.write(annotation.message() + "\n");
		    }
		}

	    }

	} catch (IOException e) {
	    e.printStackTrace();
	}
    }
}
