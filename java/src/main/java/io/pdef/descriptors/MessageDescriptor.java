package io.pdef.descriptors;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.*;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.pdef.Message;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * MessageDescriptor is a descriptor for Pdef messages.
 * */
public class MessageDescriptor<M extends Message> extends DataDescriptor<M> {
	private final Supplier<M> supplier;
	private final MessageDescriptor<? super M> base;
	private final List<FieldDescriptor<M, ?>> declaredFields;
	private final List<FieldDescriptor<? super M, ?>> fields;
	private final Map<String, FieldDescriptor<? super M, ?>> fieldMap;

	private final Enum<?> discriminatorValue;
	private final FieldDescriptor<? super M, ?> discriminator;
	private final List<Supplier<MessageDescriptor<? extends M>>> subtypeSuppliers;
	private List<MessageDescriptor<? extends M>> subtypes;

	private final boolean form;

	private MessageDescriptor(final Builder<M> builder) {
		super(TypeEnum.MESSAGE, builder.javaClass);
		this.supplier = checkNotNull(builder.supplier);

		base = builder.base;

		declaredFields = ImmutableList.copyOf(builder.declaredFields);
		fields = joinFields(declaredFields, base);
		fieldMap = Maps.uniqueIndex(fields, new Function<FieldDescriptor<? super M, ?>, String>() {
			@Override
			public String apply(final FieldDescriptor<? super M, ?> input) {
				return input.getName();
			}
		});

		discriminator = findDiscriminator(fields);
		discriminatorValue = builder.discriminatorValue;
		subtypeSuppliers = ImmutableList.copyOf(builder.subtypes);

		form = builder.form;
	}

	public static <M extends Message> Builder<M> builder() {
		return new Builder<M>();
	}

	/** Returns a base descriptor. */
	public MessageDescriptor getBase() {
		return base;
	}

	/** Returns true when a discriminator field is present. */
	public boolean isPolymorphic() {
		return discriminator != null;
	}

	/** Returns a discriminator value. */
	public Enum<?> getDiscriminatorValue() {
		return discriminatorValue;
	}

	/** Returns a discriminator field. */
	public FieldDescriptor<? super M, ?> getDiscriminator() {
		return discriminator;
	}

	/** Returns a list of subtype descriptors. */
	public List<MessageDescriptor<? extends M>> getSubtypes() {
		if (subtypes != null) {
			return subtypes;
		}

		ImmutableList.Builder<MessageDescriptor<? extends M>> builder = ImmutableList.builder();
		for (Supplier<MessageDescriptor<? extends M>> supplier : subtypeSuppliers) {
			MessageDescriptor<? extends M> subtype = supplier.get();
			builder.add(subtype);
		}

		return (subtypes = builder.build());
	}

	/** Returns an immutable list of declared fields. */
	public List<FieldDescriptor<M, ?>> getDeclaredFields() {
		return declaredFields;
	}

	/** Returns an immutable list of inherited and declared fields. */
	public List<FieldDescriptor<? super M, ?>> getFields() {
		return fields;
	}

	/** Returns a immutable map of names to fields. */
	public Map<String, FieldDescriptor<? super M, ?>> getFieldMap() {
		return fieldMap;
	}

	/** Returns whether this message is a form. */
	public boolean isForm() {
		return form;
	}

	/** Finds a subtype by a discriminator value or returns this type. */
	public MessageDescriptor<M> findSubtypeOrThis(@Nullable final Object discriminatorValue) {
		if (discriminatorValue == null) {
			return this;
		}

		for (MessageDescriptor<? extends M> subtype : getSubtypes()) {
			if (discriminatorValue.equals(subtype.getDiscriminatorValue())) {
				@SuppressWarnings("unchecked")
				MessageDescriptor<M> result = (MessageDescriptor<M>) subtype;
				return result;
			}
		}

		return this;
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
		for (FieldDescriptor<? super M, ?> field : getFields()) {
			field.copy(src, dst);
		}
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
		private MessageDescriptor<? super M> base;
		private Enum<?> discriminatorValue;
		private final List<FieldDescriptor<M, ?>> declaredFields;
		private final List<Supplier<MessageDescriptor<? extends M>>> subtypes;
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

		public Builder<M> setBase(final MessageDescriptor<? super M> base) {
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

		public Builder<M> addField(final FieldDescriptor<M, ?> field) {
			declaredFields.add(field);
			return this;
		}

		public Builder<M> addSubtype(final Supplier<MessageDescriptor<? extends M>> subtype) {
			subtypes.add(subtype);
			return this;
		}

		public MessageDescriptor<M> build() {
			return new MessageDescriptor<M>(this);
		}
	}

	private static <M extends Message> ImmutableList<FieldDescriptor<? super M, ?>> joinFields(
			final Iterable<FieldDescriptor<M, ?>> declaredFields,
			@Nullable final MessageDescriptor<? super M> base) {
		ImmutableList.Builder<FieldDescriptor<? super M, ?>> result = ImmutableList.builder();
		if (base != null) {
			result.addAll(base.getFields());
		}
		result.addAll(declaredFields);
		return result.build();
	}

	private static <M> FieldDescriptor<? super M, ?> findDiscriminator(
			final Iterable<FieldDescriptor<? super M, ?>> fields) {
		for (FieldDescriptor<? super M, ?> field : fields) {
			if (field.isDiscriminator()) {
				return field;
			}
		}
		return null;
	}
}
