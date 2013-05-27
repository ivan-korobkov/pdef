package io.pdef.fluent;

import com.google.common.base.Function;
import com.google.common.util.concurrent.ForwardingListenableFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import javax.annotation.Nullable;
import java.util.concurrent.Executor;

import static com.google.common.base.Preconditions.checkNotNull;

/** Wraps a guava listenable future. */
public class ForwardingFluentFuture<V> extends ForwardingListenableFuture<V>
		implements FluentFuture<V> {
	protected final ListenableFuture<V> delegate;
	@Nullable private final Executor executor;

	ForwardingFluentFuture(final ListenableFuture<V> delegate) {
		this(delegate, null);
	}

	ForwardingFluentFuture(final ListenableFuture<V> delegate, @Nullable final Executor executor) {
		this.delegate = checkNotNull(delegate);
		this.executor = executor;
	}

	@Override
	protected ListenableFuture<V> delegate() {
		return delegate;
	}

	@Override
	public <V1> FluentFuture<V1> then(final Function<V, V1> function) {
		checkNotNull(function);
		ListenableFuture<V1> transformed = executor == null
										   ? Futures.transform(delegate, function)
										   : Futures.transform(delegate, function, executor);
		return new ForwardingFluentFuture<V1>(transformed, executor);
	}

	@Override
	public FluentFuture<V> use(final Executor executor) {
		checkNotNull(executor);
		return new ForwardingFluentFuture<V>(delegate, executor);
	}

	@Override
	public FluentFuture<V> addCallback(final FutureCallback<V> callback) {
		checkNotNull(callback);
		if (executor == null) {
			Futures.addCallback(delegate, callback);
		} else {
			Futures.addCallback(delegate, callback, executor);
		}

		return this;
	}

	@Override
	public V getUnchecked() {
		return Futures.getUnchecked(delegate);
	}
}
