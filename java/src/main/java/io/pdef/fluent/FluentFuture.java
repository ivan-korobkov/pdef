package io.pdef.fluent;

import com.google.common.base.Function;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;

public interface FluentFuture<V> extends ListenableFuture<V> {

	FluentFuture<V> use(Executor executor);

	<V1> FluentFuture<V1> then(Function<V, V1> function);

	FluentFuture<V> addCallback(FutureCallback<V> callback);

	V getUnchecked();
}
