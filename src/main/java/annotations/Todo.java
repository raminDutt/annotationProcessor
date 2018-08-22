package annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Repeatable(Todo.Todos.class)
//@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface Todo {
	public String message();
	public String description() default "";

	@Retention(RetentionPolicy.SOURCE)
	//@Target(ElementType.METHOD)
	public @interface Todos {
		Todo[] value();
	}

}
