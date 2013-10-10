package io.pdef.meta;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.pdef.Message;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * MessageType is a metatype for Pdef messages.
 * */
public class MessageType<M extends Message> extends DataType<M> {
	private final Class<M> javaClass;
	private final Supplier<M> supplier;
	private final MessageType<? super M> base;
	private final List<MessageField<M, ?>> declaredFields;
	private final List<MessageField<? super M, ?>> fields;

	private final Enum<?> discriminatorValue;
	private final MessageField<? super M, ?> discriminator;
	private final List<Supplier<MessageType<? extends M>>> subtypes;

	private final boolean form;

	private MessageType(final Builder<M> builder) {
		super(TypeEnum.MESSAGE);
		this.javaClass = checkNotNull(builder.javaClass);
		this.supplier = checkNotNull(builder.supplier);

		base = builder.base;

		declaredFields = ImmutableList.copyOf(builder.declaredFields);
		fields = joinFields(declaredFields, base);

		discriminator = findDiscriminator(fields);
		discriminatorValue = builder.discriminatorValue;
		subtypes = ImmutableList.copyOf(builder.subtypes);

		form = builder.form;
	}

	public static <M extends Message> Builder<M> builder() {
		return new Builder<M>();
	}

	private static <M extends Message> ImmutableList<MessageField<? super M, ?>> joinFields(
			final Iterable<MessageField<M, ?>> declaredFields,
			@Nullable final MessageType<? super M> base) {
		ImmutableList.Builder<MessageField<? super M, ?>> result = ImmutableList.builder();
		if (base != null) {
			result.addAll(base.getFields());
		}
		result.addAll(declaredFields);
		return result.build();
	}

	private static <M> MessageField<? super M, ?> findDiscriminator(
			final Iterable<MessageField<? super M, ?>> fields) {
		for (MessageField<? super M, ?> field : fields) {
			if (field.isDiscriminator()) {
				return field;
			}
		}
		return null;
	}

	/** Returns a Java class. */
	public Class<M> getJavaClass() {
		return javaClass;
	}

	/** Returns a base type. */
	public MessageType getBase() {
		return base;
	}

	/** Returns a discriminator value. */
	public Enum<?> getDiscriminatorValue() {
		return discriminatorValue;
	}

	/** Returns a discriminator field. */
	public MessageField<? super M, ?> getDiscriminator() {
		return discriminator;
	}

	/** Returns a list of subtypes. */
	public List<Supplier<MessageType<? extends M>>> getSubtypes() {
		return subtypes;
	}

	/** Returns declared fields. */
	public List<MessageField<M, ?>> getDeclaredFields() {
		return declaredFields;
	}

	/** Returns inherited + declared fields. */
	public List<MessageField<? super M, ?>> getFields() {
		return fields;
	}

	/** Returns whether this message is a form. */
	public boolean isForm() {
		return form;
	}

	// Message methods.

	/** Creates a new message instance. */
	public M newInstance() {
		return supplier.get();
	}

	@Override
	public M copy(final M src) {
		if (src == null) {
			return null;
		}

		M dst = newInstance();
		copy(src, dst);
		return dst;
	}

	/** Copies one message fields into another. */
	public void copy(final M src, final M dst) {
		for (MessageField<? super M, ?> field : getFields()) {
			field.copy(src, dst);
		}
	}

	// Native message format.

	@Override
	protected M fromNative(final Object object) throws Exception {
		if (object == null) {
			return null;
		}

		Map<?, ?> map = (Map<?, ?>) object;
		Enum<?> discriminatorValue = parseNativeDiscriminator(map);
		MessageType<M> polymorphicType = findSubtype(discriminatorValue);

		M message = polymorphicType.newInstance();
		for (MessageField<? super M, ?> field : polymorphicType.getFields()) {
			Object value = map.get(field.getName());
			field.setNative(message, value);
		}

		return message;
	}

	@Nullable
	@VisibleForTesting
	Enum<?> parseNativeDiscriminator(final Map<?, ?> map) throws Exception {
		MessageField<? super M, ?> discriminator = getDiscriminator();
		if (discriminator == null) {
			// This is not a polymorphic message.
			return null;
		}

		Object value = map.get(discriminator.getName());
		if (value == null) {
			// No polymorphic discriminator value.
			return null;
		}

		return (Enum<?>) discriminator.getType().fromNative(value);
	}

	@SuppressWarnings("unchecked")
	@VisibleForTesting
	MessageType<M> findSubtype(@Nullable final Enum<?> discriminatorValue) {
		if (discriminatorValue == null) {
			return this;
		}

		for (Supplier<MessageType<? extends M>> supplier : getSubtypes()) {
			MessageType<? extends M> subtype = supplier.get();
			if (discriminatorValue.equals(subtype.getDiscriminatorValue())) {
				return (MessageType<M>) subtype;
			}
		}

		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Map<String, Object> toNative(final M message) throws Exception {
		if (message == null) {
			return null;
		}

		// Mind polymorphic messages.
		MessageType<?> polymorphicType = message.metaType();
		Map<String, Object> result = Maps.newLinkedHashMap();

		for (MessageField field : polymorphicType.getFields()) {
			Object value = field.getNative(message);
			if (value == null) {
				continue;
			}

			result.put(field.getName(), value);
		}

		return result;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(getJavaClass().getSimpleName())
				.toString();
	}

	public static class Builder<M extends Message> {
		private Class<M> javaClass;
		private Supplier<M> supplier;
		private MessageType<? super M> base;
		private Enum<?> discriminatorValue;
		private final List<MessageField<M, ?>> declaredFields;
		private final List<Supplier<MessageType<? extends M>>> subtypes;
		private boolean form;

		private Builder() {
			declaredFields = Lists.newArrayList();
			subtypes = Lists.newArrayList();
		}

		public Builder<M> setJavaClass(final Class<M> javaClass) {
			this.javaClass = javaClass;
			return this;
		}

		public Builder<M> setSupplier(final Supplier<M> supplier) {
			this.supplier = supplier;
			return this;
		}

		public Builder<M> setBase(final MessageType<? super M> base) {
			this.base = base;
			return this;
		}

		public Builder<M> setForm(final boolean form) {
			this.form = form;
			return this;
		}

		public Builder<M> setDiscriminatorValue(final Enum<?> value) {
			this.discriminatorValue = value;
			return this;
		}

		public Builder<M> addField(final MessageField<M, ?> field) {
			declaredFields.add(field);
			return this;
		}

		public Builder<M> addSubtype(final Supplier<MessageType<? extends M>> subtype) {
			subtypes.add(subtype);
			return this;
		}

		public MessageType<M> build() {
			return new MessageType<M>(this);
		}
	}
}
