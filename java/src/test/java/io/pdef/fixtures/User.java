package io.pdef.fixtures;

import java.util.List;

public class User extends GenericObject {
	private String name;
	private Image avatar;
	private List<Image> photos;

	public User() {}

	public User(final Builder builder) {
		super(builder);
		name = builder.name;
		avatar = builder.avatar;
		photos = builder.photos;
	}

	public String getName() {
		return name;
	}

	public Image getAvatar() {
		return avatar;
	}

	public List<Image> getPhotos() {
		return photos;
	}

	public static class Builder extends GenericObject.Builder {
		private String name;
		private Image avatar;
		private List<Image> photos;

		public Builder() {
			this.setType(ObjectType.USER);
		}

		public Builder setName(final String name) {
			this.name = name;
			return this;
		}

		public Builder setAvatar(final Image avatar) {
			this.avatar = avatar;
			return this;
		}

		public Builder setPhotos(final List<Image> photos) {
			this.photos = photos;
			return this;
		}

		@Override
		public User build() {
			return new User(this);
		}
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;

		User user = (User) o;

		if (avatar != null ? !avatar.equals(user.avatar) : user.avatar != null) return false;
		if (name != null ? !name.equals(user.name) : user.name != null) return false;
		if (photos != null ? !photos.equals(user.photos) : user.photos != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (name != null ? name.hashCode() : 0);
		result = 31 * result + (avatar != null ? avatar.hashCode() : 0);
		result = 31 * result + (photos != null ? photos.hashCode() : 0);
		return result;
	}
}
