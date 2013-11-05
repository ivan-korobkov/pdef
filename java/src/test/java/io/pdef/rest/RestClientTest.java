package io.pdef.rest;

import com.google.common.collect.ImmutableMap;
import io.pdef.Descriptors;
import io.pdef.invoke.InvocationProxy;
import io.pdef.invoke.Invoker;
import io.pdef.test.interfaces.TestInterface;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Arrays;


public class RestClientTest {
	RestClient client;
	RestClient.DefaultHandler handler;

	@Before
	public void setUp() throws Exception {
		client = RestClient.create("http://localhost:8080/");
		handler = new RestClient.DefaultHandler("http://localhost:8080/", null);
	}

	@Test
	public void testInvoke() throws Exception {
		RestClient client = RestClient.create(new RestHandler() {
			@Override
			public RestResponse handle(final RestRequest request) throws Exception {
				RestResult<Integer, ?> result = RestProtocol.resultDescriptor(Descriptors.int32, null)
						.newInstance()
						.setSuccess(true)
						.setData(3);

				return new RestResponse()
						.setOkStatus()
						.setJsonContentType()
						.setContent(result.toJson(true));
			}
		});

		int methodResult = proxy(client).testIndex(1, 2);
		assertEquals(3, methodResult);
	}

	@Test
	public void testParseErrorResponse() throws Exception {
		RestResponse response = new RestResponse()
				.setStatus(HttpURLConnection.HTTP_BAD_REQUEST)
				.setContent("Bad request");

		RestException exception = client.errorResponse(response);
		assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, exception.getStatus());
		assertEquals("Bad request", exception.getMessage());
	}

	// Test DefaultHandler

	@Test
	public void testCreateHttpRequest_get() throws Exception {
		RestRequest request = RestRequest.get()
				.setPath("/hello/world")
				.setQuery(ImmutableMap.of("a", "1"));

		HttpRequest req = httpRequest(handler.createHttpRequest(request));
		assert req.getRequestLine().getMethod().equals(RestRequest.GET);
		assert req.getRequestLine().getUri().equals("http://localhost:8080/hello/world?a=1");
	}

	@Test
	public void testCreateRequest_post() throws Exception {
		RestRequest request = RestRequest.post()
				.setPath("/hello/world")
				.setPost(ImmutableMap.of("a", "1", "text", "привет"));

		HttpPost req = (HttpPost) httpRequest(handler.createHttpRequest(request));

		byte[] content = entityToBytes(req.getEntity());
		assert req.getRequestLine().getMethod().equals(RestRequest.POST);
		assert req.getRequestLine().getUri().equals("http://localhost:8080/hello/world");
		assert Arrays.equals(content, "a=1&text=%D0%BF%D1%80%D0%B8%D0%B2%D0%B5%D1%82".getBytes());
	}

	@Test
	public void testBuildUri() throws Exception {
		RestRequest request = new RestRequest()
				.setPath("/hello/world")
				.setQuery(ImmutableMap.of("a", "1", "text", "привет"));
		URI uri = handler.buildUri(request);
		assert uri.toString().equals(
				"http://localhost:8080/hello/world?a=1&text=%D0%BF%D1%80%D0%B8%D0%B2%D0%B5%D1%82");
	}

	@Test
	public void testParseHttpResponse() throws Exception {
		String content = RestProtocol.resultDescriptor(Descriptors.string, null)
				.newInstance()
				.setSuccess(true)
				.setData("привет")
				.toJson(true);

		HttpResponse resp = new BasicHttpResponse(new ProtocolVersion("HTTP", 1, 0), 200, "OK");
		resp.setEntity(new StringEntity(content, ContentType.APPLICATION_JSON));

		RestResponse response = handler.parseHttpResponse(resp);
		assert response.getStatus() == HttpURLConnection.HTTP_OK;
		assert response.hasJsonContentType();
		assert response.getContent().equals(content);
	}

	private HttpRequest httpRequest(final Request request) {
		// A bit of reflection to simplify the tests.
		// I think, it is bad to use reflection to access private fields,
		// but the fluent HttpClient is not easy to use in tests.
		try {
			Field field = Request.class.getDeclaredField("request");
			field.setAccessible(true);
			return (HttpRequest) field.get(request);
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

	private TestInterface proxy(final Invoker invoker) {
		return InvocationProxy.create(TestInterface.class, invoker);
	}
}
