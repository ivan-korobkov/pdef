package io.pdef.rpc;

import com.google.common.collect.ImmutableMap;
import io.pdef.descriptors.Descriptors;
import static io.pdef.rpc.HttpUrlConnectionRpcSession.*;
import io.pdef.test.interfaces.TestException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

public class HttpUrlConnectionRpcSessionTest {
	HttpUrlConnectionRpcSession session = new HttpUrlConnectionRpcSession("http://localhost");

	@Test
	public void testBuildUrl() throws Exception {
		RpcRequest request = new RpcRequest()
				.setPath("/method/arg")
				.setQuery(ImmutableMap.of("key", "value", "привет", "мир"));
		URL result = session.buildUrl("http://localhost", request);
		assertEquals(
				"http://localhost/method/arg?key=value"
						+ "&%D0%BF%D1%80%D0%B8%D0%B2%D0%B5%D1%82=%D0%BC%D0%B8%D1%80",
				result.toString());
	}

	@Test
	public void testBuildParamsQuery() throws Exception {
		String result = session.buildParamsQuery(ImmutableMap.of("key", "value", "привет", "мир"));
		assertEquals("key=value&%D0%BF%D1%80%D0%B8%D0%B2%D0%B5%D1%82=%D0%BC%D0%B8%D1%80", result);
	}

	@Test
	public void testSendPostData() throws Exception {
		OutputStream out = mock(OutputStream.class);
		HttpURLConnection connection = mock(HttpURLConnection.class);
		when(connection.getOutputStream()).thenReturn(out);

		RpcRequest request = new RpcRequest()
				.setPost(ImmutableMap.of("key", "value", "привет", "мир"));
		session.sendPostData(connection, request);

		byte[] data = "key=value&%D0%BF%D1%80%D0%B8%D0%B2%D0%B5%D1%82=%D0%BC%D0%B8%D1%80".getBytes();
		verify(out).write((byte[]) any(), eq(0), eq(data.length));
	}

	@Test
	public void testHandleResponse_readResult() throws Exception {
		InputStream input = new ByteArrayInputStream("\"Привет\"".getBytes(UTF8));

		HttpURLConnection connection = mock(HttpURLConnection.class);
		when(connection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
		when(connection.getInputStream()).thenReturn(input);

		String result = session.handleResponse(connection, Descriptors.string, null);
		assertEquals("Привет", result);
	}

	@Test
	public void testHandleResponse_readApplicationException() throws Exception {
		TestException e = new TestException().setText("Привет");
		InputStream input = new ByteArrayInputStream(e.toJson().getBytes(UTF8));

		HttpURLConnection connection = mock(HttpURLConnection.class);
		when(connection.getResponseCode()).thenReturn(APPLICATION_EXC_STATUS);
		when(connection.getErrorStream()).thenReturn(input);

		try {
			session.handleResponse(connection, Descriptors.void0, TestException.DESCRIPTOR);
			fail();
		} catch (TestException e1) {
			assertEquals(e, e1);
		}
	}

	@Test(expected = RpcException.class)
	public void testReadError() throws Exception {
		HttpURLConnection connection = mock(HttpURLConnection.class);
		when(connection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_BAD_REQUEST);

		session.handleResponse(connection, Descriptors.void0, null);
	}

	@Test
	public void testReadString() throws Exception {
		String s = "Привет, как дела?";
		Charset charset = Charset.forName("windows-1251");
		InputStream input = new ByteArrayInputStream(s.getBytes(charset));

		HttpURLConnection connection = mock(HttpURLConnection.class);
		when(connection.getHeaderField(CONTENT_TYPE_HEADER))
				.thenReturn("text/plain; charset=windows-1251");

		String result = session.readString(connection, input);
		assertEquals(s, result);
	}

	@Test
	public void testGuessContentTypeCharset() throws Exception {
		HttpURLConnection connection = mock(HttpURLConnection.class);
		when(connection.getHeaderField(CONTENT_TYPE_HEADER))
				.thenReturn("text/plain; charset=us-ascii");

		Charset charset = session.guessContentTypeCharset(connection);
		assertEquals(Charset.forName("US-ASCII"), charset);
	}
}
