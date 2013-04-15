package io.pdef.fixtures;

import com.google.common.util.concurrent.ListenableFuture;
import io.pdef.Interface;

public interface Calc extends Interface {

	ListenableFuture<Integer> sum(int i0, int i1);
}
