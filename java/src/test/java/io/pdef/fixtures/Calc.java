package io.pdef.fixtures;

import com.google.common.util.concurrent.ListenableFuture;
import io.pdef.Interface;
import io.pdef.annotations.Name;

public interface Calc extends Interface {

	ListenableFuture<Integer> sum(@Name("i0") int i0, @Name("i1") int i1);
}
