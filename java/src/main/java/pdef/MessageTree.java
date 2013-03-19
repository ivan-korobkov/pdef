package pdef;

import java.util.Map;

public interface MessageTree {

	/** Returns the type of the message which holds this tree. */
	EnumType getType();

	/** Returns the field which holds the type. */
	FieldDescriptor getField();

	/** Returns a subtype map which includes this message and all its subclasses. */
	Map<EnumType, MessageDescriptor> getMap();

	Builder subtreeBuilder(EnumType subtype);

	public static interface Builder {

		Builder setType(EnumType type);

		Builder setField(FieldDescriptor field);

		Builder put(EnumType enumType, final MessageDescriptor message);

		Builder putAll(Map<? extends EnumType, ? extends MessageDescriptor> map);

		MessageTree build();
	}
}
