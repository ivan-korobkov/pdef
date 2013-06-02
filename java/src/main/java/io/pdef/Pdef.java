package io.pdef;

import static com.google.common.base.Preconditions.*;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

/** Pdef descriptor pool. */
public class Pdef {
	private final Map<Type, PdefDescriptor> descriptors;
	private final Map<Class<?>, Constructor<?>> proxyConstructors;

	public Pdef() {
		descriptors = Maps.newHashMap();
		proxyConstructors = Maps.newHashMap();
	}

	/** Returns a pdef descriptor for a java type. */
	public synchronized PdefDescriptor get(final Type javaType) {
		PdefDescriptor d = descriptors.get(javaType);
		if (d != null) return d;
		return createDescriptor(javaType);
	}

	/** Creates a new proxy, internally, caches the proxy classes. */
	@SuppressWarnings("unchecked")
	public synchronized <T> T proxy(final Class<T> cls, final InvocationHandler handler) {
		Constructor<?> constructor = proxyConstructors.get(cls);
		if (constructor == null) {
			Class<?> proxyClass = Proxy.getProxyClass(cls.getClassLoader(), new Class[]{cls});
			try {
				constructor = proxyClass.getConstructor(new Class[] {InvocationHandler.class});
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

	/** Adds a new descriptor to this pdef pool; must be called from a descriptor constructor
	 * to allow circular references. */
	void add(final Type javaType, final PdefDescriptor descriptor) {
		checkNotNull(javaType);
		checkNotNull(descriptor);
		checkState(!descriptors.containsKey(javaType), "Duplicate type descriptor for " + javaType);
		descriptors.put(javaType, descriptor);
	}

	private PdefDescriptor createDescriptor(final Type type) {
		checkNotNull(type);
		Class<?> cls;
		if (type instanceof Class<?>) {
			cls = (Class<?>) type;
		} else if (type instanceof ParameterizedType) {
			cls = (Class<?>) ((ParameterizedType) type).getRawType();
		} else {
			throw new IllegalArgumentException("Unsupported java type " + type);
		}

		if (cls == boolean.class || cls == Boolean.class) return primitive(PdefType.BOOL, type, false);
		if (cls == short.class || cls == Short.class) return primitive(PdefType.INT16, type, 0);
		if (cls == int.class || cls == Integer.class) return primitive(PdefType.INT32, type, 0);
		if (cls == long.class || cls == Long.class) return primitive(PdefType.INT64, type, 0);
		if (cls == float.class || cls == Float.class) return primitive(PdefType.FLOAT, type, 0.0f);
		if (cls == double.class || cls == Double.class) return primitive(PdefType.DOUBLE, type, 0.0d);

		if (cls == BigDecimal.class) return primitive(PdefType.DECIMAL, type, BigDecimal.ZERO);
		if (cls == String.class) return primitive(PdefType.STRING, type, null);
		if (cls == Object.class) return primitive(PdefType.OBJECT, type, null);
		if (cls == void.class || cls == Void.class) return primitive(PdefType.VOID, type, null);

		if (cls == List.class) return new PdefList(type, this);
		if (cls == Set.class) return new PdefSet(type, this);
		if (cls == Map.class) return new PdefMap(type, this);
		if (cls.isEnum()) return new PdefEnum(type, this);

		if (Message.class.isAssignableFrom(cls)) return new PdefMessage(cls, this);
		if (Future.class.isAssignableFrom(cls)) return new PdefFuture(type, this);
		if (cls.isInterface()) return new PdefInterface(cls, this);

		throw new IllegalArgumentException("Unsupported java javaType " + type);
	}

	private PdefDescriptor primitive(final PdefType type, final Type javaType,
			final Object defaultValue) {
		return new PdefPrimitive(type, javaType, defaultValue, this);
	}

	static Type[] actualTypeArgs(final Type javaType) {
		return ((ParameterizedType) javaType).getActualTypeArguments();
	}
}
