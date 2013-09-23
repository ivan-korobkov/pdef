# encoding: utf-8
from pdef import Type, _json as json


class Descriptor(object):
    '''Base type descriptor.'''
    def __init__(self, type0, pyclass_supplier):
        self.type = type0
        self.pyclass_supplier = pyclass_supplier

        self.is_primitive = self.type in Type.PRIMITIVES
        self.is_datatype = self.type in Type.DATA_TYPES
        self.is_string = self.type == Type.STRING

        self.is_enum = self.type == Type.ENUM
        self.is_message = self.type == Type.MESSAGE
        self.is_void = self.type == Type.VOID

        self.is_interface = self.type == Type.INTERFACE

        self.is_list = self.type == Type.LIST
        self.is_set = self.type == Type.SET
        self.is_map = self.type == Type.MAP

    @property
    def pyclass(self):
        return self.pyclass_supplier()

    def check_type(self, obj):
        '''Check the object type or raise a TypeError.'''
        if self.is_valid_type(obj):
            return
        raise TypeError('Wrong type, expected=%r, actual=%r' % (self.pyclass, obj))

    def is_valid_type(self, obj):
        '''Return whether the object is of a valid type.'''
        if obj is None:
            return True

        return isinstance(obj, self.pyclass)


class DataTypeDescriptor(Descriptor):
    def parse_object(self, obj):
        '''Parse an object and return a value.'''
        raise NotImplementedError

    def parse_json(self, s):
        '''Parse an object from a json string.'''
        if s is None:
            return None

        value = json.loads(s)
        return self.parse_object(value)

    def to_object(self, obj):
        '''Serialize an object into a primitive or a collection.'''
        raise NotImplementedError

    def to_json(self, obj, indent=None):
        '''Serialize an object into a json string.'''
        value = self.to_object(obj)
        return json.dumps(value, indent=indent)


class MessageDescriptor(DataTypeDescriptor):
    '''Message descriptor.'''
    def __init__(self, pyclass_supplier,
                 base=None,
                 discriminator_value=None,
                 subtypes=None,
                 declared_fields=None,
                 is_form=False):
        super(MessageDescriptor, self).__init__(Type.MESSAGE, pyclass_supplier)
        self.base = base
        self.discriminator_value = discriminator_value
        self.subtypes = dict(subtypes) if subtypes else {}
        self.is_form = is_form

        self.declared_fields = tuple(declared_fields) if declared_fields else ()
        self.inherited_fields = base.fields if base else ()
        self.fields = self.inherited_fields + self.declared_fields

        self.discriminator = None
        for field in self.fields:
            if field.is_discriminator:
                self.discriminator = field
                break

        for field in self.declared_fields:
            field.message = self

    def __str__(self):
        return self.pyclass.__class__.__name__

    def find_field(self, name):
        '''Return a field by its name or None.'''
        for field in self.fields:
            if field.name == name:
                return field

    def subtype(self, type0):
        '''Returns a subtype descriptor by a type enum value.'''
        subtype = self.subtypes.get(type0)
        if subtype is None:
            return self.pyclass
        return subtype() if callable(subtype) else subtype

    def parse_object(self, d):
        '''Parse a message from a dictionary.'''
        if d is None:
            return None

        discriminator = self.discriminator
        if discriminator:
            type0 = discriminator.type.parse_object(d.get(discriminator.name))
            subtype_supplier = self.subtypes.get(type0)
            if subtype_supplier:
                return subtype_supplier().parse_dict(d)

        message = self.pyclass()
        for field in self.fields:
            if field.name not in d:
                continue

            data = d[field.name]
            value = field.type.parse_object(data)
            field.set(message, value)
        return message

    def to_object(self, message):
        '''Serialize a message into a dict.'''
        self.check_type(message)

        if message is None:
            return None

        d = {}
        for field in self.fields:
            value = field.get(message)
            data = field.type.to_object(value)
            if data is None:
                continue

            d[field.name] = data
        return d


class FieldDescriptor(object):
    '''Message field descriptor.'''
    def __init__(self, name, type_supplier, is_discriminator=False):
        self.name = name
        self.type_supplier = type_supplier
        self.is_discriminator = is_discriminator
        self.message = None  # Set in the declaring message.

    def __str__(self):
        return self.name + ' ' + self.type

    @property
    def type(self):
        '''Return field type descriptor.'''
        return self.type_supplier()

    def set(self, message, value):
        '''Set this field in a message to a value, check the type of the value.'''
        type0 = self.type
        if not type0.is_valid_type(value):
            raise TypeError('Wrong field type, field=%s, expected=%r, actual=%r'
                            % (self.name, type0.pyclass, value))

        setattr(message, self.name, value)

    def get(self, message):
        '''Get this field value in a message, check the type of the value.'''
        type0 = self.type
        value = getattr(message, self.name)

        if not type0.is_valid_type(value):
            raise TypeError('Wrong field type, field=%s, expected=%r, actual=%r'
                            % (self.name, type0.pyclass, value))

        return value


