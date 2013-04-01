package pdef;

public interface FieldDescriptor extends Symbol, Bindable<FieldDescriptor> {

	TypeDescriptor getType();

	boolean isTypeField();

	Object get(Message message);

	Object get(Message.Builder builder);

	boolean isSet(Message message);

	boolean isSet(Message.Builder builder);

	void set(Message.Builder builder, Object value);

	void clear(Message.Builder builder);
}
