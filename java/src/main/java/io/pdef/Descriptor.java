package io.pdef;

public interface Descriptor<T> extends Reader<T>, Writer<T> {
	T getDefault();
}
