package io.pdef;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.pdef.rpc.MethodCall;
import io.pdef.rpc.RpcException;
import io.pdef.rpc.RpcExceptionCode;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/** Pdef interface descriptor. */
public class PdefInterface extends PdefDescriptor {
	private final Set<PdefInterface> bases;
	private final Map<String, PdefMethod> methods;
	private final Map<String, PdefMethod> declaredMethods;

	PdefInterface(final Class<?> cls, final Pdef pdef) {
		super(PdefType.INTERFACE, cls, pdef);

		bases = buildBases(cls, pdef);
		declaredMethods = buildDeclaredMethods(cls, this);
		methods = buildMethods(bases, declaredMethods);
	}

	public Set<PdefInterface> getBases() {
		return bases;
	}

	public Map<String, PdefMethod> getMethods() {
		return methods;
	}

	public Map<String, PdefMethod> getDeclaredMethods() {
		return declaredMethods;
	}

	public Object invoke(final Object object, final Iterable<MethodCall> calls)
			throws RpcException {
		checkNotNull(object);

		Object o = object;
		PdefInterface i = this;
		StringBuilder path = new StringBuilder();
		for (MethodCall call : calls) {
			String methodName = call.getMethod();
			path.append(methodName);

			PdefMethod method = i == null ? null : i.getMethods().get(methodName);
			if (method == null) {
				throw RpcException.builder()
						.setCode(RpcExceptionCode.BAD_REQUEST)
						.setText("Method not found: " + path)
						.build();
			}

			o = method.invoke(o, call.getArgs());
			PdefDescriptor resultInfo = method.getResult();
			if (resultInfo.getType() == PdefType.INTERFACE) {
				i = (PdefInterface) resultInfo;
			} else {
				i = null;
			}
		}

		return o;
	}

	static ImmutableSet<PdefInterface> buildBases(final Class<?> cls, final Pdef pdef) {
		ImmutableSet.Builder<PdefInterface> b = ImmutableSet.builder();
		for (Class<?> base : cls.getInterfaces()) {
			PdefInterface descriptor = (PdefInterface) pdef.get(base);
			b.add(descriptor);
		}
		return b.build();
	}

	static ImmutableMap<String, PdefMethod> buildDeclaredMethods(final Class<?> cls,
			final PdefInterface iface) {
		ImmutableMap.Builder<String, PdefMethod> b = ImmutableMap.builder();
		for (Method method : cls.getDeclaredMethods()) {
			PdefMethod descriptor = new PdefMethod(method, iface);
			b.put(descriptor.getName(), descriptor);
		}
		return b.build();
	}

	static ImmutableMap<String, PdefMethod> buildMethods(final Set<PdefInterface> bases,
			final Map<String, PdefMethod> declaredMethods) {
		ImmutableMap.Builder<String, PdefMethod> b = ImmutableMap.builder();
		for (PdefInterface base : bases) {
			b.putAll(base.methods);
		}
		b.putAll(declaredMethods);
		return b.build();
	}
}
