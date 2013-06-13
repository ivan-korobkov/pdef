package io.pdef.io;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.List;

public class Readers {
	private Readers() {}

	public static Reader<String> stringReader() {
		return new Reader<String>() {
			@Override
			public String read(final Input input) {
				return input.asValue().getString();
			}
		};
	}

	public static <T> Reader<List<T>> list(final Reader<? extends T> reader) {
		return new Reader<List<T>>() {
			@Override
			public List<T> read(final Input input) {
				List<T> result = Lists.newArrayList();

				InputList in = input.asList();
				for (InputValue v : in) {
					T element = reader.read(v);
					if (element == null) continue;
					result.add(element);
				}

				return ImmutableList.copyOf(result);
			}
		};
	}
}
