package pdef.fixtures;

import pdef.ImmutableSymbolTable;
import pdef.Message;
import pdef.SymbolTable;
import pdef.descriptors.BaseFieldDescriptor;
import pdef.descriptors.BaseMessageDescriptor;
import pdef.descriptors.FieldDescriptor;
import pdef.descriptors.MessageDescriptor;

public class User extends Entity {
	private Image image;
	private Weighted<Image> aura;
	private RootNode<Integer> root;

	public Image getImage() {
		return image;
	}

	public User setImage(final Image image) {
		this.image = image;
		return this;
	}

	public Weighted<Image> getAura() {
		return aura;
	}

	public void setAura(final Weighted<Image> aura) {
		this.aura = aura;
	}

	public RootNode<Integer> getRoot() {
		return root;
	}

	public void setRoot(final RootNode<Integer> root) {
		this.root = root;
	}

	@Override
	public MessageDescriptor getDescriptor() {
		return Descriptor.getInstance();
	}

	public static class Descriptor extends BaseMessageDescriptor {
		private static final Descriptor INSTANCE = new Descriptor();

		public static Descriptor getInstance() {
			INSTANCE.link();
			return INSTANCE;
		}

		private MessageDescriptor base;
		private SymbolTable<FieldDescriptor> declaredFields;

		Descriptor() {
			super(User.class);
		}

		@Override
		public MessageDescriptor getBase() {
			return base;
		}

		@Override
		public SymbolTable<FieldDescriptor> getDeclaredFields() {
			return declaredFields;
		}

		@Override
		protected void init() {
			base = Entity.Descriptor.getInstance();
			declaredFields = ImmutableSymbolTable.<FieldDescriptor>of(
					new BaseFieldDescriptor("avatar", Image.Descriptor.getInstance()) {
						@Override
						public Object get(final Message message) {
							return ((User) message).getImage();
						}

						@Override
						public void set(final Message message, final Object value) {
							((User) message).setImage((Image) value);
						}
					},

					new BaseFieldDescriptor("aura", Weighted.Descriptor.getInstance()
							.parameterize(Image.Descriptor.getInstance())) {
						@Override
						public Object get(final Message message) {
							return ((User) message).getAura();
						}

						@Override
						@SuppressWarnings("unchecked")
						public void set(final Message message, final Object value) {
							((User) message).setAura((Weighted<Image>) value);
						}
					},

					new BaseFieldDescriptor("root", RootNode.Descriptor.getInstance()
							.parameterize(IntDescriptor.getInstance())) {
						@Override
						public Object get(final Message message) {
							return ((User) message).getRoot();
						}

						@Override
						@SuppressWarnings("unchecked")
						public void set(final Message message, final Object value) {
							((User) message).setRoot((RootNode<Integer>) value);
						}
					}
			);
		}
	}
}
