package io.pdef.fluent;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import static com.google.common.base.Preconditions.checkNotNull;

public class FluentFutures {
	private FluentFutures() {}

	public static <V> FluentFuture<V> of(final V value) {
		return wrap(Futures.immediateFuture(value));
	}

	public static <V> FluentFuture<V> failed(final Exception e) {
		return wrap(Futures.<V>immediateFailedFuture(e));
	}

	public static <V> FluentFuture<V> wrap(final ListenableFuture<V> listenableFuture) {
		checkNotNull(listenableFuture);
		return new ForwardingFluentFuture<V>(listenableFuture);
	}
}
