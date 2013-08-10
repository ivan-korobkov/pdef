# encoding: utf-8
from collections import OrderedDict
from pdef.types import Type


class Descriptor(object):
    '''Base data type descriptor.'''
    def __init__(self, type0, pyclass_supplier):
        self.type = type0
        self.pyclass_supplier = pyclass_supplier

        self.is_primitive = self.type in Type.PRIMITIVES
        self.is_datatype = self.type in Type.DATATYPES
        self.is_interface = self.type == Type.INTERFACE
        self.is_message = self.type == Type.MESSAGE

        self.is_enum = self.type == Type.ENUM

        self.is_list = self.type == Type.LIST
        self.is_set = self.type == Type.SET
        self.is_map = self.type == Type.MAP

    @property
    def pyclass(self):
        return self.pyclass_supplier()

    def parse(self, obj):
        '''Parse an object and return a value.'''
        raise NotImplementedError

    def serialize(self, obj):
        '''Serialize an object into a primitive or a collection.'''
        raise NotImplementedError


class MessageDescriptor(Descriptor):
    '''Message descriptor.'''
    def __init__(self, pyclass_supplier,
                 base=None,
                 base_type=None,
                 discriminator_name=None,
                 subtypes=None,
                 declared_fields=None):
        super(MessageDescriptor, self).__init__(Type.MESSAGE, pyclass_supplier)
        self.base = base
        self.base_type = base_type
        self.subtypes = dict(subtypes) if subtypes else {}

        self.declared_fields = tuple(declared_fields) if declared_fields else ()
        self.inherited_fields = base.__descriptor__.fields if base else ()
        self.fields = self.inherited_fields + self.declared_fields

        self.discriminator = None
        if discriminator_name:
            for field in self.fields:
                if field.name == discriminator_name:
                    self.discriminator = field
                    break

    def instance(self):
        '''Create a new instance.'''
        return self.pyclass()

    def subtype(self, type0):
        '''Returns a subtype descriptor by a type enum value.'''
        subtype = self.subtypes.get(type0)
        return subtype() if callable(subtype) else subtype

    def parse(self, d):
        '''Parse a message from a dictionary.'''
        if d is None:
            return None

        return self.pyclass.parse_dict(d)

    def serialize(self, obj):
        '''Serialize a message to a dictionary.'''
        if obj is None:
            return None

        return obj.to_dict()


class FieldDescriptor(object):
    '''Message field descriptor.'''
    def __init__(self, name, descriptor_supplier):
        self.name = name
        self.descriptor_supplier = descriptor_supplier

    @property
    def descriptor(self):
        '''Return field type descriptor.'''
        return self.descriptor_supplier()

    def is_set(self, obj):
        return getattr(obj, self.name) is not None

    def set(self, obj, value):
        setattr(obj, self.name, value)

    def get(self, obj):
        return getattr(obj, self.name)

    def clear(self, obj):
        return setattr(obj, self.name, None)


class InterfaceDescriptor(Descriptor):
    '''Interface descriptor.'''

    def __init__(self, pyclass_supplier, base=None, exc_supplier=None, declared_methods=None):
        super(InterfaceDescriptor, self).__init__(Type.INTERFACE, pyclass_supplier)
        self.base = base
        self.exc_supplier = exc_supplier

        self.declared_methods = tuple(declared_methods) if declared_methods else ()
        self.inherited_methods = base.__descriptor__.methods if base else ()
        self.methods = self.inherited_methods + self.declared_methods

    @property
    def exc(self):
        return self.exc_supplier() if self.exc_supplier else None


class MethodDescriptor(object):
    '''Interface method descriptor.'''
    def __init__(self, name, result_supplier, args=None):
        self.name = name
        self.result_supplier = result_supplier
        self.args = OrderedDict(args) if args else OrderedDict()

    @property
    def result(self):
        '''Return result descriptor.'''
        return self.result_supplier()

    @property
    def is_remote(self):
        '''Method is remote when its result is not an interface.'''
        return not self.result.is_interface

    def invoke(self, obj, *args, **kwargs):
        '''Invoke this method on an object with a given arguments, return the result'''
        return getattr(obj, self.name)(*args, **kwargs)


class PrimitiveDescriptor(Descriptor):
    '''Primitive descriptors must support serializing to/parsing from strings.'''
    def __init__(self, type0, pyclass):
        super(PrimitiveDescriptor, self).__init__(type0, lambda: pyclass)

    def parse(self, obj):
        return None if obj is None else self.pyclass(obj)

    def parse_string(self, s):
        '''Parse a primitive from a string.'''
        return None if s is None else self.pyclass(s)

    def serialize(self, obj):
        return None if obj is None else self.pyclass(obj)

    def serialize_to_string(self, obj):
        '''Serialize a primitive to a string, or return None when the primitive is None.'''
        return None if obj is None else str(self.serialize(obj))


