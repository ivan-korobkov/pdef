package io.pdef.descriptors;

import io.pdef.Message;
import io.pdef.Provider;

import javax.annotation.Nullable;
import java.util.*;

/**
 * MessageDescriptor is a descriptor for Pdef messages.
 * */
public class MessageDescriptor<M extends Message> extends DataDescriptor<M> {
	private final Provider<M> provider;
	private final MessageDescriptor<? super M> base;
	private final List<FieldDescriptor<M, ?>> declaredFields;
	private final List<FieldDescriptor<? super M, ?>> fields;
	private final Map<String, FieldDescriptor<? super M, ?>> fieldMap;

	private final Enum<?> discriminatorValue;
	private final FieldDescriptor<? super M, ?> discriminator;
	private final List<Provider<MessageDescriptor<? extends M>>> subtypeProviders;
	private Set<MessageDescriptor<? extends M>> subtypes;

	private final boolean form;

	private MessageDescriptor(final Builder<M> builder) {
		super(TypeEnum.MESSAGE, builder.javaClass);
		provider = builder.provider;
		base = builder.base;

		declaredFields = Collections.unmodifiableList(
				new ArrayList<FieldDescriptor<M, ?>>(builder.declaredFields));
		fields = joinFields(declaredFields, base);
		fieldMap = fieldMap(fields);

		discriminator = findDiscriminator(fields);
		discriminatorValue = builder.discriminatorValue;
		subtypeProviders = Collections.unmodifiableList(
				new ArrayList<Provider<MessageDescriptor<? extends M>>>(builder.subtypes));

		form = builder.form;

		if (provider == null) throw new NullPointerException("provider");
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
	public Set<MessageDescriptor<? extends M>> getSubtypes() {
		if (subtypes != null) {
			return subtypes;
		}

		Set<MessageDescriptor<? extends M>> set = new HashSet<MessageDescriptor<? extends M>>();
		for (Provider<MessageDescriptor<? extends M>> provider : subtypeProviders) {
			MessageDescriptor<? extends M> subtype = provider.get();
			set.add(subtype);
		}

		return (subtypes = Collections.unmodifiableSet(set));
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
		return provider.get();
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
		return "MessageDescriptor{" + getJavaClass().getSimpleName() + '}';
	}

	public static class Builder<M extends Message> {
		private Class<M> javaClass;
		private Provider<M> provider;
		private MessageDescriptor<? super M> base;
		private Enum<?> discriminatorValue;
		private final List<FieldDescriptor<M, ?>> declaredFields;
		private final List<Provider<MessageDescriptor<? extends M>>> subtypes;
		private boolean form;

		private Builder() {
			declaredFields = new ArrayList<FieldDescriptor<M, ?>>();
			subtypes = new ArrayList<Provider<MessageDescriptor<? extends M>>>();
		}

		public Builder<M> setJavaClass(final Class<M> javaClass) {
			this.javaClass = javaClass;
			return this;
		}

		public Builder<M> setProvider(final Provider<M> provider) {
			this.provider = provider;
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

		public Builder<M> addSubtype(final Provider<MessageDescriptor<? extends M>> subtype) {
			subtypes.add(subtype);
			return this;
		}

		public MessageDescriptor<M> build() {
			return new MessageDescriptor<M>(this);
		}
	}

	private static <M extends Message> List<FieldDescriptor<? super M, ?>> joinFields(
			final List<FieldDescriptor<M, ?>> declaredFields,
			@Nullable final MessageDescriptor<? super M> base) {
		List<FieldDescriptor<? super M, ?>> result = new ArrayList<FieldDescriptor<? super M, ?>>();
		if (base != null) {
			result.addAll(base.getFields());
		}
		result.addAll(declaredFields);
		return Collections.unmodifiableList(result);
	}

	private static <M extends Message> Map<String, FieldDescriptor<? super M, ?>> fieldMap(
			final List<FieldDescriptor<? super M, ?>> iterable) {
		Map<String, FieldDescriptor<? super M, ?>> map = new HashMap<String,
				FieldDescriptor<? super M, ?>>();
		for (FieldDescriptor<? super M, ?> field : iterable) {
			map.put(field.getName(), field);
		}
		return Collections.unmodifiableMap(map);
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
