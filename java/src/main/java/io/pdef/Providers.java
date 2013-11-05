package io.pdef;

public class Providers {
	private Providers() {}

	/**
	 * Creates a provider which always returns the same instance.
	 */
	public static <T> Provider<T> ofInstance(final T instance) {
		return new Provider<T>() {
			@Override
			public T get() {
				return instance;
			}
		};
	}
}
