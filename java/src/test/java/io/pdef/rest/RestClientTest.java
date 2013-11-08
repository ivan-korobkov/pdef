package io.pdef.rest;

import com.google.common.collect.ImmutableMap;
import io.pdef.descriptors.Descriptors;
import io.pdef.Invocation;
import io.pdef.test.interfaces.TestException;
import io.pdef.test.interfaces.TestInterface;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


public class RestClientTest {
	@Test
	public void testInvoke() throws Exception {
		RestSession session = mock(RestSession.class);
		RestClient<TestInterface> client = new RestClient<TestInterface>(TestInterface.DESCRIPTOR, session);

		Invocation invocation = getInvocation(1, 2);
		client.invoke(invocation);

		RestRequest request = new RestRequest()
				.setPath("/query")
				.setQuery(ImmutableMap.of("arg0", "1", "arg1", "2"));
		verify(session).send(request, Descriptors.int32, TestException.DESCRIPTOR);
	}

	private Invocation getInvocation(final int arg0, final int arg1) {
		return Invocation.root(TestInterface.DESCRIPTOR.getMethod("query"),
				new Object[]{arg0, arg1});
	}
}
