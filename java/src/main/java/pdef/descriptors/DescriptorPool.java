package pdef.descriptors;

import static com.google.common.base.Preconditions.*;
import com.google.common.collect.Maps;
import pdef.Message;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentMap;

public class DescriptorPool {
	private final ConcurrentMap<Type, Descriptor> map;

	public DescriptorPool() {
		map = Maps.newConcurrentMap();
	}

	public Descriptor getDescriptor(final Type type) {
		checkNotNull(type);
		Descriptor descriptor = map.get(type);
		if (descriptor != null) return descriptor;

		synchronized (this) {
			descriptor = map.get(type);
			if (descriptor != null) return descriptor;

			descriptor = createDescriptor(type);
			map.put(type, descriptor);

			descriptor.link();
			return descriptor;
		}
	}

	private Descriptor createDescriptor(final Type type) {
		if (type instanceof Class) {
			Class<?> cls = (Class<?>) type;
			if (Message.class.isAssignableFrom(cls)) {
				return new MessageDescriptor(cls, this);
			}
		} else if (type instanceof ParameterizedType) {

		}
		throw new IllegalArgumentException("Unsupported type for a descriptor " + type);
	}
}
