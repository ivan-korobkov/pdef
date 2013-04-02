package pdef;

import javax.annotation.Nullable;
import java.util.Map;

public interface Subtypes {

	/** Returns the type of the message which holds this tree. */
	EnumType getType();

	/** Returns the field which holds the type. */
	FieldDescriptor getField();

	/** Returns this message subtype. */
	@Nullable
	MessageDescriptor getSubtype(Object object);

	/** Returns a subtype map which includes this message and all its subclasses. */
	Map<EnumType, MessageDescriptor> getMap();

	Builder subclass(EnumType subtype);

	public static interface Builder {

		Builder setType(EnumType type);

		Builder setField(FieldDescriptor field);

		Builder put(EnumType enumType, final MessageDescriptor message);

		Builder putAll(Map<? extends EnumType, ? extends MessageDescriptor> map);

		Subtypes build();
	}
}
