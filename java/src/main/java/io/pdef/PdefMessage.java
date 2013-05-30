package io.pdef;

import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class PdefMessage extends PdefDescriptor {
	private final Class<?> builderClass;
	private final Method builderMethod;
	private final PdefMessage base;
	private final Map<String, PdefField> fields;
	private final Map<String, PdefField> declaredFields;
	private final Map<String, PdefMessage> subtypes;
	private final PdefField discriminator;

	PdefMessage(final Class<?> cls, final Pdef pdef) {
		super(PdefType.MESSAGE, cls, pdef);
		Class<?> baseType = cls.getSuperclass();
		try {
			builderClass = Class.forName(cls.getName() + "$Builder");
			builderMethod = cls.getDeclaredMethod("builder");
			base = baseType == GeneratedMessage.class || baseType == GeneratedException.class
				   ? null : (PdefMessage) descriptorOf(baseType);
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}

		{
			Field[] declared = cls.getDeclaredFields();
			ImmutableMap.Builder<String, PdefField> builder = ImmutableMap.builder();
			for (Field field : declared) {
				if (Modifier.isStatic(field.getModifiers())) continue;
				PdefField fieldInfo = new PdefField(field, this);
				builder.put(fieldInfo.getName(), fieldInfo);
			}
			declaredFields = builder.build();
		}

		{
			fields = base == null ? declaredFields : ImmutableMap.<String, PdefField>builder()
					.putAll(base.getFields())
					.putAll(declaredFields)
					.build();
		}

		if (!cls.isAnnotationPresent(Subtypes.class)) {
			discriminator = null;
			subtypes = ImmutableMap.of();
		} else {
			Discriminator dann = cls.getAnnotation(Discriminator.class);
			Subtypes sann = cls.getAnnotation(Subtypes.class);
			checkNotNull(dann, "Discriminator annotation must be present in %s", cls);
			checkNotNull(sann, "Subtypes annotation must be present in %s", cls);

			ImmutableMap.Builder<String, PdefMessage> builder = ImmutableMap.builder();
			for (Subtype value : sann.value()) {
				String name = value.type();
				PdefMessage info = (PdefMessage) descriptorOf(value.value());
				builder.put(name, info);
			}
			subtypes = builder.build();
			discriminator = fields.get(dann.value().toLowerCase());
			checkState(discriminator != null, "Discriminator field \"%s\" is not found in %s",
					dann.value(), cls);
		}
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this).addValue(getJavaClass().getSimpleName()).toString();
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
}
