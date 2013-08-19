package pdef.descriptors;

/** A class which allows to lazily reference descriptors. */
public interface DescriptorSupplier {

	/** Returns a descriptor instance. */
	Descriptor get();
}
