package annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Repeatable(TestCaseEnigma.EningmaTestCases.class)
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TestCaseEnigma {

	String[] params();
	String expected();
	boolean enabled() default true;
	String name();
	
	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface EningmaTestCases
	{
		TestCaseEnigma[] value();
	}
}


