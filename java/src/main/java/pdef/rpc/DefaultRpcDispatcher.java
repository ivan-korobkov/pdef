package pdef.rpc;

import static com.google.common.base.Preconditions.*;
import pdef.MethodDescriptor;

import java.util.List;
import java.util.Map;

public class DefaultRpcDispatcher implements RpcDispatcher {

	@Override
	public Object dispatch(final List<Call> calls, final Object service) {
		checkNotNull(service);
		checkNotNull(calls);

		Object object = service;
		for (Call call : calls) {
			MethodDescriptor method = call.getMethod();
			@SuppressWarnings("unchecked")
			Map<String, Object> args = (Map<String, Object>) call.getArgs();
			object = method.call(object, args);
		}

		return object;
	}
}
