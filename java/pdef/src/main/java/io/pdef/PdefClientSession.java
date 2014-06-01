package io.pdef;

import java.io.IOException;
import java.net.HttpURLConnection;

public interface PdefClientSession {
	/** Invoked when a new connection is opened, the method can modify the connection. */
	void connectionOpened(HttpURLConnection connection) throws IOException;
	
	/** Invoked when a response is received. */
	void responseReceived(HttpURLConnection connection) throws IOException;

	/** Handles an application specific error and throws an application exception. */
	void handleError(HttpURLConnection connection) throws IOException;
}
