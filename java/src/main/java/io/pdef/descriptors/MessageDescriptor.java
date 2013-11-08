package io.pdef.descriptors;

import io.pdef.Message;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

public interface MessageDescriptor<M extends Message> extends DataTypeDescriptor<M> {
	/**
	 * Returns this message base descriptor or {@literal null}.
	 */
	@Nullable
	MessageDescriptor getBase();

	/**
	 * Returns a field descriptor by its name or {@literal null}.
	 */
	@Nullable
	FieldDescriptor<? super M, ?> getField(String name);

	/**
	 * Returns a list of field declared in this message and in its base or an empty list.
	 */
	List<FieldDescriptor<? super M, ?>> getFields();

	/**
	 * Returns a list of fields declared in this message (not in a base) or an empty list.
	 */
	List<FieldDescriptor<M, ?>> getDeclaredFields();

	/**
	 * Returns whether this message has a discriminator field.
	 */
	boolean isPolymorphic();

	/**
	 * Returns this message discriminator field value if polymorphic or {@literal null}.
	 */
	@Nullable
	Enum<?> getDiscriminatorValue();

	/**
	 * Returns this message discriminator field if polymorphic or {@literal null}.
	 */
	@Nullable
	FieldDescriptor<? super M, ?> getDiscriminator();

	/**
	 * Returns a subtype descriptor by its discriminator value or {@literal null}.
	 */
	MessageDescriptor<? extends M> getSubtype(@Nullable Enum<?> discriminatorValue);

	/**
	 * Returns this message subtypes (including their subtypes) or an empty set.
	 */
	Set<MessageDescriptor<? extends M>> getSubtypes();

	/**
	 * Returns whether this message is a form.
	 */
	boolean isForm();

	/**
	 * Creates a new message getInstance.
	 */
	M newInstance();

	/**
	 * Deeply copies present fields from one message into another.
	 */
	void copy(M src, M dst);
}
