package io.pdef.types;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public abstract class MessageType<M extends Message> extends DataType<M> {
	private final Class<M> javaClass;

	protected MessageType(final Class<M> javaClass) {
		super(TypeEnum.MESSAGE);
		this.javaClass = checkNotNull(javaClass);
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(javaClass.getSimpleName())
				.toString();
	}

	public Class<M> getJavaClass() {
		return javaClass;
	}

	public abstract M newInstance();

	public abstract Descriptor<M> descriptor();

	public boolean isForm() {
		return descriptor().form;
	}

	public List<MessageField<? super M, ?>> getFields() {
		return descriptor().fields;
	}

	// Methods.

	@Override
	public M copy(final M src) {
		if (src == null) {
			return null;
		}

		M dst = newInstance();
		copy(src, dst);
		return dst;
	}

	public void copy(final M src, final M dst) {
		for (MessageField<? super M, ?> field : descriptor().getFields()) {
			field.copy(src, dst);
		}
	}

	// Native format.

	@Override
	protected M doParseNative(final Object object) throws Exception {
		if (object == null) {
			return null;
		}

		Map<?, ?> map = (Map<?, ?>) object;
		Enum<?> discriminatorValue = parseNativeDiscriminator(map);
		MessageType<M> type = findSubtype(discriminatorValue);
		Descriptor<M> descriptor = type.descriptor();

		M message = type.newInstance();
		for (MessageField<? super M, ?> field : descriptor.getFields()) {
			Object value = map.get(field.getName());
			field.setNative(message, value);
		}

		return message;
	}

	@Nullable
	@VisibleForTesting
	Enum<?> parseNativeDiscriminator(final Map<?, ?> map) throws Exception {
		MessageField<? super M, ?> discriminator = descriptor().getDiscriminator();
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

	@SuppressWarnings("unchecked")
	@VisibleForTesting
	MessageType<M> findSubtype(@Nullable final Enum<?> discriminatorValue) {
		if (discriminatorValue == null) {
			return this;
		}

		for (Supplier<MessageType<? extends M>> supplier : descriptor().getSubtypes()) {
			MessageType<? extends M> subtype = supplier.get();
			if (discriminatorValue.equals(subtype.descriptor().getDiscriminatorValue())) {
				return (MessageType<M>) subtype;
			}
		}

		return this;
	}

	@Override
	protected Map<String, Object> doToNative(final M message) throws Exception {
		if (message == null) {
			return null;
		}

		// Mind polymorphic messages.
		@SuppressWarnings("unchecked")
		MessageType<Message> type = (MessageType<Message>) message.type();
		Descriptor<Message> descriptor = type.descriptor();

		Map<String, Object> result = Maps.newLinkedHashMap();
		for (MessageField<? super M, ?> field : descriptor.getFields()) {
			Object value = field.getNative(message);
			if (value == null) {
				continue;
			}

			result.put(field.getName(), value);
		}

		return result;
	}

	public static class Descriptor<M extends Message> {
		private final MessageType<? super M> base;
		private final List<MessageField<M, ?>> declaredFields;
		private final List<MessageField<? super M, ?>> fields;

		private final Enum<?> discriminatorValue;
		private final MessageField<? super M, ?> discriminator;
		private final List<Supplier<MessageType<? extends M>>> subtypes;

		private final boolean form;

		public Descriptor(final DescriptorBuilder<M> builder) {
			base = builder.base;

			declaredFields = ImmutableList.copyOf(builder.declaredFields);
			fields = joinFields(declaredFields, base);

			discriminator = findDiscriminator(fields);
			discriminatorValue = builder.discriminatorValue;
			subtypes = ImmutableList.copyOf(builder.subtypes);

			form = builder.form;
		}

		private static <M extends Message> ImmutableList<MessageField<? super M, ?>> joinFields(
				final Iterable<MessageField<M, ?>> declaredFields,
				@Nullable final MessageType<? super M> base) {
			ImmutableList.Builder<MessageField<? super M, ?>> result = ImmutableList.builder();
			if (base != null) {
				result.addAll(base.descriptor().getFields());
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

		public MessageType getBase() {
			return base;
		}

		public boolean isForm() {
			return form;
		}

		public Enum<?> getDiscriminatorValue() {
			return discriminatorValue;
		}

		public MessageField<? super M, ?> getDiscriminator() {
			return discriminator;
		}

		public List<Supplier<MessageType<? extends M>>> getSubtypes() {
			return subtypes;
		}

		public List<MessageField<M, ?>> getDeclaredFields() {
			return declaredFields;
		}

		public List<MessageField<? super M, ?>> getFields() {
			return fields;
		}
	}

	public static class DescriptorBuilder<M extends Message> {
		private MessageType<? super M> base;
		private Enum<?> discriminatorValue;
		private final List<MessageField<M, ?>> declaredFields;
		private final List<Supplier<MessageType<? extends M>>> subtypes;
		private boolean form;

		public DescriptorBuilder() {
			declaredFields = Lists.newArrayList();
			subtypes = Lists.newArrayList();
		}

		public DescriptorBuilder<M> setBase(final MessageType<? super M> base) {
			this.base = base;
			return this;
		}

		public DescriptorBuilder<M> setForm(final boolean form) {
			this.form = form;
			return this;
		}

		public DescriptorBuilder<M> setDiscriminatorValue(final Enum<?> value) {
			this.discriminatorValue = value;
			return this;
		}

		public DescriptorBuilder<M> addField(final MessageField<M, ?> field) {
			declaredFields.add(field);
			return this;
		}

		public DescriptorBuilder<M> addSubtype(final Supplier<MessageType<? extends M>> subtype) {
			subtypes.add(subtype);
			return this;
		}

		public Descriptor<M> build() {
			return new Descriptor<M>(this);
		}
	}
}
