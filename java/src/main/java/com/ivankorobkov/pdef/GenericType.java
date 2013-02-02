package com.ivankorobkov.pdef;

/**
 * Inheritance
 * generic -> datatype -> variable, value, message, list, set, map, enum
 * 	       -> field
 * 	       -> interface
 * 	       -> method
 */
public interface GenericType {

	GenericDescriptor getDescriptor();
}
