package io.pdef.raw;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.pdef.SerializationException;
import io.pdef.descriptors.*;
import io.pdef.invocation.Invocation;
import io.pdef.invocation.InvocationParser;

import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class RawMapInvocationParser implements InvocationParser {
	private final RawParser parser;
	private final DescriptorPool pool;

	public RawMapInvocationParser(final DescriptorPool pool) {
		this.pool = pool;
		this.parser = new RawParser(pool);
	}

	@Override
	public List<Invocation> parse(final Class<?> interfaceClass, final Object object) {
		checkNotNull(interfaceClass);
		checkNotNull(object);

		Map<?, ?> map = (Map<?, ?>) object;
		InterfaceDescriptor descriptor = (InterfaceDescriptor) pool.getDescriptor(interfaceClass);
		return parseMethodNames(descriptor, map);
	}

	@SuppressWarnings("unchecked")
	private List<Invocation> parseMethodNames(final InterfaceDescriptor descriptor,
			final Map<?, ?> map) {
		List<Invocation> Invocations = Lists.newArrayList();

		InterfaceDescriptor d = descriptor;
		StringBuilder path = new StringBuilder();
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			Object methodName = entry.getKey();
			if (path.length() > 0) path.append(".");
			path.append(methodName);

			MethodDescriptor method = d.getMethods().get(methodName);
			if (method == null) {
				throw new SerializationException(path.toString() + ": method not found ");
			}

			List rawArgs = (List) entry.getValue();
			List<?> args = parseArgs(method, rawArgs, path);
			Invocation Invocation = new Invocation(method, args);
			Invocations.add(Invocation);

			Descriptor result = method.getResult();
			if (result instanceof InterfaceDescriptor) {
				d = (InterfaceDescriptor) result;
			} else {
				break;
			}
		}

		return Invocations;
	}

	private List<?> parseArgs(final MethodDescriptor method, final List<Object> rawArgs,
			final StringBuilder path) {
		List<Type> argDescriptors = method.getArgTypes();
		if (rawArgs.size() != argDescriptors.size()) {
			throw new SerializationException(path.toString() + ": wrong number of arguments, " 
					+ argDescriptors.size() + " expected");
		}

		ImmutableList.Builder<Object> builder = ImmutableList.builder();
		Iterator<Type> iterator0 = argDescriptors.iterator();
		Iterator<Object> iterator1 = rawArgs.iterator();
		while (iterator0.hasNext()) {
			Type argType = iterator0.next();
			Object rawArg = iterator1.next();
			Object arg = parser.parse(argType, rawArg);
			builder.add(arg);
		}
		
		return builder.build();
	}
}