class InterfaceDescriptor(Descriptor):
    '''Interface descriptor.'''
    def __init__(self, pyclass_supplier,
                 base=None,
                 exc_supplier=None,
                 declared_methods=None):
        super(InterfaceDescriptor, self).__init__(Type.INTERFACE, pyclass_supplier)
        self.base = base
        self.exc_supplier = exc_supplier

        self.declared_methods = tuple(declared_methods) if declared_methods else ()
        self.inherited_methods = base.methods if base else ()
        self.methods = self.inherited_methods + self.declared_methods

        for method in self.declared_methods:
            method.interface = self

        self.index_method = None
        for method in self.methods:
            if method.is_index:
                self.index_method = method

    def __str__(self):
        return self.pyclass.__class__.__name__

    @property
    def exc(self):
        return self.exc_supplier() if self.exc_supplier else None

    def find_method(self, name):
        '''Find a method by its name.'''
        for method in self.methods:
            if method.name == name:
                return method


class MethodDescriptor(object):
    '''Interface method descriptor.'''
    def __init__(self, name, result_supplier, is_index=False, is_post=False):
        self.name = name
        self.result_supplier = result_supplier
        self.is_index = is_index
        self.is_post = is_post
        self.interface = None  # Set in the declaring interface.

        self.args = []

    @property
    def result(self):
        '''Return a result descriptor.'''
        return self.result_supplier()

    @property
    def exc(self):
        '''Return a expected interface exception.'''
        return self.interface.exc if self.interface else None

    @property
    def is_remote(self):
        '''Method is remote when its result is not an interface.'''
        return not self.result.is_interface

    def add_arg(self, name, type_supplier, is_query=False):
        '''Create and add an argument to this method, return the method.'''
        arg = ArgDescriptor(name, type_supplier, is_query=is_query)
        self.args.append(arg)
        return self

    def invoke(self, obj, *args, **kwargs):
        '''Invoke this method on an object with a given arguments, return the result'''
        return getattr(obj, self.name)(*args, **kwargs)

    def __str__(self):
        '''Return a method signature as a string.'''
        s = [self.name, '(']

        aa = []
        for arg in self.args:
            if aa:
                aa.append(', ')
            aa.append(arg.name)
            aa.append(' ')
            aa.append(str(arg.type))

        s += aa
        s.append(')=')
        s.append(str(self.result))

        return ''.join(s)


class ArgDescriptor(object):
    '''Method argument descriptor.'''
    def __init__(self, name, type_supplier, is_query=False, is_post=False):
        self.name = name
        self.type_supplier = type_supplier
        self.is_query = is_query
        self.is_post = is_post
        self.is_expand = False

    @property
    def type(self):
        '''Return argument type descriptor.'''
        return self.type_supplier()


class PrimitiveDescriptor(DataTypeDescriptor):
    '''Primitive descriptors must support serializing to/parsing from strings.'''
    def __init__(self, type0, pyclass):
        super(PrimitiveDescriptor, self).__init__(type0, lambda: pyclass)
        self._native = pyclass  # Just an optimization to get rid of the lambda in self.pyclass.

    def __str__(self):
        return self.type

    def parse_object(self, obj):
        if isinstance(obj, basestring):
            return self.parse_string(obj)

        return None if obj is None else self._native(obj)

    def parse_string(self, s):
        '''Parse a primitive from a string.'''
        return None if s is None else self._native(s)

    def to_object(self, obj):
        self.check_type(obj)

        return None if obj is None else self._native(obj)

    def to_string(self, obj):
        '''Serialize a primitive to a string, or return None when the primitive is None.'''
        return None if obj is None else unicode(self.to_object(obj))


class BoolDescriptor(PrimitiveDescriptor):
    def __init__(self):
        super(BoolDescriptor, self).__init__(Type.BOOL, bool)

    def parse_string(self, s):
        '''Parse a primitive from a string.'''
        if s is None:
            return None

        s = s.lower()
        if s == 'true':
            return True
        elif s == 'false':
            return False

        raise ValueError('Failed to parse a bool, %s' % s)

    def to_string(self, obj):
        '''Serialize a primitive to a string, or return None when the primitive is None.'''
        return None if obj is None else unicode(self.to_object(obj)).lower()


class StringDescriptor(PrimitiveDescriptor):
    def __init__(self):
        super(StringDescriptor, self).__init__(Type.STRING, unicode)

    def is_valid_type(self, obj):
        if obj is None:
            return True

        return isinstance(obj, basestring)


