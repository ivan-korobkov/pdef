package pdef;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import java.util.*;

public class ImmutableSymbolTable<T extends Symbol> implements SymbolTable<T> {

	private final ImmutableList<T> list;
	private final ImmutableSet<T> set;
	private final ImmutableMap<String, T> map;

	private static final ImmutableSymbolTable<?> EMPTY;

	static {
		@SuppressWarnings("unchecked")
		ImmutableSymbolTable table = new ImmutableSymbolTable(Collections.emptyList());
		EMPTY = table;
	}

	@SuppressWarnings("unchecked")
	public static <T extends Symbol> ImmutableSymbolTable<T> of() {
		return (ImmutableSymbolTable<T>) EMPTY;
	}

	public static <T extends Symbol> ImmutableSymbolTable<T> of(final T... elements) {
		return new ImmutableSymbolTable<T>(Arrays.asList(elements));
	}

	public static <T extends Symbol> ImmutableSymbolTable<T> copyOf(final Iterable<T> iterable) {
		return new ImmutableSymbolTable<T>(iterable);
	}

	private ImmutableSymbolTable(final Iterable<T> iterable) {
		list = ImmutableList.copyOf(iterable);
		set = ImmutableSet.copyOf(list);

		ImmutableMap.Builder<String, T> builder = ImmutableMap.builder();
		for (T symbol : list) {
			builder.put(symbol.getName(), symbol);
		}
		map = builder.build();
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(map)
				.toString();
	}

	@Override
	public List<T> list() {
		return list;
	}

	@Override
	public Set<T> set() {
		return set;
	}

	@Override
	public Map<String, T> map() {
		return map;
	}

	@Override
	public Iterator<T> iterator() {
		return list.iterator();
	}

	@Override
	public SymbolTable<T> merge(final SymbolTable<T> another) {
		checkNotNull(another);
		List<T> list = Lists.newArrayList(list());
		for (T element : another) {
			list.add(element);
		}

		return copyOf(list);
	}

	@Override
	public int size() {
		return list.size();
	}
}
