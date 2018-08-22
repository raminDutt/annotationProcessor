package annotationProcessor;

import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import com.google.auto.service.AutoService;

//Disabled it for now (You want to enable this processor just uncomment @SupportedAnnotationTypes)
//@SupportedAnnotationTypes({ "annotationProcessor.TestCaseProcessor.TestCase", 
//		"annotationProcessor.TestCaseProcessor.TestCase.TestCases" })
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class TestCaseProcessor extends AbstractProcessor {

	@Repeatable(TestCase.TestCases.class)
	@Retention(RetentionPolicy.SOURCE)
	@Target(ElementType.METHOD)
	public @interface TestCase {
		String params();

		String expected();

		@Retention(RetentionPolicy.SOURCE)
		@Target(ElementType.METHOD)
		@interface TestCases {
			TestCase[] value();
		}
	}

	public static class Viewhelper {
		String className = null;
		String methodName = null;
		String packageName = null;
		String methodSignature = null;
		List<String> assertStatements = new ArrayList<String>();

		public static Viewhelper create(ExecutableElement method) {

			// methodName
			String methodeName = method.getSimpleName().toString();

			// ClassName
			TypeElement classElement = (TypeElement) method
					.getEnclosingElement();
			String className = classElement.getSimpleName().toString();

			// Package
			PackageElement packageElement = (PackageElement) classElement
					.getEnclosingElement();
			String packageName = packageElement.getSimpleName().toString();

			// Modifier
			String modifier = method.getModifiers().stream()
					.map(m -> m.toString()).reduce((a, b) -> a + " " + b)
					.orElse("");

			// Return
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

			// Creating an Instance of ViewHelper
			Viewhelper viewhelper = new Viewhelper();
			viewhelper.className = className;
			viewhelper.methodName = methodeName;
			viewhelper.methodSignature = methodeSignature;
			viewhelper.packageName = packageName;
			return viewhelper;
		}
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations,
			RoundEnvironment roundEnv) {
		if (annotations.isEmpty())
			return true;

		System.out.println("*****" + this.getClass().getSimpleName() + "*****");
		Set<? extends Element> set1 = roundEnv
				.getElementsAnnotatedWith(TestCase.class);
		Set<? extends Element> set2 = roundEnv
				.getElementsAnnotatedWith(TestCase.TestCases.class);
		Set<Element> annotatedElements = new HashSet<Element>(set1);
		annotatedElements.addAll(set2);

		Map<String, Map<String, Viewhelper>> viewMap = new HashMap<>();

		for (Element annotatedElement : annotatedElements) {
			if (annotatedElement instanceof ExecutableElement) {

				// Get methodeName
				ExecutableElement executableElement = (ExecutableElement) annotatedElement;
				Viewhelper viewHelper = Viewhelper.create(executableElement);

				// Get Annotations metadata just for fun
				List<? extends AnnotationMirror> annotationMirrors = executableElement
						.getAnnotationMirrors();
				
				for(AnnotationMirror annotationMirror : annotationMirrors)
				{
					System.out.println(annotationMirror.getAnnotationType());
					Map<? extends ExecutableElement, ? extends AnnotationValue> map = annotationMirror.getElementValues();
					Set<? extends Entry<? extends ExecutableElement, ? extends AnnotationValue>> entries = map.entrySet();
					for(Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : entries)
					{
						entry.getKey();
					}
			
					
				}
				
				// Get Annotations  
				TestCase[] testCases = executableElement
						.getAnnotationsByType(TestCase.class);

				for (TestCase testCase : testCases) {
					String assertStatement = viewHelper.className + "."
							+ viewHelper.methodName + "(" + testCase.params()
							+ ") == " + testCase.expected();
					viewHelper.assertStatements.add(assertStatement);
				}

				Map<String, Viewhelper> viewHelpers = viewMap
						.get(viewHelper.className);
				if (viewHelpers == null) {
					viewHelpers = new HashMap<>();
				}
				viewHelpers.put(viewHelper.methodSignature, viewHelper);
				viewMap.put(viewHelper.className, viewHelpers);
			}

			if (annotatedElement instanceof VariableElement) {
				// Getting value of a final Field
				VariableElement element = (VariableElement) annotatedElement;
				System.out.println(element.getConstantValue());
			}
		}

		privateCreatetestcase(viewMap);
		return true;
	}

	private void privateCreatetestcase(Map<String, Map<String, Viewhelper>> map) {

		Set<Entry<String, Map<String, Viewhelper>>> entries = map.entrySet();

		for (Entry<String, Map<String, Viewhelper>> classes : entries) {
			String className = classes.getKey();
			String packageName = classes.getValue().entrySet().stream()
					.filter(e -> e.getValue().packageName != null).findAny()
					.get().getValue().packageName;

			String testClassName = className + "Test";
			String newLine = "\n";
			String importStatement1 = "import org.junit.Test;";
			String importStatement2 = "import static org.junit.Assert.*;";
			String importStatement3 = "import " + packageName + "." + className
					+ ";";

			try (Writer writer = processingEnv
					.getFiler()
					.createSourceFile(
							this.getClass().getPackage().getName() + "."
									+ testClassName).openWriter();) {
				writer.write(importStatement1 + newLine);
				writer.write(importStatement2 + newLine);
				writer.write(importStatement3 + newLine);
				writer.write("public class " + testClassName + "{" + newLine);
				writer.write("@Test" + newLine);

				// Writing method
				Map<String, Viewhelper> clazz = classes.getValue();
				Set<Entry<String, Viewhelper>> methods = clazz.entrySet();
				for (Entry<String, Viewhelper> method : methods) {

					Viewhelper viewhelper = method.getValue();
					String testMethodName = "test" + viewhelper.methodName;

					List<String> assertStatements = viewhelper.assertStatements;

					writer.write("public void " + testMethodName + "() {"
							+ newLine);

					for (String assertStatement : assertStatements) {
						if (!assertStatement.isEmpty()) {
							writer.write("assert(" + assertStatement + ");"
									+ newLine);
						}
					}
					writer.write("\t}" + newLine);
					
				}
				writer.write("}" + newLine);

			} catch (IOException e) {
				e.printStackTrace();
			}

		}

	}

}
