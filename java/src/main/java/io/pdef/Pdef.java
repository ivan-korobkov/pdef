package io.pdef;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class Pdef {
	private final Map<Type, PdefDescriptor> descriptors;
	private final Map<Class<?>, Constructor<?>> proxyConstructors;

	public Pdef() {
		descriptors = Maps.newHashMap();
		proxyConstructors = Maps.newHashMap();
	}

	/** Returns a pdef descriptor for a java type. */
	public synchronized PdefDescriptor get(final Type javaType) {
		PdefDescriptor info = descriptors.get(javaType);
		if (info != null) return info;
		return createDescriptor(javaType);
	}

	/** Creates a new proxy, internally, caches the proxy classes. */
	@SuppressWarnings("unchecked")
	public synchronized <T> T proxy(final Class<T> cls, final InvocationHandler handler) {
		Constructor<?> constructor = proxyConstructors.get(cls);
		if (constructor == null) {
			Class<?> proxyClass = Proxy.getProxyClass(cls.getClassLoader(), new Class[]{cls});
			try {
				constructor = proxyClass.getConstructor(new Class[] { InvocationHandler.class});
			} catch (NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
			proxyConstructors.put(cls, constructor);
		}

		try {
			return (T) constructor.newInstance(handler);
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
	}

	/** Adds a new descriptor to this pdef pool; must be called from the descriptor constructors
	 * to allow circular references. */
	void add(final Type javaType, final PdefDescriptor descriptor) {
		checkState(!descriptors.containsKey(javaType), "Duplicate type descriptor for " + javaType);
		descriptors.put(javaType, descriptor);
	}

	private PdefDescriptor createDescriptor(final Type javaType) {
		checkNotNull(javaType);
		Class<?> cls;
		if (javaType instanceof Class<?>) {
			cls = (Class<?>) javaType;
		} else if (javaType instanceof ParameterizedType) {
			cls = (Class<?>) ((ParameterizedType) javaType).getRawType();
		} else {
			throw new IllegalArgumentException("Unsupported java javaType " + javaType);
		}

		if (cls == boolean.class || cls == Boolean.class) return datatype(javaType, PdefType.BOOL);
		if (cls == short.class || cls == Short.class) return datatype(javaType, PdefType.INT16);
		if (cls == int.class || cls == Integer.class) return datatype(javaType, PdefType.INT32);
		if (cls == long.class || cls == Long.class) return datatype(javaType, PdefType.INT64);
		if (cls == float.class || cls == Float.class) return datatype(javaType, PdefType.FLOAT);
		if (cls == double.class || cls == Double.class) return datatype(javaType, PdefType.DOUBLE);

		if (cls == BigDecimal.class) return datatype(javaType, PdefType.DECIMAL);
		if (cls == String.class) return datatype(javaType, PdefType.STRING);
		if (cls == Object.class) return datatype(javaType, PdefType.OBJECT);
		if (cls == void.class || cls == Void.class) return datatype(javaType, PdefType.VOID);

		if (cls == List.class) return new PdefList(javaType, this);
		if (cls == Set.class) return new PdefSet(javaType, this);
		if (cls == Map.class) return new PdefMap(javaType, this);
		if (cls.isEnum()) return new PdefEnum(javaType, this);

		if (Message.class.isAssignableFrom(cls)) return new PdefMessage(cls, this);
		if (Future.class.isAssignableFrom(cls)) return new PdefFuture(javaType, this);
		if (cls.isInterface()) return new PdefInterface(cls, this);

		throw new IllegalArgumentException("Unsupported java javaType " + javaType);
	}

	private PdefDescriptor datatype(final Type javaType, final PdefType type) {
		return new PdefDatatype(type, javaType, this);
	}

	static Type[] actualTypeArgs(final Type javaType) {
		return ((ParameterizedType) javaType).getActualTypeArguments();
	}
}
