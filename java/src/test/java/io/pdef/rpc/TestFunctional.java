package io.pdef.rpc;

import static org.junit.Assert.assertEquals;

public class TestFunctional {

	@org.junit.Test
	public void test() throws Exception {
		Func<String, String> service = PipelineBuilder
				.start(new Square())
				.add(new StringFilter())
				.add(new MetricFilter<String, String>())
				.build();

		String out = service.handle("12");
		assertEquals("144", out);
	}

	public static class MetricFilter<I, O> implements Observer<I, O> {

		@Override
		public O handle(final I in, final Func<I, O> next) {
			return next.handle(in);
		}
	}

	public static class Square implements Func<Integer, Integer> {
		@Override
		public Integer handle(final Integer in) {
			return in * in;
		}
	}

	public static class StringFilter implements Filter<String, String, Integer, Integer> {

		public String handle(final String in, Func<Integer, Integer> next) {
			Integer i = Integer.valueOf(in);
			Integer o = next.handle(i);
			return Integer.toString(o);
		}
	}
}
