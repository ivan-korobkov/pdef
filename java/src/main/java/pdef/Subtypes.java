package pdef;

import javax.annotation.Nullable;
import java.util.Map;

public interface Subtypes {

	/** Returns the type of the message which holds this tree. */
	Enum<?> getType();

	/** Returns the field which holds the type. */
	FieldDescriptor getField();

	/** Returns this message subtype. */
	@Nullable
	MessageDescriptor getSubtype(Object object);

	/** Returns a subtype map which includes this message and all its subclasses. */
	Map<Enum<?>, MessageDescriptor> getMap();

	Builder subclass(Enum<?> subtype);

	public static interface Builder {

		Builder setType(Enum<?> type);

		Builder setField(FieldDescriptor field);

		Builder put(Enum<?> enumType, final MessageDescriptor message);

		Builder putAll(Map<? extends Enum<?>, ? extends MessageDescriptor> map);

		Subtypes build();
	}
}
