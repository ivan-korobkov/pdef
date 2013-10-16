package io.pdef.rest;

import io.pdef.AbstractMessage;
import io.pdef.Message;
import io.pdef.Provider;
import io.pdef.descriptors.DataDescriptor;
import io.pdef.descriptors.Descriptors;
import io.pdef.descriptors.FieldDescriptor;
import io.pdef.descriptors.MessageDescriptor;

import java.util.concurrent.atomic.AtomicReference;

public class RestResult<T, E> extends AbstractMessage {
	private final MessageDescriptor<RestResult<T, E>> descriptor;
	private boolean success;
	private T data;
	private E exc;

	private RestResult(final MessageDescriptor<RestResult<T, E>> descriptor) {
		this.descriptor = descriptor;
	}

	public boolean isSuccess() {
		return success;
	}

	public RestResult<T, E> setSuccess(final boolean success) {
		this.success = success;
		return this;
	}

	public T getData() {
		return data;
	}

	public RestResult<T, E> setData(final T data) {
		this.data = data;
		return this;
	}

	public E getExc() {
		return exc;
	}

	public RestResult<T, E> setExc(final E exc) {
		this.exc = exc;
		return this;
	}

	@Override
	public MessageDescriptor<? extends Message> descriptor() {
		return descriptor;
	}

	public static <T, E> MessageDescriptor<RestResult<T, E>> runtimeDescriptor(
			final DataDescriptor<T> datad, final DataDescriptor<E> excd) {
		if (datad == null) throw new NullPointerException("dataDescriptor");

		@SuppressWarnings("unchecked")
		Class<RestResult<T, E>> cls = (Class<RestResult<T, E>>) (Class<?>) RestResult.class;

		final AtomicReference<MessageDescriptor<RestResult<T, E>>> ref =
				new AtomicReference<MessageDescriptor<RestResult<T, E>>>();

		MessageDescriptor.Builder<RestResult<T, E>> builder = MessageDescriptor
				.<RestResult<T, E>>builder()
				.setJavaClass(cls)
				.setProvider(new Provider<RestResult<T, E>>() {
					@Override
					public RestResult<T, E> get() {
						return new RestResult<T, E>(ref.get());
					}
				})
				.addField(FieldDescriptor.<RestResult<T, E>, Boolean>builder()
						.setName("success")
						.setType(Descriptors.bool)
						.setReflexAccessor(cls)
						.build())
				.addField(FieldDescriptor.<RestResult<T, E>, T>builder()
						.setName("data")
						.setType(datad)
						.setReflexAccessor(cls)
						.build());

		if (excd != null) {
			builder.addField(FieldDescriptor.<RestResult<T, E>, E>builder()
					.setName("exc")
					.setType(excd)
					.setReflexAccessor(cls)
					.build());
		}

		MessageDescriptor<RestResult<T, E>> descriptor = builder.build();
		ref.set(descriptor);
		return descriptor;
	}
}
