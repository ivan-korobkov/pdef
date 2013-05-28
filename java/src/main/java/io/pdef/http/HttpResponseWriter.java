package io.pdef.http;

import com.google.common.net.MediaType;
import io.pdef.JsonFormat;
import io.pdef.Pdef;
import io.pdef.rpc.Response;
import io.pdef.rpc.ResponseStatus;
import io.pdef.rpc.RpcException;
import io.pdef.rpc.RpcExceptionCode;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

public class HttpResponseWriter {
	private final JsonFormat format;

	public HttpResponseWriter(final Pdef pdef) {
		format = new JsonFormat(pdef);
	}

	public HttpResponseWriter(final JsonFormat format) {
		this.format = checkNotNull(format);
	}

	public void write(final Response response, final HttpServletResponse httpResponse)
			throws IOException {
		String result = format.write(response);

		int httpStatus = HttpServletResponse.SC_OK;
		ResponseStatus status = response.getStatus();
		switch (status != null ? status : ResponseStatus.RPC_ERROR) {
			case OK:
			case ERROR:
				httpStatus = HttpServletResponse.SC_OK;
				break;
			case RPC_ERROR:
				RpcException e = response.getRpcExc();
				RpcExceptionCode code = e.getCode();
				switch (code != null ? code : RpcExceptionCode.SERVER_ERROR) {
					case SERVER_ERROR:
						httpStatus = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
						break;
					case BAD_REQUEST:
						httpStatus = HttpServletResponse.SC_BAD_REQUEST;
						break;
					case SERVICE_UNAVAILABLE:
						httpStatus = HttpServletResponse.SC_SERVICE_UNAVAILABLE;
						break;
					case TIMEOUT:
						httpStatus = HttpServletResponse.SC_REQUEST_TIMEOUT;
						break;
				}
		}

		httpResponse.setStatus(httpStatus);
		httpResponse.setContentType(MediaType.JSON_UTF_8.toString());
		httpResponse.getWriter().write(result);
	}
}
