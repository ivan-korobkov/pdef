package pdef.rpc;

import com.google.common.annotations.VisibleForTesting;
import static com.google.common.base.Preconditions.*;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import pdef.InterfaceDescriptor;
import pdef.MethodDescriptor;
import pdef.TypeDescriptor;
import pdef.formats.Parser;

import java.util.List;
import java.util.Map;

public class RawDispatcher implements Dispatcher {
	private final Parser parser;

	public RawDispatcher(final Parser parser) {
		this.parser = parser;
	}

	@Override
	public Object dispatch(final InterfaceDescriptor descriptor, final Object service,
			final Map<String, Object> request) {
		checkNotNull(descriptor);
		checkNotNull(service);
		checkNotNull(request);

		List<Call> rawCalls = parseMethodNames(descriptor, request);
		List<Call> calls = parseMethodArgs(rawCalls);
		return dispatch(service, calls);
	}

	@VisibleForTesting
	List<Call> parseMethodNames(final InterfaceDescriptor descriptor,
			final Map<String, Object> request) {
		List<Call> calls = Lists.newArrayList();

		InterfaceDescriptor d = descriptor;
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, Object> entry : request.entrySet()) {
			String methodName = entry.getKey();
			if (sb.length() > 0) sb.append(".");
			sb.append(methodName);

			MethodDescriptor method = d.getMethods().map().get(methodName);
			if (method == null) throw new DispatcherException("Method not found " + sb.toString());

			calls.add(new Call(method, (Map<?, ?>) entry.getValue()));
			TypeDescriptor result = method.getResult();
			if (result instanceof InterfaceDescriptor) {
				d = (InterfaceDescriptor) result;
			} else {
				break;
			}
		}

		return calls;
	}

	@VisibleForTesting
	List<Call> parseMethodArgs(final List<Call> rawCalls) {
		List<Call> calls = Lists.newArrayListWithCapacity(rawCalls.size());

		for (Call rawCall : rawCalls) {
			MethodDescriptor method = rawCall.method;
			Object rawArgs = rawCall.args;

			Map<String, Object> args = parseArgs(method.getArgs(), rawArgs);
			calls.add(new Call(method, args));
		}

		return calls;
	}

	@VisibleForTesting
	Map<String, Object> parseArgs(final Map<String, TypeDescriptor> descriptors,
			final Object rawArgs) {
		Map<?, ?> argMap = (Map<?, ?>) rawArgs;

		Map<String, Object> args = Maps.newLinkedHashMap();
		for (Map.Entry<String, TypeDescriptor> entry : descriptors.entrySet()) {
			String name = entry.getKey();
			TypeDescriptor descriptor = entry.getValue();
			Object rawValue = argMap.get(name);
			Object value = parser.parse(descriptor, rawValue);
			args.put(name, value);
		}

		return args;
	}

	@VisibleForTesting
	Object dispatch(final Object service, final List<Call> calls) {
		checkNotNull(service);
		checkNotNull(calls);

		Object object = service;
		for (Call call : calls) {
			MethodDescriptor method = call.method;
			@SuppressWarnings("unchecked")
			Map<String, Object> args = (Map<String, Object>) call.args;
			object = method.call(object, args);
		}

		return object;
	}

	private static class Call {
		private final MethodDescriptor method;
		private final Map<?, ?> args;

		private Call(final MethodDescriptor method, final Map<?, ?> args) {
			this.method = method;
			this.args = args;
		}
	}
}
