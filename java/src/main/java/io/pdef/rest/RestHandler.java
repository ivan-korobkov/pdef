package io.pdef.rest;

public interface RestHandler {
	RestResponse handle(RestRequest request) throws Exception;
}
