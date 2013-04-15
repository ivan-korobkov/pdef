package pdef.rpc;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import pdef.InterfaceDescriptor;
import pdef.MethodDescriptor;
import pdef.fixtures.interfaces.App;
import pdef.fixtures.interfaces.Calc;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class RpcInvocationTest {

	@Test
	public void testInvoke() throws Exception {
		InterfaceDescriptor descriptor = App.Descriptor.getInstance();
		TestRpcHandler handler = new TestRpcHandler();

		RpcInvocation invoker = RpcInvocation.of(descriptor, handler);
		App app = (App) invoker.toProxy();
		int result = app.calc().sum(10, 11);
		assertEquals(1, result);

		App.Descriptor appDescriptor = App.Descriptor.getInstance();
		Calc.Descriptor calcDescriptor = Calc.Descriptor.getInstance();
		MethodDescriptor calcMethod = appDescriptor.getMethods().map().get("calc");
		MethodDescriptor sumMethod = calcDescriptor.getMethods().map().get("sum");

		List<Call> calls = handler.calls;
		assertEquals(calcMethod, calls.get(0).getMethod());
		assertEquals(sumMethod, calls.get(1).getMethod());
		assertEquals(ImmutableMap.of(), calls.get(0).getArgs());
		assertEquals(ImmutableMap.of("i0", 10, "i1", 11), calls.get(1).getArgs());
	}

	private static class TestRpcHandler implements RpcHandler {
		List<Call> calls;

		@Override
		public Object handle(final List<Call> calls) {
			this.calls = calls;
			return 1;
		}
	}
}
