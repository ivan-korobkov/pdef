package io.pdef.client;

import com.google.common.base.Function;
import io.pdef.Pdef;
import io.pdef.io.ObjectInput;
import io.pdef.io.Reader;
import io.pdef.rpc.Response;
import io.pdef.rpc.ResponseStatus;

import java.lang.reflect.Type;

import static com.google.common.base.Preconditions.checkNotNull;

public class ResponseToResult implements Function<Response, Object> {
	private final Type resultType;
	private final Type excType;
	private final Pdef pdef;

	public ResponseToResult(final Type resultType, final Type excType, final Pdef pdef) {
		this.pdef = pdef;
		this.resultType = checkNotNull(resultType);
		this.excType = excType;
	}

	@Override
	public Object apply(final Response response) {
		ResponseStatus status = response.getStatus();

		Reader<?> reader;
		if (status == ResponseStatus.OK) reader = pdef.getReader(resultType);
		else if (status == ResponseStatus.ERROR) reader = pdef.getReader(io.pdef.rpc.Error.class);
		else if (status == ResponseStatus.EXCEPTION) reader = pdef.getReader(excType);
		else throw new IllegalArgumentException("No status in response: " + response);

		ObjectInput input = new ObjectInput(response.getResult());
		Object result = reader.get(input);

		if (status == ResponseStatus.OK) return reader;
		throw (RuntimeException) result;
	}
}
