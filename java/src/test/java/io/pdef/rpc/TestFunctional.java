package io.pdef.rpc;

import static org.junit.Assert.assertEquals;

public class TestFunctional {

	@org.junit.Test
	public void test() throws Exception {
		Func<String, String> func = new StringFilter()
				.then(new MetricFilter<Integer, Integer>())
				.then(new Square());

		String out = func.apply("12");
		assertEquals("144", out);
	}

	public static class MetricFilter<I, O> extends Filter<I, O, I, O> {
		@Override
		public O apply(final I request, final Func<I, O> func) {
			return func.apply(request);
		}
	}

	public static class Square implements Func<Integer, Integer> {
		@Override
		public Integer apply(final Integer in) {
			return in * in;
		}
	}

	public static class StringFilter extends Filter<String, String, Integer, Integer> {

		@Override
		public String apply(final String request, final Func<Integer, Integer> func) {
			Integer i = Integer.valueOf(request);
			Integer o = func.apply(i);
			return Integer.toString(o);
		}
	}
}
