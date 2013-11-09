package io.pdef.rpc;

import com.google.common.collect.ImmutableMap;
import io.pdef.descriptors.ValueDescriptor;
import io.pdef.descriptors.Descriptors;
import io.pdef.test.interfaces.TestException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;

public class DeafultClientSessionTest {
	DefaultRpcClientSession session = new DefaultRpcClientSession("http://localhost:8080");

	@Test
	public void testBuildUri() throws Exception {
		RpcRequest request = new RpcRequest()
				.setPath("/hello/world")
				.setQuery(ImmutableMap.of("a", "1"));

		URI uri = session.buildUri(request);
		assertEquals("http://localhost:8080/hello/world?a=1", uri.toASCIIString());
	}

	@Test
	public void testBuildRequest_get() throws Exception {
		RpcRequest request = new RpcRequest()
				.setPath("/hello/world")
				.setQuery(ImmutableMap.of("a", "1"));

		org.apache.http.HttpRequest req = httpRequest(session.buildRequest(request));
		assertEquals(RpcRequest.GET, req.getRequestLine().getMethod());
		assertEquals("http://localhost:8080/hello/world?a=1", req.getRequestLine().getUri());
	}

	@Test
	public void testBuildRequest_post() throws Exception {
		RpcRequest request = new RpcRequest()
				.setMethod(RpcRequest.POST)
				.setPath("/hello/world")
				.setPost(ImmutableMap.of("a", "1", "text", "привет"));

		HttpPost req = (HttpPost) httpRequest(session.buildRequest(request));

		byte[] content = entityToBytes(req.getEntity());
		assertEquals(RpcRequest.POST, req.getRequestLine().getMethod());
		assertEquals("http://localhost:8080/hello/world", req.getRequestLine().getUri());
		assertArrayEquals("a=1&text=%D0%BF%D1%80%D0%B8%D0%B2%D0%B5%D1%82".getBytes(), content);
	}

	@Test
	public void testHandle_result() throws Exception {
		HttpResponse resp = new BasicHttpResponse(HttpVersion.HTTP_1_0, 200, "OK");
		resp.setEntity(new StringEntity("\"Привет\"", ContentType.APPLICATION_JSON));

		ValueDescriptor<String> resultDescriptor = Descriptors.string;
		String result = session.handle(resp, resultDescriptor, null);

		assertEquals("Привет", result);
	}

	@Test(expected = TestException.class)
	public void testHandle_applicationException() throws Exception {
		TestException e = new TestException().setText("привет");
		HttpResponse resp = new BasicHttpResponse(HttpVersion.HTTP_1_0,
				DefaultRpcClientSession.APPLICATION_EXC_STATUS, "OK");
		resp.setEntity(new StringEntity(e.toJson(), ContentType.APPLICATION_JSON));

		session.handle(resp, null, TestException.DESCRIPTOR);
	}

	@Test(expected = RpcException.class)
	public void testHandle_rpcException() throws Exception {
		HttpResponse resp = new BasicHttpResponse(HttpVersion.HTTP_1_0, 404, "OK");
		session.handle(resp, null, null);
	}

	private org.apache.http.HttpRequest httpRequest(final Request request) {
		// A bit of reflection to simplify the tests.
		// I think, it is bad to use reflection to access private fields,
		// but the fluent HttpClient is not easy to use in tests.
		try {
			Field field = Request.class.getDeclaredField("request");
			field.setAccessible(true);
			return (org.apache.http.HttpRequest) field.get(request);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		}
	}

	private byte[] entityToBytes(final HttpEntity entity) throws IOException {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(bout);
		entity.writeTo(out);
		return bout.toByteArray();
	}
}
