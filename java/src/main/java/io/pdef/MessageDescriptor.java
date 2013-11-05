package io.pdef;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface MessageDescriptor<M extends Message> extends DataDescriptor<M> {
	/**
	 * Returns this message base descriptor or {@literal null}.
	 */
	@Nullable
	MessageDescriptor getBase();

	/**
	 * Returns a list of fields declared in this message (not in a base) or an empty list.
	 */
	List<FieldDescriptor<M, ?>> getDeclaredFields();

	/**
	 * Returns a list of field declared in this message and in its base or an empty list.
	 */
	List<FieldDescriptor<? super M, ?>> getFields();

	/**
	 * Returns a map of field names to fields or an empty map.
	 */
	Map<String, FieldDescriptor<? super M, ?>> getFieldMap();

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
	 * Returns this message subtypes (including their subtypes) or an empty set.
	 */
	Set<MessageDescriptor<? extends M>> getSubtypes();

	/**
	 * Finds a subtype descriptor by an enum value and returns it or this message descriptor.
	 */
	MessageDescriptor<M> findSubtypeOrThis(@Nullable Object discriminatorValue);

	/**
	 * Returns whether this message is a form.
	 */
	boolean isForm();

	/**
	 * Creates a new message instance.
	 */
	M newInstance();

	/**
	 * Deeply copies present fields from one message into another.
	 */
	void copy(M src, M dst);
}
