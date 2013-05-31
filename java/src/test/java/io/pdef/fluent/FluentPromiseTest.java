package io.pdef.fluent;

import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FluentPromiseTest {
	@Test
	public void testSet() throws Exception {
		FluentPromise<String> promise = FluentPromise.create();
		promise.set("hello");
		assertEquals("hello", promise.getUnchecked());
	}

	@Test
	public void testSetException() throws Exception {
		IllegalStateException exc = new IllegalStateException();
		FluentPromise<String> promise = FluentPromise.create();
		promise.setException(exc);

		try {
			promise.get();
		} catch (ExecutionException e) {
			assertTrue(e.getCause() == exc);
		}
	}
}
