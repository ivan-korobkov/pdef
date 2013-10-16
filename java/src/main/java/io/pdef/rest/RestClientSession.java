package io.pdef.rest;

import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;

public interface RestClientSession {
	Response send(Request request) throws Exception;
}
