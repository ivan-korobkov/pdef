package io.pdef.fluent;

import com.google.common.util.concurrent.SettableFuture;

/** Settable fluent future. */
public class FluentPromise<V> extends ForwardingFluentFuture<V> {
	public static <V> FluentPromise<V> create() {
		return new FluentPromise<V>();
	}

	FluentPromise() {
		super(SettableFuture.<V>create());
	}

	public boolean set(final V value) {
		return delegate().set(value);
	}

	public FluentPromise<V> setException(final Exception e) {
		delegate().setException(e);
		return this;
	}

	@Override
	protected SettableFuture<V> delegate() {
		return (SettableFuture<V>) super.delegate();
	}
}
