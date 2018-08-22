package annotationProcessor;

import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import afu.org.checkerframework.checker.oigj.qual.Modifier;

import com.google.auto.service.AutoService;

@SupportedAnnotationTypes({ "annotationProcessor.JavaDocProcessor.Param",
		"annotationProcessor.JavaDocProcessor.Param.Params" })
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class JavaDocProcessor extends AbstractProcessor {

	@Repeatable(JavaDocProcessor.Param.Params.class)
	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.SOURCE)
	public @interface Param {
		String name() default "";

		String description() default "";

		@Target(ElementType.METHOD)
		@Retention(RetentionPolicy.SOURCE)
		public @interface Params {
			Param[] value();
		}
	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.SOURCE)
	public @interface Return {
		String description() default "";
	}

	private static class Viewhelper {
		public String className = null;
		public String methodeName = null;
		public String methodeSignature = null;
		public List<String> params = new ArrayList<>();
		public String returns = null;

		public String getParams() {
			return params.stream().collect(Collectors.joining("\n"));

		}

		public String toString() {
			String result = "className: " + className + "\n";
			result = result + "methodeName: " + methodeName + "\n";
			result = result + "methodeSignature: " + methodeSignature + "\n";
			result = result + "params: " + params + "\n";
			result = result + "returns: " + returns + "\n";
			return result;
		}

		public static Viewhelper create(ExecutableElement method) {

			String className = method.getEnclosingElement().getSimpleName()
					.toString();
			String methodeName = method.getSimpleName().toString();
			String modifier = method.getModifiers().stream()
					.map(m -> m.toString()).reduce((a, b) -> a + " " + b)
					.orElse("");
			String returnType = method.getReturnType().toString();

			// methodeSignature
			List<? extends VariableElement> variableElements = method
					.getParameters();
			String parameters = "";
			int i = 0;
			int length = variableElements.size();
			while (i < length) {
				VariableElement variableElement = variableElements.get(i);

				parameters = parameters + variableElement.asType().toString()
						+ " " + variableElement;
				i++;
				if (i < length) {
					parameters = parameters + ",";
				}

			}
			String methodeSignature = modifier + " " + returnType + " "
					+ methodeName + "(" + parameters + ")";

			Viewhelper viewhelper = new Viewhelper();
			viewhelper.className = className;
			viewhelper.methodeName = methodeName;
			viewhelper.methodeSignature = methodeSignature;
			return viewhelper;
		}

		private static String getSimpleName(String type) {
			String name = null;
			try {
				name = Class.forName(type).getSimpleName();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			return name;
		}
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations,
			RoundEnvironment roundEnv) {
		if (annotations.isEmpty())
			return true;
		System.out.println("****" + this.getClass().getSimpleName() + "****");

		Set<? extends Element> set1 = roundEnv
				.getElementsAnnotatedWith(Param.class);

		Set<? extends Element> set2 = roundEnv
				.getElementsAnnotatedWith(Param.Params.class);

		Set<Element> params = new HashSet<Element>(set1);
		params.addAll(set2);

		Map<String, Map<String, Viewhelper>> map = new HashMap<>();

		// Processing @Params
		for (Element param : params) {

			if (param instanceof ExecutableElement) {
				ExecutableElement method = (ExecutableElement) param;

				Viewhelper viewhelper = Viewhelper.create(method);

				// Method @Param Annotation
				Param[] paramAnnotations = param
						.getAnnotationsByType(Param.class);

				for (Param paramAnnotation : paramAnnotations) {
					String parameter = paramAnnotation.name() + "-"
							+ paramAnnotation.description();
					viewhelper.params.add(parameter);
				}

				Map<String, Viewhelper> map2 = map.get(viewhelper.className);
				if (map2 != null) {
					map2.put(viewhelper.methodeSignature, viewhelper);
				} else {
					map2 = new HashMap<String, JavaDocProcessor.Viewhelper>();
					map2.put(viewhelper.methodeSignature, viewhelper);
					map.put(viewhelper.className, map2);
				}
			}

		}

		// Processing @Returns
		Set<? extends Element> returns = roundEnv
				.getElementsAnnotatedWith(Return.class);
		for (Element ret : returns) {
			if (ret instanceof ExecutableElement) {
				ExecutableElement method = (ExecutableElement) ret;
				Viewhelper viewhelperCandidate = Viewhelper.create(method);
				Return annotation = ret.getAnnotation(Return.class);
				viewhelperCandidate.returns = annotation.description();

				Map<String, Viewhelper> map2 = map
						.get(viewhelperCandidate.className);

				if (map2 == null) {
					map2 = new HashMap<String, JavaDocProcessor.Viewhelper>();
					map2.put(viewhelperCandidate.methodeSignature,
							viewhelperCandidate);
					map.put(viewhelperCandidate.className, map2);
				} else {
					Viewhelper viewhelper = map2.putIfAbsent(
							viewhelperCandidate.methodeSignature,
							viewhelperCandidate);
					if (viewhelper != null) {
						viewhelper.returns = viewhelperCandidate.returns;
					}
				}

			}
		}

		// generate HTML:
		// ./workspace/coreJava/target/generated-sources/annotations/annotationProcessor/JavaDocProcessor.html
		String pkg = this.getClass().getPackage().getName();
		String relativeName = this.getClass().getSimpleName() + ".html";
		try {
			FileObject fileObject = processingEnv.getFiler().createResource(
					StandardLocation.SOURCE_OUTPUT, pkg, relativeName);
			try (Writer writer = fileObject.openWriter();) {
				String newLine = "\n";
				writer.write("<!DOCTYPE html>" + newLine);
				writer.write("<html>" + newLine);
				writer.write("<head>" + newLine);
				writer.write("<title>Java Doc</title>" + newLine);
				writer.write("</head>" + newLine);
				writer.write("<body>" + newLine);
				generateHtml(writer, map);
				writer.write("</body>" + newLine);
				writer.write("</html>" + newLine);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return true;
	}

	private void generateHtml(Writer writer,
			Map<String, Map<String, Viewhelper>> map) throws IOException {
		Set<Entry<String, Map<String, Viewhelper>>> entries = map.entrySet();
		String newLine = "\n";
		for (Entry<String, Map<String, Viewhelper>> entry : entries) {
			String className = entry.getKey();
			writer.write("<h1>" + className + "</h1>");
			Set<Entry<String, Viewhelper>> viEntries = entry.getValue()
					.entrySet();
			for (Entry<String, Viewhelper> viEntry : viEntries) {

				Viewhelper viewhelper = viEntry.getValue();
				List<String> params = viewhelper.params;

				writer.write("<h2>" + viewhelper.methodeName + "</h2>"
						+ newLine);
				writer.write("<h3>" + viewhelper.methodeSignature + "</h3>"
						+ newLine);
				writer.write("<p><b>Parameters:</b></p>" + newLine);
				for (String param : params) {
					writer.write("<p>" + param + "</p>" + newLine);
				}

				writer.write("<p><b>Returns:</b></p>");
				writer.write("<p>" + viewhelper.returns + "</p>" + newLine);
			}
		}

	}

}
