package pdef.fixtures;

import pdef.ImmutableSymbolTable;
import pdef.Message;
import pdef.SymbolTable;
import pdef.descriptors.AbstractFieldDescriptor;
import pdef.descriptors.AbstractMessageDescriptor;
import pdef.descriptors.FieldDescriptor;
import pdef.descriptors.MessageDescriptor;

public class Entity implements Message {
	private Id id;

	public Id getId() {
		return id;
	}

	public Entity setId(final Id id) {
		this.id = id;
		return this;
	}

	@Override
	public MessageDescriptor getDescriptor() {
		return Descriptor.getInstance();
	}

	public static class Descriptor extends AbstractMessageDescriptor {
		private static final Descriptor INSTANCE = new Descriptor();
		public static Descriptor getInstance() {
			INSTANCE.link();
			return INSTANCE;
		}

		private ImmutableSymbolTable<FieldDescriptor> declaredFields;
		private ImmutableSymbolTable<FieldDescriptor> fields;

		private Descriptor() {
			super(Entity.class);
		}

		@Override
		public SymbolTable<FieldDescriptor> getDeclaredFields() {
			return declaredFields;
		}

		@Override
		public SymbolTable<FieldDescriptor> getFields() {
			return fields;
		}

		@Override
		protected void doLink() {
			declaredFields = ImmutableSymbolTable.<FieldDescriptor>of(
					new AbstractFieldDescriptor("id", Id.Descriptor.getInstance()) {
						@Override
						public Object get(final Message message) {
							return ((Entity) message).getId();
						}

						@Override
						public void set(final Message message, final Object value) {
							((Entity) message).setId((Id) value);
						}
					}
			);
			fields = declaredFields;
		}
	}
}
