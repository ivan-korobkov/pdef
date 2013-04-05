package pdef.generated;

import com.google.common.annotations.VisibleForTesting;
import static com.google.common.base.Preconditions.*;
import com.google.common.collect.Queues;
import pdef.InterfaceDescriptor;
import pdef.MessageDescriptor;
import pdef.TypeDescriptor;

import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

public class GeneratedTypeInitializer {
	private static final GeneratedTypeInitializer instance = new GeneratedTypeInitializer();
	private final Queue<GeneratedTypeDescriptor> queue;
	private final ReentrantLock lock;

	public static void initialize(final GeneratedTypeDescriptor descriptor) {
		instance.init(descriptor);
	}

	@VisibleForTesting
	GeneratedTypeInitializer() {
		queue = Queues.newLinkedBlockingQueue();
		lock = new ReentrantLock();
	}

	public void init(final GeneratedTypeDescriptor descriptor) {
		checkNotNull(descriptor);

		queue.add(descriptor);
		processQueue();
	}

	private void processQueue() {
		if (lock.isHeldByCurrentThread()) {
			// Prevent recursive calls.
			return;
		}

		lock.lock();
		try {
			for (GeneratedTypeDescriptor d = queue.poll(); d != null; d = queue.poll()) {
				processInit(d);
			}
		} finally {
			lock.unlock();
		}
	}

	private void processInit(final TypeDescriptor descriptor) {
		if (!(descriptor instanceof GeneratedTypeDescriptor)) return;
		GeneratedTypeDescriptor d = (GeneratedTypeDescriptor) descriptor;

		if (d.getState() != GeneratedTypeDescriptor.State.NEW) return;
		if (d.getState() == GeneratedTypeDescriptor.State.NEW) d.executeLink();

		// Process the rawtype of this parameterized type.
		if (d instanceof ParameterizedTypeDescriptor<?>) {
			ParameterizedTypeDescriptor<?> message =
					(ParameterizedTypeDescriptor<?>) d;
			TypeDescriptor rawtype = message.getRaw();
			if (rawtype instanceof GeneratedTypeDescriptor) {
				processInit(rawtype);
			}
		}

		// Process the base of this message.
		if (d instanceof MessageDescriptor) {
			MessageDescriptor message = (MessageDescriptor) d;
			MessageDescriptor base = message.getBase();
			if (base != null && base instanceof GeneratedTypeDescriptor) {
				processInit(base);
			}
		}

		// Process the bases of this interface.
		if (d instanceof InterfaceDescriptor) {
			InterfaceDescriptor iface = (InterfaceDescriptor) d;
			for (InterfaceDescriptor base : iface.getBases()) {
				processInit(base);
			}
		}

		d.executeInit();
	}
}
