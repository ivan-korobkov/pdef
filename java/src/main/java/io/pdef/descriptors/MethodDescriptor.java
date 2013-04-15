package io.pdef.descriptors;

import com.google.common.collect.ImmutableList;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class MethodDescriptor {
	private final Method method;
	private final String name;
	private final Descriptor result;
	private final List<Descriptor> args;

	public MethodDescriptor(final Method method, final DescriptorPool pool) {
		this.method = checkNotNull(method);
		name = method.getName();
		result = pool.getDescriptor(method.getGenericReturnType());

		ImmutableList.Builder<Descriptor> builder = ImmutableList.builder();
		for (Type type : method.getGenericParameterTypes()) {
			Descriptor arg = pool.getDescriptor(type);
			builder.add(arg);
		}
		args = builder.build();
	}

	public Method getMethod() {
		return method;
	}

	public String getName() {
		return name;
	}

	public Descriptor getResult() {
		return result;
	}

	public List<Descriptor> getArgs() {
		return args;
	}
}
