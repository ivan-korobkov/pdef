package pdef.descriptors;

import static com.google.common.base.Preconditions.*;
import com.google.common.collect.Lists;
import pdef.ImmutableSymbolTable;
import pdef.Message;
import pdef.SymbolTable;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.List;

public class MessageDescriptor extends AbstractDescriptor {
	private final Class<?> cls;
	private final Type baseType;

	private MessageDescriptor base;
	private SymbolTable<FieldDescriptor> fields;
	private SymbolTable<FieldDescriptor> declaredFields;

	public MessageDescriptor(final Class<?> cls, final DescriptorPool pool) {
		super(DescriptorType.MESSAGE, pool);
		this.cls = checkNotNull(cls);
		baseType = cls.getGenericSuperclass();
		checkArgument(Message.class.isAssignableFrom(cls));
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
		Field[] declared = cls.getDeclaredFields();
		List<FieldDescriptor> temp = Lists.newArrayList();
		for (Field field : declared) {
			FieldDescriptor fdescriptor = new FieldDescriptor(field);
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

	public MessageDescriptor getBase() {
		return base;
	}

	public SymbolTable<FieldDescriptor> getFields() {
		return fields;
	}

	public SymbolTable<FieldDescriptor> getDeclaredFields() {
		return declaredFields;
	}
}
