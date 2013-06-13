package io.pdef.io;

import java.util.List;

public class Writers {

	public static <T> Writer<List<T>> list(final Writer<? extends T> writer) {
		return new Writer<List<T>>() {
			@Override
			public void write(final List<T> object, final Output output) {
				if (object == null) {
					output.writeNull();
					return;
				}

				OutputList list = output.asList();
				list.write(object, writer);
			}
		};
	}
}
