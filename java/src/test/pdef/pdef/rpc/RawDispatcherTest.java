package pdef.rpc;

import com.google.common.collect.ImmutableMap;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import pdef.fixtures.interfaces.App;
import pdef.fixtures.interfaces.Calc;
import pdef.fixtures.interfaces.Users;
import pdef.formats.Parser;
import pdef.formats.RawParser;

import java.util.Map;

public class RawDispatcherTest {
	private Dispatcher dispatcher;

	@Before
	public void setUp() throws Exception {
		Parser parser = new RawParser();
		dispatcher = new RawDispatcher(parser);
	}

	@Test
	public void testDispatch() throws Exception {
		App app = new TestApp();
		Map<String, Object> args = ImmutableMap.<String, Object>of(
				"calc", ImmutableMap.of(),
				"sum", ImmutableMap.of("i0", 10, "i1", 38));

		Object result = dispatcher.dispatch(App.Descriptor.getInstance(), app, args);
		assertEquals(48, (int) (Integer) result);
	}

	private static class TestApp implements App {
		@Override
		public Users users() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Calc calc() {
			return new TestCalc();
		}
	}

	private static class TestCalc implements Calc {
		@Override
		public int sum(final int i0, final int i1) {
			return i0 + i1;
		}
	}
}
