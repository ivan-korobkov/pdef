package io.pdef;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import static com.google.common.base.Preconditions.checkState;

/** Pdef message descriptor. */
public class PdefMessage extends PdefDatatype {
	private final PdefMessage base;
	private final Class<?> builderClass;
	private final Method builderMethod;
	private final Object defaultValue;

	private final ImmutableMap<String, PdefField> fields;
	private final ImmutableMap<String, PdefField> declaredFields;
	private final ImmutableMap<String, PdefMessage> subtypes;
	private final PdefField discriminator;

	PdefMessage(final Class<?> cls, final Pdef pdef) {
		super(PdefType.MESSAGE, cls, pdef);
		Class<?> baseType = cls.getSuperclass();
		base = baseType == GeneratedMessage.class || baseType == GeneratedException.class
			   ? null : (PdefMessage) pdef.get(baseType);
		try {
			// Must be initialized before the fields.
			builderClass = Class.forName(cls.getName() + "$Builder");
			builderMethod = cls.getDeclaredMethod("builder");
			defaultValue = cls.getDeclaredMethod("getInstance").invoke(null);
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}

		declaredFields = buildDeclaredFields(cls, this);
		fields = buildFields(base, declaredFields);
		discriminator = buildDiscriminator(cls, fields);
		subtypes = buildSubtypes(cls, pdef);
	}

	public Class<?> getBuilderClass() {
		return builderClass;
	}

	public PdefMessage getBase() {
		return base;
	}

	public Map<String, PdefField> getFields() {
		return fields;
	}

	public Map<String, PdefField> getDeclaredFields() {
		return declaredFields;
	}

	@Override
	public Object defaultValue() {
		return defaultValue;
	}

	@Nullable
	public PdefField getDiscriminator() {
		return discriminator;
	}

	public Map<String, PdefMessage> getSubtypes() {
		return subtypes;
	}

	public boolean isPolymorphic() {
		return !subtypes.isEmpty();
	}

	public Message.Builder createBuilder() {
		try {
			return (Message.Builder) builderMethod.invoke(null);
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
	}

	static ImmutableMap<String, PdefField> buildDeclaredFields(final Class<?> cls,
			final PdefMessage message) {
		Field[] declared = cls.getDeclaredFields();
		ImmutableMap.Builder<String, PdefField> builder = ImmutableMap.builder();
		for (Field field : declared) {
			if (Modifier.isStatic(field.getModifiers())) continue;
			if (Modifier.isTransient(field.getModifiers())) continue;

			PdefField fieldInfo = new PdefField(field, message);
			builder.put(fieldInfo.getName(), fieldInfo);
		}
		return builder.build();
	}

	static ImmutableMap<String, PdefField> buildFields(final PdefMessage base,
			final ImmutableMap<String, PdefField> declaredFields) {
		return base == null ? declaredFields : ImmutableMap.<String, PdefField>builder()
				.putAll(base.getFields())
				.putAll(declaredFields)
				.build();
	}

	@Nullable
	static PdefField buildDiscriminator(final Class<?> cls, final Map<String, PdefField> fields) {
		Discriminator ann = cls.getAnnotation(Discriminator.class);
		if (ann == null) return null;

		PdefField discriminator = fields.get(ann.value().toLowerCase());
		checkState(discriminator != null, "Discriminator field \"%s\" is not present in %s",
				ann.value(), cls);

		return discriminator;
	}

	static ImmutableMap<String, PdefMessage> buildSubtypes(final Class<?> cls, final Pdef pdef) {
		Subtypes ann = cls.getAnnotation(Subtypes.class);
		if (ann == null) return ImmutableMap.of();

		ImmutableMap.Builder<String, PdefMessage> builder = ImmutableMap.builder();
		for (Subtype value : ann.value()) {
			String name = value.type();
			PdefMessage info = (PdefMessage) pdef.get(value.value());
			builder.put(name, info);
		}

		return builder.build();
	}
}
