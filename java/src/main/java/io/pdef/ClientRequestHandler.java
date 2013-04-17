package io.pdef;

import com.google.common.util.concurrent.ListenableFuture;

public interface ClientRequestHandler {

	ListenableFuture<?> handle(Object request);
}
