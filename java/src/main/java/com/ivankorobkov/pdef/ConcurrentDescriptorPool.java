package com.ivankorobkov.pdef;

import static com.google.common.base.Preconditions.*;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.common.reflect.Reflection;
import com.google.common.reflect.TypeToken;
import com.ivankorobkov.pdef.data.DataTypeDescriptor;
import com.ivankorobkov.pdef.data.TypeVariableDescriptor;
import static java.lang.System.identityHashCode;

import javax.annotation.concurrent.GuardedBy;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ConcurrentDescriptorPool implements DescriptorPool {

	private final Queue<PackageDescriptor> queue;
	private final ReentrantReadWriteLock.ReadLock readLock;
	private final ReentrantReadWriteLock.WriteLock writeLock;

	private final ConcurrentMap<String, PackageDescriptor> packages;
	private final ConcurrentMap<Class<?>, PackageDescriptor> packagesByClass;
	private final ConcurrentMap<TypeToken<?>, DataTypeDescriptor> definitions;

	public ConcurrentDescriptorPool() {
		queue = Queues.newLinkedBlockingQueue();

		ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
		readLock = lock.readLock();
		writeLock = lock.writeLock();

		packages = Maps.newConcurrentMap();
		packagesByClass = Maps.newConcurrentMap();
		definitions = Maps.newConcurrentMap();
	}

	@Override
	public void add(final PackageDescriptor pkg) {
		checkNotNull(pkg);
		// TODO: Replace prints with logs.
		//System.out.println("Enqueued " + pkg);

		queue.add(pkg);
		processQueue();
	}

	@Override
	public <T extends PackageDescriptor> T getPackage(final Class<T> cls) {
		readLock.lock();
		try {
			PackageDescriptor pkg = packagesByClass.get(cls);
			if (pkg != null) {
				@SuppressWarnings("unchecked")
				T cast = (T) pkg;
				return cast;
			}

			throw new IllegalArgumentException("No package " + cls);
		} finally {
			readLock.unlock();
		}
	}

	private void processQueue() {
		if (writeLock.isHeldByCurrentThread()) {
			// Prevent recursive calls, because they break "add-initialize" passes.
			//System.out.println("Write locked already");
			return;
		}

		if (!writeLock.tryLock()) {
			// Another thread is already processing the queue.
			// Just return.
			return;
		}

		//System.out.println("Write lock");
		try {
			List<PackageDescriptor> addedPackages = Lists.newArrayList();
			Set<Class<? extends PackageDescriptor>> dependencies = Sets.newHashSet();

			for (PackageDescriptor pkg = queue.poll(); pkg != null; pkg = queue.poll()) {
				if (addPackageIfNotPresent(pkg)) {
					addedPackages.add(pkg);
				}

				Set<Class<? extends PackageDescriptor>> deps = pkg.getDependencies();
				dependencies.addAll(deps);
				Reflection.initialize(deps.toArray(new Class[deps.size()]));
			}
			for (Class<?> dependency : dependencies) {
				checkState(packagesByClass.containsKey(dependency), "No package instance %s",
						dependency);
			}

			for (PackageDescriptor newPackage : addedPackages) {
				addDefinitions(newPackage);
			}

			for (PackageDescriptor newPackage : addedPackages) {
				linkDefinitions(newPackage);
			}
		} finally {
			writeLock.unlock();
			//System.out.println("Write unlock");
		}
	}

	@GuardedBy("writeLock")
	private boolean addPackageIfNotPresent(final PackageDescriptor pkg) {
		checkState(writeLock.isHeldByCurrentThread());

		String name = pkg.getName();
		if (packages.containsKey(name)) {
			PackageDescriptor present = packages.get(name);
			checkArgument(present == pkg,
					"Duplicate package named \"%s\", %s hash(%s), %s hash(%s)", name, present,
					identityHashCode(present), pkg, identityHashCode(pkg));
			return false;
		}

		packages.put(name, pkg);
		packagesByClass.put(pkg.getClass(), pkg);

		return true;
	}

	@GuardedBy("writeLock")
	private void addDefinitions(final PackageDescriptor newPackage) {
		checkState(writeLock.isHeldByCurrentThread());

		for (Map.Entry<Class<?>, DataTypeDescriptor> e : newPackage.getDefinitions().entrySet()) {
			Class<?> cls = e.getKey();
			DataTypeDescriptor descriptor = e.getValue();
			addDefinition(cls, descriptor);
		}
	}

	@GuardedBy("writeLock")
	private void addDefinition(final Class<?> cls, final DataTypeDescriptor descriptor) {
		checkState(writeLock.isHeldByCurrentThread());

		TypeToken<?> token = TypeToken.of(cls);
		checkState(!definitions.containsKey(token), "Duplicate type descriptor %s", descriptor);
		definitions.put(token, descriptor);
	}

	@GuardedBy("writeLock")
	private void linkDefinitions(final PackageDescriptor newPackage) {
		checkState(writeLock.isHeldByCurrentThread());

		for (DataTypeDescriptor descriptor : newPackage.getDefinitions().values()) {
			descriptor.link(this);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> DataTypeDescriptor get(final TypeToken<T> token) {
		DataTypeDescriptor descriptor = findRawType(token);
		return (DataTypeDescriptor) descriptor;
	}

	@SuppressWarnings("unchecked")
	private <T> DataTypeDescriptor findRawType(final TypeToken<T> token) {
		checkNotNull(token, "token == null");

		readLock.lock();
		try {
			DataTypeDescriptor descriptor = definitions.get(token);
			if (descriptor != null) {
				return descriptor;
			}
		} finally {
			readLock.unlock();
		}

		writeLock.lock();
		try {
			DataTypeDescriptor descriptor = definitions.get(token);
			if (descriptor != null) {
				return descriptor;
			}

			Type type = token.getType();
			if (type instanceof TypeVariable) {
				descriptor = new TypeVariableDescriptor(token);
				definitions.put(token, descriptor);
				return descriptor;

			} else if (type instanceof ParameterizedType) {
				// Get a raw descriptor and parameterize it
				// with the actual arguments.

				ParameterizedType ptype = (ParameterizedType) type;
				TypeToken rawToken = TypeToken.of(ptype.getRawType());
				DataTypeDescriptor rawDescriptor = definitions.get(rawToken);
				if (rawDescriptor == null) {
					throw new IllegalArgumentException("Type descriptor is not found, " + token);
				}
				descriptor = rawDescriptor.parameterize(token);
				definitions.put(token, descriptor);
				descriptor.link(this);

				return descriptor;
			} else {
				throw new IllegalArgumentException("Type descriptor is not found, " + token);
			}
		} finally {
			writeLock.unlock();
		}
	}
}
