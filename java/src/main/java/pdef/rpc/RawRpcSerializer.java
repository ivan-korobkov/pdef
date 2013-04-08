package pdef.rpc;

import com.google.common.collect.ImmutableMap;
import pdef.MethodDescriptor;
import pdef.TypeDescriptor;
import pdef.formats.RawSerializer;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class RawRpcSerializer implements RpcSerializer {
	private final RawSerializer serializer;

	public RawRpcSerializer(final RawSerializer serializer) {
		this.serializer = serializer;
	}

	@Override
	public Map<String, Object> serialize(final List<Call> calls) {
		checkNotNull(calls);
		ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();

		for (Call call : calls) {
			MethodDescriptor method = call.getMethod();
			Map<String, Object> args = serializeArgs(method, call.getArgs());
			builder.put(method.getName(), args);
		}

		return builder.build();
	}

	private Map<String, Object> serializeArgs(final MethodDescriptor method,
			final Map<?, ?> args) {
		ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
		for (Map.Entry<String, TypeDescriptor> entry : method.getArgs().entrySet()) {
			String name = entry.getKey();
			TypeDescriptor type = entry.getValue();
			Object arg = args.get(name);

			Object rawArg = serializer.serialize(type, arg);
			builder.put(name, rawArg);
		}
		return builder.build();
	}
}
