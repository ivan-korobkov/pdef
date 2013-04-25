package io.pdef.fixtures;

import com.google.common.util.concurrent.ListenableFuture;
import io.pdef.Interface;
import io.pdef.Name;

public interface App extends Interface {

	Calc calc();

	ListenableFuture<String> echoText(@Name("text") String text);
}
