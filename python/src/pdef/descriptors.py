# encoding: utf-8
from pdef.types import Type


class Descriptor(object):
    '''Base data type descriptor.'''
    def __init__(self, type0):
        self.type = type

    def default(self):
        '''Return the default value.'''
        raise NotImplementedError

    def parse(self, obj):
        '''Parse an object and return a value.'''
        raise NotImplementedError

    def serialize(self, obj):
        '''Serialize an object into a primitive or a collection.'''
        raise NotImplementedError


class PrimitiveDescriptor(Descriptor):
    '''Primitive descriptors must support serializing to/parsing from strings.'''
    def parse_string(self, s):
        '''Parse a primitive from a string.'''
        raise NotImplementedError

    def serialize_to_string(self, obj):
        '''Serialize a primitive to a string, or return None when the primitive is None.'''
        raise NotImplementedError


class EnumDescriptor(PrimitiveDescriptor):
    '''Enum descriptor.'''
    def __init__(self, pyclass, *values):
        super(EnumDescriptor, self).__init__(Type.ENUM)
        self.pyclass = pyclass
        self.values = tuple(values)

    def default(self):
        return self.values[0] if self.values else None

    def parse(self, obj):
        s = str(obj).lower()
        return s if s in self.values else None

    def parse_string(self, s):
        return self.parse(s)

    def serialize(self, obj):
        return str(obj)

    def serialize_to_string(self, obj):
        return self.serialize(obj)


class MessageDescriptor(Descriptor):
    '''Message descriptor.'''
    def __init__(self, pyclass, type0, discriminator=None, subtypes=None, fields=None):
        super(MessageDescriptor, self).__init__(Type.MESSAGE)
        self.pyclass = pyclass
        self.subtypes = dict(subtypes) if subtypes else {}
        self.fields = tuple(fields) if fields else ()

    def default(self):
        return self.instance()

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

        instance = self.pyclass()
        instance.merge_dict(d)
        return instance


    def serialize(self, obj):
        '''Serialize a message to a dictionary.'''
        if obj is None:
            return None

        return obj.to_dict()


class FieldDescriptor(object):
    '''Message field descriptor.'''
    def __init__(self, name, type0):
        self.name = name
        self._type = type0

    @property
    def type(self):
        '''Return field type descriptor.'''
        t = self._type
        return t() if callable(t) else t

    def is_set(self, obj):
        return getattr(obj, self.name) is not None

    def set(self, obj, value):
        setattr(obj, self.name, value)

    def get(self, obj):
        return getattr(obj, self.name)

    def clear(self, obj):
        return setattr(obj, self.name, None)


class _SimplePrimitiveDescriptor(PrimitiveDescriptor):
    '''Internal primitive descriptor.'''
    def __init__(self, type0, pyclass, default):
        super(_SimplePrimitiveDescriptor, self).__init__(type0)
        self.pyclass = pyclass
        self._default = default

    def default(self):
        return self._default

    def parse(self, obj):
        return self.default() if obj is None else self.pyclass(obj)

    def serialize_to_string(self, obj):
        return None if obj is None else str(obj)


class _ListDescriptor(Descriptor):
    '''Internal list descriptor.'''
    def __init__(self, element):
        super(_ListDescriptor, self).__init__(Type.LIST)
        self.element = element

    def default(self):
        return []

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
        super(_SetDescriptor, self).__init__(Type.SET)
        self.element = element

    def default(self):
        return set()

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
        super(_MapDescriptor, self).__init__(Type.MAP)
        self.key = key
        self.value = value

    def default(self):
        return {}

    def parse(self, obj):
        if obj is None:
            return None
        return {self.key.parse(k): self.value.parse(v) for k, v in obj.values()}

    def serialize(self, obj):
        if obj is None:
            return None
        return {self.key.serialize(k): self.value.serialize(v) for k, v in obj.values()}


class _ObjectDescriptor(Descriptor):
    def default(self):
        return None

    def parse(self, obj):
        return obj

    def serialize(self, obj):
        return obj


bool0 = _SimplePrimitiveDescriptor(Type.BOOL, bool, False)
int16 = _SimplePrimitiveDescriptor(Type.INT16, int, 0)
int32 = _SimplePrimitiveDescriptor(Type.INT32, int, 0)
int64 = _SimplePrimitiveDescriptor(Type.INT64, int, 0)
float0 = _SimplePrimitiveDescriptor(Type.FLOAT, float, 0.0)
double0 = _SimplePrimitiveDescriptor(Type.DOUBLE, float, 0.0)
string = _SimplePrimitiveDescriptor(Type.STRING, unicode, '')
object0 = _ObjectDescriptor(Type.OBJECT)
void = _ObjectDescriptor(Type.VOID)


def list0(element):
    '''Create a list descriptor with an element descriptor.'''
    return _ListDescriptor(element)


def set0(element):
    '''Create a set descriptor with an element descriptor.'''
    return _SetDescriptor(element)


def map0(key, value):
    '''Create a map (dict) descriptor with key/value descriptors.'''
    return _MapDescriptor(key, value)


def message(pyclass, subtypes=None, fields=None):
    '''Create a message descriptor.'''
    return MessageDescriptor(pyclass, subtypes=subtypes, fields=fields)


def field(name, type0):
    '''Create a field descriptor.'''
    return FieldDescriptor(name, type0)


def enum(pyclass, *values):
    '''Create an enum descriptor.'''
    return EnumDescriptor(pyclass, *values)
