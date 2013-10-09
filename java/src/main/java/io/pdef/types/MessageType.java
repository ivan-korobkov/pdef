package io.pdef.types;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.*;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public class MessageType extends DataType {
	private final MessageType base;
	private final Supplier<Message> supplier;

	private final List<MessageField> declaredFields;
	private final List<MessageField> fields;

	private final Enum<?> discriminatorValue;
	private final MessageField discriminator;
	private final List<Supplier<MessageType>> subtypes;

	private final boolean form;

	public static Builder builder() {
		return new Builder();
	}

	private MessageType(final Builder builder) {
		super(TypeEnum.MESSAGE, builder.javaClass);
		base = builder.base;
		supplier = checkNotNull(builder.supplier);

		declaredFields = buildDeclaredFields(builder.declaredFields, this);
		fields = buildFields(declaredFields, base);

		discriminator = findDiscriminator(fields);
		discriminatorValue = builder.discriminatorValue;
		subtypes = ImmutableList.copyOf(builder.subtypes);

		form = builder.form;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(getJavaClass().getSimpleName())
				.toString();
	}

	public MessageType getBase() {
		return base;
	}

	public Supplier<Message> getSupplier() {
		return supplier;
	}

	public boolean isForm() {
		return form;
	}

	public Enum<?> getDiscriminatorValue() {
		return discriminatorValue;
	}

	public MessageField getDiscriminator() {
		return discriminator;
	}

	public List<Supplier<MessageType>> getSubtypes() {
		return subtypes;
	}

	public List<MessageField> getDeclaredFields() {
		return declaredFields;
	}

	public List<MessageField> getFields() {
		return fields;
	}

	// Methods.

	public Message copy(final Object object) {
		if (object == null) {
			return null;
		}

		Message message = (Message) object;
		Message copy = newInstance();

		for (MessageField field : fields) {
			Object value = field.get(message);
			if (value == null) {
				continue;
			}

			Object cvalue = field.getType().copy(value);
			field.set(copy, cvalue);
		}

		return copy;
	}

	public Message newInstance() {
		return supplier.get();
	}

	// Native format.

	@Override
	protected Object doParseNative(final Object o) throws Exception {
		if (o == null) {
			return null;
		}

		Map<?, ?> map = (Map<?, ?>) o;
		Enum<?> discriminatorValue = parseNativeDiscriminator(map);
		MessageType type = getSubtype(discriminatorValue);
		return doParseNative(type, map);
	}

	private static Object doParseNative(final MessageType type, final Map<?, ?> map)
			throws Exception {
		Message result = type.newInstance();
		for (MessageField field : type.fields) {
			Object nvalue = map.get(field.getName());
			if (nvalue == null) {
				continue;
			}

			Object value = field.getType().doParseNative(nvalue);
			field.set(result, value);
		}

		return result;
	}

	@Nullable
	@VisibleForTesting
	Enum<?> parseNativeDiscriminator(final Map<?, ?> map) throws Exception {
		if (discriminator == null) {
			// This is not a polymorphic message.
			return null;
		}

		Object value = map.get(discriminator.getName());
		if (value == null) {
			// No polymorphic discriminator value.
			return null;
		}

		return (Enum<?>) discriminator.getType().doParseNative(value);
	}

	@VisibleForTesting
	MessageType getSubtype(@Nullable final Enum<?> discriminatorValue) {
		if (discriminatorValue == null) {
			return this;
		}

		for (Supplier<MessageType> supplier : subtypes) {
			MessageType subtype = supplier.get();
			if (discriminatorValue.equals(subtype.getDiscriminatorValue())) {
				return subtype;
			}
		}

		return this;
	}

	@Override
	protected Map<String, Object> doToNative(final Object o) throws Exception {
		if (o == null) {
			return null;
		}

		Message message = (Message) o;
		MessageType type = message.type();
		return doToNative(type, message);
	}

	private static Map<String, Object> doToNative(final MessageType type, final Message message)
			throws Exception {
		Map<String, Object> result = Maps.newLinkedHashMap();

		for (MessageField field : type.getFields()) {
			Object value = field.get(message);
			if (value == null) {
				continue;
			}

			Object nvalue = field.getType().doToNative(value);
			result.put(field.getName(), nvalue);
		}

		return result;
	}

	public static class Builder {
		private Class<?> javaClass;
		private MessageType base;
		private Enum<?> discriminatorValue;
		private Supplier<Message> supplier;
		private final List<MessageField.Builder> declaredFields;
		private final List<Supplier<MessageType>> subtypes;
		private boolean form;

		private Builder() {
			declaredFields = Lists.newArrayList();
			subtypes = Lists.newArrayList();
		}

		public Builder setJavaClass(final Class<?> cls) {
			this.javaClass = cls;
			return this;
		}

		public Builder setBase(final MessageType base) {
			this.base = base;
			return this;
		}

		public Builder setForm(final boolean form) {
			this.form = form;
			return this;
		}

		public Builder setDiscriminatorValue(final Enum<?> value) {
			this.discriminatorValue = value;
			return this;
		}

		public Builder setSupplier(final Supplier<Message> supplier) {
			this.supplier = supplier;
			return this;
		}

		public Builder addField(final String name, final boolean discriminator,
				final Supplier<Type> type,
				final MessageField.Getter getter,
				final MessageField.Setter setter) {
			return addField(MessageField.builder()
					.setName(name)
					.setType(type)
					.setDiscriminator(discriminator)
					.setGetter(getter)
					.setSetter(setter));
		}

		public Builder addField(final MessageField.Builder field) {
			declaredFields.add(field);
			return this;
		}

		public Builder addSubtype(final Supplier<MessageType> subtype) {
			subtypes.add(subtype);
			return this;
		}

		public MessageType build() {
			return new MessageType(this);
		}
	}

	// Static utility methods.

	private static ImmutableList<MessageField> buildDeclaredFields(
			final Iterable<MessageField.Builder> fields, final MessageType message) {
		ImmutableList.Builder<MessageField> temp = ImmutableList.builder();
		for (MessageField.Builder fb : fields) {
			MessageField field = fb.build(message);
			temp.add(field);
		}
		return temp.build();
	}

	private static ImmutableList<MessageField> buildFields(
			final Iterable<MessageField> declaredFields, final MessageType base) {
		return ImmutableList.<MessageField>builder()
				.addAll(base == null ? ImmutableList.<MessageField>of() : base.getFields())
				.addAll(declaredFields)
				.build();
	}

	private static MessageField findDiscriminator(final Iterable<MessageField> fields) {
		for (MessageField field : fields) {
			if (field.isDiscriminator()) {
				return field;
			}
		}
		return null;
	}
}
