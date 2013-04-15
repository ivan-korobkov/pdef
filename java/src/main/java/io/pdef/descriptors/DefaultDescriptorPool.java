package io.pdef.descriptors;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.primitives.Primitives;
import com.google.common.util.concurrent.ListenableFuture;
import io.pdef.Interface;
import io.pdef.Message;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import static com.google.common.base.Preconditions.checkNotNull;

public class DefaultDescriptorPool implements DescriptorPool {
	private static final Set<Class<?>> VALUE_CLASSES = ImmutableSet.<Class<?>>of(
			boolean.class, short.class, int.class, long.class, float.class, double.class,
			String.class, Object.class, void.class);

	private final ConcurrentMap<Type, Descriptor> map;

	public DefaultDescriptorPool() {
		map = Maps.newConcurrentMap();
	}

	@Override
	public Descriptor getDescriptor(final Type type) {
		checkNotNull(type);
		Type javaType = type instanceof Class ? Primitives.unwrap((Class<?>) type) : type;
		Descriptor descriptor = map.get(javaType);
		if (descriptor != null) return descriptor;

		synchronized (this) {
			descriptor = map.get(javaType);
			if (descriptor != null) return descriptor;

			descriptor = createDescriptor(javaType);
			if (descriptor == null) {
				throw new IllegalArgumentException("Unsupported descriptor type " + javaType);
			}
			map.put(javaType, descriptor);

			descriptor.link();
			return descriptor;
		}
	}

	protected Descriptor createDescriptor(final Type type) {
		if (type instanceof Class) {
			return createDescriptor((Class<?>) type);
		} else if (type instanceof ParameterizedType) {
			return createDescriptor((ParameterizedType) type);
		}
		return null;
	}

	protected Descriptor createDescriptor(final Class<?> cls1) {
		Class<?> cls = Primitives.unwrap(cls1);
		if (Message.class.isAssignableFrom(cls)) return new MessageDescriptor(cls, this);
		if (Interface.class.isAssignableFrom(cls)) return new InterfaceDescriptor(cls, this);
		if (VALUE_CLASSES.contains(cls)) return new ValueDescriptor(cls, this);
		if (cls.isEnum()) return new EnumDescriptor(cls, this);
		return null;
	}

	protected Descriptor createDescriptor(final ParameterizedType type) {
		Class<?> raw = (Class<?>) type.getRawType();
		if (List.class == raw) return new ListDescriptor(type, this);
		if (Set.class == raw) return new SetDescriptor(type, this);
		if (Map.class == raw) return new MapDescriptor(type, this);
		if (ListenableFuture.class == raw) return new FutureDescriptor(type, this);
		return null;
	}
}
