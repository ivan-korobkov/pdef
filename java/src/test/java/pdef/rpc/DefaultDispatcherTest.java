package pdef.rpc;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import pdef.MethodDescriptor;
import pdef.fixtures.interfaces.App;
import pdef.fixtures.interfaces.Calc;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class DefaultDispatcherTest {
	private DefaultRpcDispatcher dispatcher;

	@Before
	public void setUp() throws Exception {
		dispatcher = new DefaultRpcDispatcher();
	}

	@Test
	public void testDispatch() throws Exception {
		App.Descriptor app = App.Descriptor.getInstance();
		Calc.Descriptor calc = Calc.Descriptor.getInstance();
		MethodDescriptor calcMethod = app.getMethods().map().get("calc");
		MethodDescriptor sumMethod = calc.getMethods().map().get("sum");

		List<Call> calls = ImmutableList.of(
				new Call(calcMethod, ImmutableMap.of()),
				new Call(sumMethod, ImmutableMap.of("i0", 1, "i1", 2)));

		App sevice = new TestApp();
		Object result = dispatcher.dispatch(calls, sevice);
		assertEquals(3, (int) (Integer) result);
	}

}
