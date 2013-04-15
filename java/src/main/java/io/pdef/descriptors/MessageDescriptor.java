package io.pdef.descriptors;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import io.pdef.Message;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;

public class MessageDescriptor extends AbstractDescriptor {
	private Class<?> builderType;
	private Constructor builderConstructor;
	private MessageDescriptor base;
	private Map<String, FieldDescriptor> fields;
	private Map<String, FieldDescriptor> declaredFields;

	public MessageDescriptor(final Class<?> messageType, final DescriptorPool pool) {
		super(messageType, DescriptorType.MESSAGE, pool);
		checkArgument(Message.class.isAssignableFrom(messageType));
	}

	@Override
	public Class<?> getJavaType() {
		return (Class<?>) super.getJavaType();
	}

	public Class<?> getBuilderType() {
		return builderType;
	}

	public MessageDescriptor getBase() {
		return base;
	}

	public Map<String, FieldDescriptor> getFields() {
		return fields;
	}

	public Map<String, FieldDescriptor> getDeclaredFields() {
		return declaredFields;
	}

	public Message.Builder newBuilder() {
		try {
			return (Message.Builder) builderConstructor.newInstance();
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
	}

	@Override
	protected void doLink() {
		linkBuilder();
		linkBase();
		linkDeclaredFields();
		linkFields();
	}

	private void linkBuilder() {
		String name = getJavaType().getName();
		try {
			builderType = Class.forName(name + "$Builder");
			builderConstructor = builderType.getConstructor();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		checkArgument(Message.Builder.class.isAssignableFrom(builderType));
	}

	private void linkBase() {
		Type superclass = getJavaType().getGenericSuperclass();
		Type baseType = superclass == Object.class ? null : superclass;
		base = baseType == null ? null : (MessageDescriptor) pool.getDescriptor(baseType);
	}

	private void linkDeclaredFields() {
		Field[] declared = getJavaType().getDeclaredFields();

		ImmutableMap.Builder<String, FieldDescriptor> builder = ImmutableMap.builder();
		for (Field field : declared) {
			FieldDescriptor fdescriptor = new FieldDescriptor(field, this);
			builder.put(fdescriptor.getName(), fdescriptor);
		}
		declaredFields = builder.build();
	}

	private void linkFields() {
		if (base == null) {
			fields = declaredFields;
		} else {
			fields = ImmutableMap.<String, FieldDescriptor>builder()
					.putAll(base.getFields())
					.putAll(declaredFields)
					.build();
		}
	}
}
