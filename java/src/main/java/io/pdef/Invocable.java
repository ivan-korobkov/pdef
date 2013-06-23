package io.pdef;

interface Invocable {
	Object invoke(Object delegate, Object... args);
}
