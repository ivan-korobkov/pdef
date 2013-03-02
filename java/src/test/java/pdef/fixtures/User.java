package pdef.fixtures;

import pdef.ImmutableSymbolTable;
import pdef.Message;
import pdef.SymbolTable;
import pdef.generated.GeneratedFieldDescriptor;
import pdef.generated.GeneratedMessageDescriptor;
import pdef.FieldDescriptor;
import pdef.MessageDescriptor;
import pdef.provided.NativeValueDescriptors;

public class User extends Entity {
	private Image image;
	private Weighted<Image> aura;
	private RootNode<Integer> root;

	protected User(final Builder builder) {
		super(builder);
		this.image = builder.getImage();
		this.aura = builder.getAura();
		this.root = builder.getRoot();
	}

	public Image getImage() { return image; }

	public Weighted<Image> getAura() { return aura; }

	public RootNode<Integer> getRoot() { return root; }

	@Override
	public MessageDescriptor getDescriptor() { return Descriptor.getInstance(); }

	public static class Builder extends Entity.Builder {
		private Image image;
		private Weighted<Image> aura;
		private RootNode<Integer> root;

		public Image getImage() { return image; }

		public Builder setImage(final Image image) { this.image = image; return this; }

		public Weighted<Image> getAura() { return aura; }

		public Builder setAura(final Weighted<Image> aura) { this.aura = aura; return this; }

		public RootNode<Integer> getRoot() { return root; }

		public Builder setRoot(final RootNode<Integer> root) { this.root = root; return this; }

		@Override
		public Builder setId(final Id id) { super.setId(id); return this; }

		@Override
		public User build() { return new User(this); }
	}

	public static class Descriptor extends GeneratedMessageDescriptor {
		private static final Descriptor instance = new Descriptor();
		public static Descriptor getInstance() { instance.link(); return instance; }

		private MessageDescriptor base;
		private SymbolTable<FieldDescriptor> declaredFields;
		private FieldDescriptor imageField;
		private FieldDescriptor auraField;
		private FieldDescriptor rootField;

		Descriptor() {
			super(User.class);
		}

		@Override
		public MessageDescriptor getBase() { return base; }

		@Override
		public SymbolTable<FieldDescriptor> getDeclaredFields() { return declaredFields; }

		@Override
		protected void init() {
			base = Entity.Descriptor.getInstance();
			imageField = new GeneratedFieldDescriptor("image", Image.Descriptor.getInstance()) {
				@Override
				public Object get(final Message message) {
					return ((User) message).getImage();
				}

				@Override
				public Object get(final Message.Builder builder) {
					return ((Builder) builder).getImage();
				}

				@Override
				public void set(final Message.Builder builder, final Object value) {
					((Builder) builder).setImage((Image) value);
				}
			};
			auraField = new GeneratedFieldDescriptor("aura",
					Weighted.Descriptor.getInstance().parameterize(Image.Descriptor.getInstance())) {
				@Override
				public Object get(final Message message) {
					return ((User) message).getAura();
				}

				@Override
				public Object get(final Message.Builder builder) {
					return ((Builder) builder).getImage();
				}

				@Override
				public void set(final Message.Builder builder, final Object value) {
					((Builder) builder).setImage((Image) value);
				}
			};

			rootField = new GeneratedFieldDescriptor("root",
					RootNode.Descriptor.getInstance().parameterize(NativeValueDescriptors.getInt32())) {
				@Override
				public Object get(final Message message) {
					return ((User) message).getRoot();
				}

				@Override
				public Object get(final Message.Builder builder) {
					return ((Builder) builder).getRoot();
				}

				@Override
				@SuppressWarnings("unchecked")
				public void set(final Message.Builder builder, final Object value) {
					((Builder) builder).setRoot((RootNode<Integer>) value);
				}
			};

			declaredFields = ImmutableSymbolTable.of(imageField, auraField, rootField);
		}
	}
}
