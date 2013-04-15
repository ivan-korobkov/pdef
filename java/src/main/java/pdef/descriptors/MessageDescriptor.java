package pdef.descriptors;

import com.google.common.collect.Lists;
import pdef.ImmutableSymbolTable;
import pdef.Message;
import pdef.SymbolTable;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

public class MessageDescriptor extends AbstractDescriptor {
	private final Type baseType;

	private MessageDescriptor base;
	private SymbolTable<FieldDescriptor> fields;
	private SymbolTable<FieldDescriptor> declaredFields;

	public MessageDescriptor(final Class<?> messageType, final DescriptorPool pool) {
		super(messageType, DescriptorType.MESSAGE, pool);
		Type superclass = messageType.getGenericSuperclass();
		baseType = superclass == Object.class ? null : superclass;
		checkArgument(Message.class.isAssignableFrom(messageType));
	}

	@Override
	public Class<?> getJavaType() {
		return (Class<?>) super.getJavaType();
	}

	public Type getBaseType() {
		return baseType;
	}

	public MessageDescriptor getBase() {
		return base;
	}

	public SymbolTable<FieldDescriptor> getFields() {
		return fields;
	}

	public SymbolTable<FieldDescriptor> getDeclaredFields() {
		return declaredFields;
	}

	@Override
	protected void doLink() {
		linkBase();
		linkDeclaredFields();
		linkFields();
	}

	private void linkBase() {
		base = baseType == null ? null : (MessageDescriptor) pool.getDescriptor(baseType);
	}

	private void linkDeclaredFields() {
		Field[] declared = getJavaType().getDeclaredFields();
		List<FieldDescriptor> temp = Lists.newArrayList();
		for (Field field : declared) {
			FieldDescriptor fdescriptor = new FieldDescriptor(field, pool);
			temp.add(fdescriptor);
		}
		declaredFields = ImmutableSymbolTable.copyOf(temp);
	}

	private void linkFields() {
		if (base == null) {
			fields = declaredFields;
		} else {
			fields = ImmutableSymbolTable.<FieldDescriptor>of()
					.merge(base.getFields())
					.merge(declaredFields);
		}
	}
}
