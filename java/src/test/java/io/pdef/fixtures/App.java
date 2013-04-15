package io.pdef.fixtures;

import com.google.common.util.concurrent.ListenableFuture;
import io.pdef.Interface;

public interface App extends Interface {

	Calc calc();

	ListenableFuture<String> echo(String text);
}
