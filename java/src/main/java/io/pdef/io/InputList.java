package io.pdef.io;

import java.util.List;

public interface InputList extends Input {
	<T> List<T> readUsing(Reader<T> elementReader);
}