class EnumDescriptor(PrimitiveDescriptor):
    '''Enum descriptor.'''
    def __init__(self, pyclass_supplier, values):
        super(EnumDescriptor, self).__init__(Type.ENUM, pyclass_supplier)
        self.values = tuple(values)

    def __str__(self):
        return self.pyclass.__class__.__name__

    def is_valid_type(self, obj):
        if obj is None:
            return True

        return isinstance(obj, basestring)

    def parse_object(self, obj):
        if obj is None:
            return None
        s = unicode(obj).upper()
        return s if s in self.values else None

    def parse_string(self, s):
        return self.parse_object(s)

    def to_object(self, obj):
        self.check_type(obj)

        if obj is None:
            return None
        return unicode(obj).lower()

    def to_string(self, obj):
        return self.to_object(obj)


class ListDescriptor(DataTypeDescriptor):
    '''Internal list descriptor.'''
    def __init__(self, element):
        super(ListDescriptor, self).__init__(Type.LIST, lambda: list)
        self.element = element

    def __str__(self):
        return 'list<%s>' % (self.element)

    def parse_object(self, obj):
        if obj is None:
            return obj
        return [self.element.parse_object(e) for e in obj]

    def to_object(self, obj):
        self.check_type(obj)

        if obj is None:
            return obj
        return [self.element.to_object(e) for e in obj]


class SetDescriptor(DataTypeDescriptor):
    '''Internal set descriptor.'''
    def __init__(self, element):
        super(SetDescriptor, self).__init__(Type.SET, lambda: set)
        self.element = element

    def __str__(self):
        return 'set<%s>' % (self.element)

    def parse_object(self, obj):
        if obj is None:
            return None
        return set(self.element.parse_object(e) for e in obj)

    def to_object(self, obj):
        self.check_type(obj)

        if obj is None:
            return None
        return set(self.element.to_object(e) for e in obj)


class MapDescriptor(DataTypeDescriptor):
    '''Internal map/dict descriptor.'''
    def __init__(self, key, value):
        super(MapDescriptor, self).__init__(Type.MAP, lambda: dict)
        self.key = key
        self.value = value

    def __str__(self):
        return 'map<%s, %s>' % (self.key, self.value)

    def parse_object(self, obj):
        if obj is None:
            return None
        return {self.key.parse_object(k): self.value.parse_object(v) for k, v in obj.items()}

    def to_object(self, obj):
        self.check_type(obj)

        if obj is None:
            return None
        return {self.key.to_object(k): self.value.to_object(v) for k, v in obj.items()}


class ObjectDescriptor(DataTypeDescriptor):
    def __init__(self):
        super(ObjectDescriptor, self).__init__(Type.OBJECT, lambda: object)

    def __str__(self):
        return 'object'

    def parse_object(self, obj):
        return obj

    def to_object(self, obj):
        return obj


class VoidDescriptor(DataTypeDescriptor):
    def __init__(self):
        super(VoidDescriptor, self).__init__(Type.VOID, lambda: object)

    def __str__(self):
        return 'void'

    def parse_object(self, obj):
        return None

    def to_object(self, obj):
        return None


def list0(element):
    '''Create a list descriptor with an element descriptor.'''
    return ListDescriptor(element)


def set0(element):
    '''Create a set descriptor with an element descriptor.'''
    return SetDescriptor(element)


def map0(key, value):
    '''Create a map (dict) descriptor with key/value descriptors.'''
    return MapDescriptor(key, value)


def message(pyclass_supplier,
            base=None,
            discriminator_value=None,
            subtypes=None,
            declared_fields=None,
            is_form=False):
    '''Create a message descriptor.'''
    return MessageDescriptor(pyclass_supplier,
                             base=base,
                             discriminator_value=discriminator_value,
                             subtypes=subtypes,
                             declared_fields=declared_fields,
                             is_form=is_form)


def field(name, descriptor_supplier, is_discriminator=False):
    '''Create a field descriptor.'''
    return FieldDescriptor(name, descriptor_supplier, is_discriminator=is_discriminator)


def enum(pyclass, values):
    '''Create an enum descriptor.'''
    return EnumDescriptor(pyclass, values)


def interface(pyclass_supplier, base=None, exc_supplier=None, declared_methods=None):
    '''Create an interface descriptor.'''
    return InterfaceDescriptor(pyclass_supplier,
                               base=base,
                               exc_supplier=exc_supplier,
                               declared_methods=declared_methods)


def method(name, result_supplier, is_index=False, is_post=False):
    '''Create an interface method descriptor.'''
    return MethodDescriptor(name, result_supplier=result_supplier, is_index=is_index,
                            is_post=is_post)


def arg(name, descriptor_supplier, is_query=False):
    '''Create a method argument descriptor.'''
    return ArgDescriptor(name, descriptor_supplier, is_query=is_query)


bool0 = BoolDescriptor()
int16 = PrimitiveDescriptor(Type.INT16, int)
int32 = PrimitiveDescriptor(Type.INT32, int)
int64 = PrimitiveDescriptor(Type.INT64, int)
float0 = PrimitiveDescriptor(Type.FLOAT, float)
double0 = PrimitiveDescriptor(Type.DOUBLE, float)
string = StringDescriptor()
object0 = ObjectDescriptor()
void = VoidDescriptor()
