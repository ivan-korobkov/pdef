package pdef.generated;

import com.google.common.annotations.VisibleForTesting;
import static com.google.common.base.Preconditions.*;
import com.google.common.collect.Queues;
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

	private void processInit(final GeneratedTypeDescriptor descriptor) {
		if (descriptor.getState() != GeneratedTypeDescriptor.State.NEW) return;
		if (descriptor.getState() == GeneratedTypeDescriptor.State.NEW) descriptor.executeLink();

		// Process the rawtype of this parameterized type.
		if (descriptor instanceof ParameterizedTypeDescriptor<?>) {
			ParameterizedTypeDescriptor<?> message =
					(ParameterizedTypeDescriptor<?>) descriptor;
			TypeDescriptor rawtype = message.getRaw();
			if (rawtype instanceof GeneratedTypeDescriptor) {
				processInit((GeneratedTypeDescriptor) rawtype);
			}
		}

		// Process the base of this message.
		if (descriptor instanceof MessageDescriptor) {
			MessageDescriptor message = (MessageDescriptor) descriptor;
			MessageDescriptor base = message.getBase();
			if (base != null && base instanceof GeneratedTypeDescriptor) {
				processInit((GeneratedTypeDescriptor) base);
			}
		}

		descriptor.executeInit();
	}
}
