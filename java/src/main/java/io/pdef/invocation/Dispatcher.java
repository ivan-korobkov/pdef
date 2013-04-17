package io.pdef.invocation;

import com.google.common.collect.ImmutableList;
import io.pdef.descriptors.MethodDescriptor;

import java.util.Iterator;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class Dispatcher {
	public Object dispatch(final Object service, final List<Invocation> invocations) {
		checkNotNull(service);
		checkNotNull(invocations);

		Object object = service;
		Iterator<Invocation> iterator = invocations.iterator();
		while (iterator.hasNext()) {
			if (object instanceof Dispatchable) {
				return ((Dispatchable) object).dispatch(ImmutableList.copyOf(iterator));
			}

			Invocation invocation = iterator.next();
			MethodDescriptor method = invocation.getMethod();
			List<?> args = invocation.getArgs();
			object = method.invoke(object, args);
		}

		return object;
	}
}
