package io.pdef.descriptors;

import com.google.common.collect.ImmutableMap;
import io.pdef.Discriminator;
import io.pdef.Subtype;
import io.pdef.Subtypes;

import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class SubtypesDescriptor {
	private final MessageDescriptor message;
	private final Map<Enum<?>, Class<?>> subtypes;
	private final FieldDescriptor field;

	public SubtypesDescriptor(final MessageDescriptor message) {
		this.message = message;
		Class<?> cls = message.getJavaType();
		checkArgument(hasSubtypes(cls), "No subtypes in %s", cls);

		Discriminator fieldAnnotation = cls.getAnnotation(Discriminator.class);
		Subtypes subtypesAnnotation = cls.getAnnotation(Subtypes.class);
		checkNotNull(fieldAnnotation, "Discriminator annotation must be present in %s", cls);
		checkNotNull(subtypesAnnotation, "Subtypes annotation must be present in %s", cls);

		field = message.getFields().get(fieldAnnotation.value());
		checkNotNull(field, "Type field %s is not found in %s", fieldAnnotation.value(), cls);

		EnumDescriptor enumType = (EnumDescriptor) field.getType();
		ImmutableMap.Builder<Enum<?>, Class<?>> builder = ImmutableMap.builder();
		for (Subtype value : subtypesAnnotation.value()) {
			String typeName = value.type();
			Enum<?> subtype = enumType.getValues().get(typeName.toUpperCase());
			builder.put(subtype, value.value());
		}
		subtypes = builder.build();
	}

	public static boolean hasSubtypes(final Class<?> cls) {
		return cls.isAnnotationPresent(Subtypes.class);
	}

	public MessageDescriptor getMessage() {
		return message;
	}

	public Map<Enum<?>, Class<?>> getMap() {
		return subtypes;
	}

	public FieldDescriptor getField() {
		return field;
	}
}
