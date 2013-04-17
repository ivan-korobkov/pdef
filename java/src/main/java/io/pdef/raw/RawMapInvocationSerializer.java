package io.pdef.raw;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.pdef.descriptors.DescriptorPool;
import io.pdef.invocation.Invocation;
import io.pdef.invocation.InvocationSerializer;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class RawMapInvocationSerializer implements InvocationSerializer {
	private final RawSerializer serializer;

	public RawMapInvocationSerializer(final DescriptorPool pool) {
		this.serializer = new RawSerializer(pool);
	}

	@Override
	public Map<String, List<Object>> serializeInvocations(final List<Invocation> invocations) {
		checkNotNull(invocations);

		ImmutableMap.Builder<String, List<Object>> builder = ImmutableMap.builder();
		for (Invocation invocation : invocations) {
			String name = invocation.getMethod().getName();

			ImmutableList.Builder<Object> args = ImmutableList.builder();
			for (Object arg : invocation.getArgs()) {
				Object rawArg = serializer.serialize(arg);
				args.add(rawArg);
			}
			builder.put(name, args.build());
		}

		return builder.build();
	}
}
