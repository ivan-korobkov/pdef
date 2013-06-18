package io.pdef.invocation;

import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public interface Invocation {
	/** Returns this invocation method name. */
	String getMethod();

	/** Returns a map of arguments. */
	Map<String, Object> getArgs();

	/** Returns the result invocation exception if present. */
	@Nullable
	Type getExcType();

	/** Returns the parent invocation if present. */
	@Nullable
	Invocation getParent();

	/** Returns this invocation chain as a list for the first parent to this invocation. */
	List<Invocation> toList();
}
