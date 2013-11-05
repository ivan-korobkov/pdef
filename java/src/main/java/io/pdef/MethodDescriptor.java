package io.pdef;

import java.util.List;

public interface MethodDescriptor<T, R> extends MethodInvoker<T, R> {
	/**
	 * Returns a pdef method name.
	 */
	String getName();

	/**
	 * Returns this method result descriptor.
	 *
	 * It can be a {@link DataDescriptor} if the method is terminal or {@link
	 * InterfaceDescriptor} otherwise.
	 */
	Descriptor<R> getResult();

	/**
	 * Returns a list of argument descriptors or an empty list.
	 */
	List<ArgumentDescriptor<?>> getArgs();

	/**
	 * Returns a method exception descriptor or {@literal null}.
	 *
	 * In default pdef implementation all interface methods share the same exception.
	 */
	MessageDescriptor<?> getExc();

	/**
	 * Returns whether this method is an index method in an interface.
	 */
	boolean isIndex();

	/**
	 * Returns whether this method is a post method (annotated with @post annotation).
	 */
	boolean isPost();

	/**
	 * Returns whether this method returns a data type or void (not an interface).
	 */
	boolean isRemote();
}
