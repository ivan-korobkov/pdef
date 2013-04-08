package pdef.rpc;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import pdef.MethodDescriptor;
import pdef.fixtures.interfaces.App;
import pdef.fixtures.interfaces.Calc;
import pdef.formats.Parser;
import pdef.formats.RawParser;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class RawRpcParserTest {
	private RpcParser rpcParser;

	@Before
	public void setUp() throws Exception {
		Parser parser = new RawParser();
		rpcParser = new RawRpcParser(parser);
	}

	@Test
	public void testDispatch() throws Exception {
		Map<String, Object> args = ImmutableMap.<String, Object>of(
				"calc", ImmutableMap.of(),
				"sum", ImmutableMap.of("i0", 10, "i1", 38));

		App.Descriptor app = App.Descriptor.getInstance();
		Calc.Descriptor calc = Calc.Descriptor.getInstance();
		MethodDescriptor calcMethod = app.getMethods().map().get("calc");
		MethodDescriptor sumMethod = calc.getMethods().map().get("sum");

		List<Call> calls = rpcParser.parse(app, args);
		Call call0 = calls.get(0);
		Call call1 = calls.get(1);

		assertEquals(2, calls.size());
		assertEquals(calcMethod, call0.getMethod());
		assertEquals(sumMethod, call1.getMethod());
	}
}
