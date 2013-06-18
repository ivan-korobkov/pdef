package io.pdef.invocation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class AbstractInvocation implements Invocation {
	private final String method;
	private final Map<String, Object> args;
	@Nullable private final Type excType;
	@Nullable private final Invocation parent;

	public AbstractInvocation(final String method, final Map<String, Object> args,
			@Nullable final Type excType, @Nullable final Invocation parent) {
		this.method = checkNotNull(method);
		this.args = ImmutableMap.copyOf(args);
		this.excType = excType;
		this.parent = parent;
	}

	@Override
	public String getMethod() {
		return method;
	}

	@Override
	public Map<String, Object> getArgs() {
		return args;
	}

	@Nullable
	@Override
	public Type getExcType() {
		return excType;
	}

	@Nullable
	@Override
	public Invocation getParent() {
		return parent;
	}

	@Override
	public List<Invocation> toList() {
		if (parent == null) return ImmutableList.<Invocation>of(this);
		return null;
	}

}
