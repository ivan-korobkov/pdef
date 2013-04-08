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

public class RawRpcParser implements RpcParser {
	private final Parser parser;

	public RawRpcParser(final Parser parser) {
		this.parser = parser;
	}

	@Override
	public List<Call> parse(final InterfaceDescriptor descriptor, final Object object) {
		checkNotNull(descriptor);
		checkNotNull(object);

		Map<?, ?> map = (Map<?, ?>) object;
		List <Call> rawCalls = parseMethodNames(descriptor, map);
		return parseMethodArgs(rawCalls);
	}

	@VisibleForTesting
	List<Call> parseMethodNames(final InterfaceDescriptor descriptor, final Map<?, ?> map) {
		List<Call> calls = Lists.newArrayList();

		InterfaceDescriptor d = descriptor;
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			Object methodName = entry.getKey();
			if (sb.length() > 0) sb.append(".");
			sb.append(methodName);

			MethodDescriptor method = d.getMethods().map().get(methodName);
			if (method == null) {
				throw new DispatcherException("Method not found " + sb.toString());
			}

			Map<?, ?> args = (Map<?, ?>) entry.getValue();
			Call call = new Call(method, args);
			calls.add(call);
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
			MethodDescriptor method = rawCall.getMethod();
			Object rawArgs = rawCall.getArgs();

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
}
