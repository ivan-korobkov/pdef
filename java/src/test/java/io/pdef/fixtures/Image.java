package io.pdef.fixtures;

import io.pdef.Message;

public class Image implements Message {
	private String url;
	private User owner;
	private long createdAt;

	public Image() {}

	public Image(final Builder builder) {
		url = builder.url;
		owner = builder.owner;
		createdAt = builder.createdAt;
	}

	public String getUrl() {
		return url;
	}

	public User getOwner() {
		return owner;
	}

	public long getCreatedAt() {
		return createdAt;
	}

	public static class Builder implements Message.Builder {
		private String url;
		private User owner;
		private long createdAt;

		public Builder setUrl(final String url) {
			this.url = url;
			return this;
		}

		public Builder setOwner(final User owner) {
			this.owner = owner;
			return this;
		}

		public Builder setCreatedAt(final long createdAt) {
			this.createdAt = createdAt;
			return this;
		}

		@Override
		public Image build() {
			return new Image(this);
		}
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Image image = (Image) o;

		if (createdAt != image.createdAt) return false;
		if (owner != null ? !owner.equals(image.owner) : image.owner != null) return false;
		if (url != null ? !url.equals(image.url) : image.url != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = url != null ? url.hashCode() : 0;
		result = 31 * result + (owner != null ? owner.hashCode() : 0);
		result = 31 * result + (int) (createdAt ^ (createdAt >>> 32));
		return result;
	}
}
