package io.pdef;

import com.google.common.collect.ImmutableList;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import rx.util.functions.Func1;

public class ObservableValueTest {

	@Test
	public void testImmediate() throws Exception {
		String s = ObservableValue.immediate("hello, world").single();
		assertEquals("hello, world", s);
	}

	@Test
	public void testSet() throws Exception {
		ObservableValue<String> value = ObservableValue.async();
		Iterable<String> o = value.getObservable().toIterable();

		value.set("hello");
		assertEquals(ImmutableList.of("hello"), ImmutableList.copyOf(o));
	}

	@Test
	public void testSetException() throws Exception {
		ObservableValue<String> value = ObservableValue.async();
		RuntimeException e = new RuntimeException();
		value.setException(e);

		String r = value.getObservable().onErrorReturn(new Func1<Exception, String>() {
			@Override
			public String call(final Exception e) {
				return "error";
			}
		}).single();

		assertEquals("error", r);
	}
}
