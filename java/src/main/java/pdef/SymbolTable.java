package pdef;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface SymbolTable<T extends Symbol> extends Iterable<T> {

	List<T> list();

	Set<T> set();

	Map<String, T> map();

	SymbolTable<T> merge(SymbolTable<T> another);

	int size();
}
