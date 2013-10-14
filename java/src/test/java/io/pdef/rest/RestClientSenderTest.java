package io.pdef.rest;

import com.google.common.collect.ImmutableMap;
import io.pdef.descriptors.Descriptors;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Arrays;

public class RestClientSenderTest {
	RestClientSender sender;

	@Before
	public void setUp() throws Exception {
		sender = new RestClientSender("http://localhost:8080/", null);
	}

	@Test
	public void testCreateRequest_get() throws Exception {
		RestRequest request = RestRequest.get()
				.setPath("/hello/world")
				.setQuery(ImmutableMap.of("a", "1"));

		HttpRequest req = httpRequest(sender.createRequest(request));
		assert req.getRequestLine().getMethod().equals(Rest.GET);
		assert req.getRequestLine().getUri().equals("http://localhost:8080/hello/world?a=1");
	}

	@Test
	public void testCreateRequest_post() throws Exception {
		RestRequest request = RestRequest.post()
				.setPath("/hello/world")
				.setPost(ImmutableMap.of("a", "1", "text", "привет"));

		HttpPost req = (HttpPost) httpRequest(sender.createRequest(request));

		byte[] content = entityToBytes(req.getEntity());
		assert req.getRequestLine().getMethod().equals(Rest.POST);
		assert req.getRequestLine().getUri().equals("http://localhost:8080/hello/world");
		assert Arrays.equals(content, "a=1&text=%D0%BF%D1%80%D0%B8%D0%B2%D0%B5%D1%82".getBytes());
	}

	@Test
	public void testBuildUri() throws Exception {
		RestRequest request = new RestRequest()
				.setPath("/hello/world")
				.setQuery(ImmutableMap.of("a", "1", "text", "привет"));
		URI uri = sender.buildUri(request);
		assert uri.toString().equals(
				"http://localhost:8080/hello/world?a=1&text=%D0%BF%D1%80%D0%B8%D0%B2%D0%B5%D1%82");
	}

	@Test
	public void testParseHttpResponse() throws Exception {
		String content = RestFormat.resultDescriptor(Descriptors.string, null)
				.newInstance()
				.setSuccess(true)
				.setData("привет")
				.serializeToJson(true);

		HttpResponse resp = new BasicHttpResponse(new ProtocolVersion("HTTP", 1, 0), 200, "OK");
		resp.setEntity(new StringEntity(content, ContentType.APPLICATION_JSON));

		RestResponse response = sender.parseHttpResponse(resp);
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

	private byte[] entityToBytes(final HttpEntity entity) {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(bout);
		try {
			entity.writeTo(out);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return bout.toByteArray();
	}
}