class EnumDescriptor(PrimitiveDescriptor):
    '''Enum descriptor.'''
    def __init__(self, pyclass_supplier, values):
        super(EnumDescriptor, self).__init__(Type.ENUM, pyclass_supplier)
        self.values = tuple(values)

    def parse(self, obj):
        if obj is None:
            return None
        s = str(obj).upper()
        return s if s in self.values else None

    def parse_string(self, s):
        return self.parse(s)

    def serialize(self, obj):
        if obj is None:
            return None
        return str(obj).lower()

    def serialize_to_string(self, obj):
        return self.serialize(obj)


class _ListDescriptor(Descriptor):
    '''Internal list descriptor.'''
    def __init__(self, element):
        super(_ListDescriptor, self).__init__(Type.LIST, lambda: list)
        self.element = element

    def parse(self, obj):
        if obj is None:
            return obj
        return [self.element.parse(e) for e in obj]

    def serialize(self, obj):
        if obj is None:
            return obj
        return [self.element.serialize(e) for e in obj]


class _SetDescriptor(Descriptor):
    '''Internal set descriptor.'''
    def __init__(self, element):
        super(_SetDescriptor, self).__init__(Type.SET, set)
        self.element = element

    def parse(self, obj):
        if obj is None:
            return None
        return set(self.element.parse(e) for e in obj)

    def serialize(self, obj):
        if obj is None:
            return None
        return set(self.element.serialize(e) for e in obj)


class _MapDescriptor(Descriptor):
    '''Internal map/dict descriptor.'''
    def __init__(self, key, value):
        super(_MapDescriptor, self).__init__(Type.MAP, dict)
        self.key = key
        self.value = value

    def parse(self, obj):
        if obj is None:
            return None
        return {self.key.parse(k): self.value.parse(v) for k, v in obj.items()}

    def serialize(self, obj):
        if obj is None:
            return None
        return {self.key.serialize(k): self.value.serialize(v) for k, v in obj.items()}


class _ObjectDescriptor(Descriptor):
    def __init__(self):
        super(_ObjectDescriptor, self).__init__(Type.OBJECT, lambda: object)

    def parse(self, obj):
        return obj

    def serialize(self, obj):
        return obj


class _VoidDescriptor(Descriptor):
    def __init__(self):
        super(_VoidDescriptor, self).__init__(Type.VOID, lambda: object)

    def parse(self, obj):
        return None

    def serialize(self, obj):
        return None


bool0 = PrimitiveDescriptor(Type.BOOL, bool)
int16 = PrimitiveDescriptor(Type.INT16, int)
int32 = PrimitiveDescriptor(Type.INT32, int)
int64 = PrimitiveDescriptor(Type.INT64, int)
float0 = PrimitiveDescriptor(Type.FLOAT, float)
double0 = PrimitiveDescriptor(Type.DOUBLE, float)
string = PrimitiveDescriptor(Type.STRING, unicode)
object0 = _ObjectDescriptor()
void = _VoidDescriptor()


def list0(element):
    '''Create a list descriptor with an element descriptor.'''
    return _ListDescriptor(element)


def set0(element):
    '''Create a set descriptor with an element descriptor.'''
    return _SetDescriptor(element)


def map0(key, value):
    '''Create a map (dict) descriptor with key/value descriptors.'''
    return _MapDescriptor(key, value)


def message(pyclass_supplier,
            base=None,
            base_type=None,
            discriminator_name=None,
            subtypes=None,
            declared_fields=None):
    '''Create a message descriptor.'''
    return MessageDescriptor(pyclass_supplier,
                             base=base,
                             base_type=base_type,
                             discriminator_name=discriminator_name,
                             subtypes=subtypes,
                             declared_fields=declared_fields)


def field(name, type_supplier):
    '''Create a field descriptor.'''
    return FieldDescriptor(name, type_supplier)


def enum(pyclass, values):
    '''Create an enum descriptor.'''
    return EnumDescriptor(pyclass, values)


def interface(pyclass_supplier, base=None, exc_supplier=None, declared_methods=None):
    '''Create an interface descriptor.'''
    return InterfaceDescriptor(pyclass_supplier,
                               base=base,
                               exc_supplier=exc_supplier,
                               declared_methods=declared_methods)


def method(name, result_supplier, args=None):
    '''Create an interface method descriptor.'''
    return MethodDescriptor(name, result_supplier=result_supplier, args=args)
