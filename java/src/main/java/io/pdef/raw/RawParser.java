package io.pdef.raw;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import io.pdef.AbstractParser;
import io.pdef.Message;
import io.pdef.SerializationException;
import io.pdef.descriptors.*;
import io.pdef.Invocation;

import java.lang.reflect.Type;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

public class RawParser extends AbstractParser {
	public RawParser() {
		super(new DefaultDescriptorPool());
	}

	public RawParser(final DescriptorPool pool) {
		super(pool);
	}

	@Override
	protected Message parseMessage(final MessageDescriptor descriptor, final Object object) {
		if (object == null) return null;
		Map<?, ?> map = (Map<?, ?>) object;
		MessageDescriptor polymorphic = parseDescriptorType(descriptor, map);
		Collection<FieldDescriptor> fields = polymorphic.getFields().values();

		Message.Builder builder = polymorphic.newBuilder();
		for (FieldDescriptor field : fields) {
			String name = field.getName();
			if (!map.containsKey(name)) continue;

			Descriptor type = field.getType();
			Object val = map.get(name);
			Object pval = doParse(type, val);

			// Even-though the field is read-only we still parse it to validate the data.
			// TODO: type field
			// if (field.isTypeField()) continue;
			field.set(builder, pval);
		}

		return builder.build();
	}

	private MessageDescriptor parseDescriptorType(final MessageDescriptor descriptor,
			final Map<?, ?> map) {
		SubtypesDescriptor subtypes = descriptor.getSubtypes();
		if (subtypes == null) return descriptor;

		FieldDescriptor field = subtypes.getField();
		String name = field.getName();
		if (!map.containsKey(name)) return descriptor;

		Descriptor type = field.getType();
		Object val = map.get(name);
		Object pval = doParse(type, val);
		Class<?> subtype = subtypes.getMap().get(pval);

		if (descriptor.getJavaType() == subtype) return descriptor;
		if (subtype == null) return descriptor;

		MessageDescriptor subdescriptor = (MessageDescriptor) pool.getDescriptor(subtype);
		return parseDescriptorType(subdescriptor, map);
	}

	@Override
	protected Enum<?> parseEnum(final EnumDescriptor descriptor, final Object object) {
		if (object == null) return null;
		String value = ((String) object).toUpperCase();
		return descriptor.getValues().get(value);
	}

	@Override
	protected List<Object> parseList(final ListDescriptor descriptor, final Object object) {
		if (object == null) return null;
		List<?> list = (List<?>) object;
		Descriptor element = descriptor.getElement();

		ImmutableList.Builder<Object> builder = ImmutableList.builder();
		for (Object o : list) {
			Object p = doParse(element, o);
			builder.add(p);
		}

		return builder.build();
	}

	@Override
	protected Set<Object> parseSet(final SetDescriptor descriptor, final Object object) {
		if (object == null) return null;
		Set<?> set = (Set<?>) object;
		Descriptor element = descriptor.getElement();

		ImmutableSet.Builder<Object> builder = ImmutableSet.builder();
		for (Object o : set) {
			Object p = doParse(element, o);
			builder.add(p);
		}

		return builder.build();
	}

	@Override
	protected Map<Object, Object> parseMap(final MapDescriptor descriptor, final Object object) {
		if (object == null) return null;
		Map<?, ?> map = (Map<?, ?>) object;
		Descriptor key = descriptor.getKey();
		Descriptor val = descriptor.getValue();

		ImmutableMap.Builder<Object, Object> builder = ImmutableMap.builder();
		for (Map.Entry<?, ?> e : map.entrySet()) {
			Object pkey = doParse(key, e.getKey());
			Object pval = doParse(val, e.getValue());
			builder.put(pkey, pval);
		}

		return builder.build();
	}

	@Override
	protected Boolean parseBoolean(final Object value) {
		if (value == null) return false;
		if (value instanceof Boolean) return (Boolean) value;
		if (value instanceof Number) {
			int v = ((Number) value).intValue();
			if (v == 1) return true;
			if (v == 0) return false;
			throw new SerializationException("Unexpected boolean value " + value);
		}
		String s = ((String) value).toLowerCase();
		if (s.equals("true")) return true;
		if (s.equals("false")) return false;
		throw new SerializationException("Unexpected boolean value " + value);
	}

	@Override
	protected Short parseShort(final Object value) {
		if (value == null) return (short) 0;
		if (value instanceof Number) return ((Number) value).shortValue();
		String s = (String) value;
		return Short.parseShort(s);
	}

	@Override
	protected Integer parseInt(final Object value) {
		if (value == null) return 0;
		if (value instanceof Number) return ((Number) value).intValue();
		String s = (String) value;
		return Integer.parseInt(s);
	}

	@Override
	protected Long parseLong(final Object value) {
		if (value == null) return 0L;
		if (value instanceof Number) return ((Number) value).longValue();
		String s = (String) value;
		return Long.parseLong(s);
	}

	@Override
	protected Float parseFloat(final Object value) {
		if (value == null) return 0f;
		if (value instanceof Number) return ((Number) value).floatValue();
		String s = (String) value;
		return Float.parseFloat(s);
	}

	@Override
	protected Double parseDouble(final Object value) {
		if (value == null) return 0d;
		if (value instanceof Number) return ((Number) value).doubleValue();
		String s = (String) value;
		return Double.parseDouble(s);
	}

	@Override
	protected String parseString(final Object value) {
		return value == null ? null : (String) value;
	}

	@Override
	public List<Invocation> parseInvocations(final Class<?> interfaceClass, final Object object) {
		checkNotNull(interfaceClass);
		checkNotNull(object);

		try {
			Map<?, ?> map = (Map<?, ?>) object;
			InterfaceDescriptor descriptor = (InterfaceDescriptor) pool
					.getDescriptor(interfaceClass);
			return parseMethodNames(descriptor, map);
		} catch (SerializationException e) {
			throw e;
		} catch (Exception e) {
			throw new SerializationException(e);
		}
	}

	@SuppressWarnings("unchecked")
	private List<Invocation> parseMethodNames(final InterfaceDescriptor descriptor,
			final Map<?, ?> map) {
		List<Invocation> Invocations = Lists.newArrayList();

		InterfaceDescriptor d = descriptor;
		StringBuilder path = new StringBuilder();
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			String methodName = ((String) entry.getKey()).toLowerCase();
			if (path.length() > 0) path.append(".");
			path.append(methodName);

			MethodDescriptor method = d.getMethods().get(methodName);
			if (method == null) {
				throw new SerializationException(path.toString() + ": method not found ");
			}

			Map<?, ?> rawArgs = (Map<?, ?>) entry.getValue();
			Object[] args = parseArgs(method, rawArgs);
			Invocation Invocation = new Invocation(method, args);
			Invocations.add(Invocation);

			Descriptor result = method.getResult();
			if (result instanceof InterfaceDescriptor) {
				d = (InterfaceDescriptor) result;
			} else {
				break;
			}
		}

		return Invocations;
	}

	private Object[] parseArgs(final MethodDescriptor method, final Map<?, ?> rawArgs) {
		Map<String, Type> argTypes = method.getArgTypes();
		Object[] args = new Object[argTypes.size()];

		int i = 0;
		for (Map.Entry<String, Type> e : argTypes.entrySet()) {
			String name = e.getKey();
			Type argType = e.getValue();
			Object rawArg = rawArgs.get(name);
			Object arg = parse(argType, rawArg);
			args[i++] = arg;
		}

		return args;
	}
}
