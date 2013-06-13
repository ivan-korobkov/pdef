package io.pdef.io;

import java.util.List;

public interface OutputList {
	<T> void write(List<T> list, Writer<? extends T> elementWriter);
}
