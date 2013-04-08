package pdef.rpc;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import pdef.MethodDescriptor;
import pdef.fixtures.interfaces.App;
import pdef.fixtures.interfaces.Calc;
import pdef.formats.RawSerializer;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class RawRpcSerializerTest {
	private RawRpcSerializer rpcSerializer;

	@Before
	public void setUp() throws Exception {
		rpcSerializer = new RawRpcSerializer(new RawSerializer());
	}

	@Test
	public void testSerialize() throws Exception {
		App.Descriptor app = App.Descriptor.getInstance();
		Calc.Descriptor calc = Calc.Descriptor.getInstance();
		MethodDescriptor calcMethod = app.getMethods().map().get("calc");
		MethodDescriptor sumMethod = calc.getMethods().map().get("sum");

		List<Call> calls = ImmutableList.of(
				new Call(calcMethod, ImmutableMap.of()),
				new Call(sumMethod, ImmutableMap.of("i0", 10, "i1", 11)));
		Map<String, Object> result = rpcSerializer.serialize(calls);

		assertEquals(ImmutableMap.<String, Object>of(
				"calc", ImmutableMap.of(),
				"sum", ImmutableMap.of("i0", 10, "i1", 11)), result);
	}
}
