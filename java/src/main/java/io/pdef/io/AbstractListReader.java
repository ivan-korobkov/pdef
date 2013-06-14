package io.pdef.io;

import java.util.List;

public abstract class AbstractListReader<T> implements Reader.ListReader<T> {
	@Override
	public List<T> get(final Input input) {
		return input.getList(this);
	}
}
