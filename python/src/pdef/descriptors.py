# encoding: utf-8
from pdef import Type


class Descriptor(object):
    '''Base type descriptor.'''
    def __init__(self, type0, pyclass):
        self.type = type0
        self._pyclass_supplier = _supplier(type0)
        self._pyclass = None

        self.is_primitive = self.type in Type.PRIMITIVES
        self.is_data_type = self.type in Type.DATA_TYPES
        self.is_message = self.type == Type.MESSAGE or self.type == Type.EXCEPTION

    def __str__(self):
        return str(self.pyclass)

    @property
    def pyclass(self):
        if self._pyclass is None:
            self._pyclass = self._pyclass_supplier()
        return self._pyclass


class DataDescriptor(Descriptor):
    def copy(self, x):
        raise NotImplementedError


class MessageDescriptor(DataDescriptor):
    '''Message descriptor.'''
    def __init__(self, pyclass, base=None, discriminator_value=None, fields=None, subtypes=None,
                 is_form=False):
        super(MessageDescriptor, self).__init__(Type.MESSAGE, pyclass)
        self.base = base
        self.discriminator_value = discriminator_value
        self.subtypes = dict(subtypes) if subtypes else {}
        self.is_form = is_form

        self.declared_fields = tuple(fields) if fields else ()
        self.inherited_fields = base.fields if base else ()
        self.fields = self.inherited_fields + self.declared_fields
        self.discriminator = self._find_discriminator(self.fields)

    @classmethod
    def _find_discriminator(cls, fields):
        for field in fields:
            if field.is_discriminator:
                return field

    def find_field(self, name):
        '''Return a field by its name or None.'''
        for field in self.fields:
            if field.name == name:
                return field

    def find_subtype(self, type0):
        '''Get a subtype descriptor by a type enum value or self if not found.'''
        subtype = self.subtypes.get(type0)
        if subtype is None:
            return self
        return subtype() if callable(subtype) else subtype


class FieldDescriptor(object):
    '''Message field descriptor.'''
    def __init__(self, name, type0, is_discriminator=False):
        self.name = name
        self._type_supplier = _supplier(type0)
        self._type = None
        self.is_discriminator = is_discriminator

    def __str__(self):
        return self.name + ' ' + self.type

    @property
    def type(self):
        '''Return field type descriptor.'''
        if self._type is None:
            self._type = self._type_supplier()
        return self._type

    def get(self, message):
        '''Get this field value in a message, check the type of the value.'''
        type0 = self.type
        return getattr(message, self.name)

    def set(self, message, value):
        '''Set this field in a message to a value, check the type of the value.'''
        type0 = self.type
        setattr(message, self.name, value)


class InterfaceDescriptor(Descriptor):
    '''Interface descriptor.'''
    def __init__(self, pyclass, exc=None, methods=None):
        super(InterfaceDescriptor, self).__init__(Type.INTERFACE, pyclass)
        self._exc_supplier = _supplier(exc)
        self._exc = None

        self.methods = self.methods
        self.index_method = self._find_index_method(self.methods)

    @classmethod
    def _find_index_method(cls, methods):
        for method in methods:
            if method.is_index:
                return method

    @property
    def exc(self):
        if self._exc is None:
            self._exc = self._exc_supplier() if self._exc_supplier else None
        return self._exc

    def find_method(self, name):
        '''Return a method by its name or None.'''
        for method in self.methods:
            if method.name == name:
                return method


class MethodDescriptor(object):
    '''Interface method descriptor.'''
    def __init__(self, name, result, exc=None, args=None, is_index=False, is_post=False):
        self.name = name
        self._result_supplier = _supplier(result)
        self._result = None

        self._exc_supplier = _supplier(exc)
        self._exc = None

        self.is_index = is_index
        self.is_post = is_post
        self.interface = None  # Set in the declaring interface.

        self.args = tuple(args) if args else ()

    @property
    def result(self):
        '''Return a result descriptor.'''
        if self._result is None:
            self._result = self._result_supplier()
        return self._result

    @property
    def exc(self):
        '''Return a expected interface exception.'''
        if self._exc is None:
            self._exc = self._exc_supplier if self._exc_supplier else None
        return self._exc

    @property
    def is_remote(self):
        '''Method is remote when its result is not an interface.'''
        return not self.result.is_interface

    def invoke(self, obj, *args, **kwargs):
        '''Invoke this method on an object with a given arguments, return the result'''
        return getattr(obj, self.name)(*args, **kwargs)

    def __str__(self):
        '''Return a method signature as a string.'''
        s = [self.name, '(']

        next_separator = ''
        for arg in self.args:
            s.append(next_separator)
            s.append(arg.name)
            s.append(' ')
            s.append(str(arg.type))

        s.append(')=')
        s.append(str(self.result))

        return ''.join(s)


class ArgDescriptor(object):
    '''Method argument descriptor.'''
    def __init__(self, name, type0):
        self.name = name
        self._type_supplier = _supplier(type0)
        self._type = None

    @property
    def type(self):
        '''Return argument type descriptor.'''
        if self._type is None:
            self._type = self._type_supplier()
        return self._type


class EnumDescriptor(DataDescriptor):
    '''Enum descriptor.'''
    def __init__(self, pyclass, values):
        super(EnumDescriptor, self).__init__(Type.ENUM, pyclass)
        self.values = tuple(values)

    def find_value(self, name):
        if name is None:
            return None
        name = name.upper()

        if name not in self.values:
            return None
        return name


class ListDescriptor(DataDescriptor):
    '''Internal list descriptor.'''
    def __init__(self, element):
        super(ListDescriptor, self).__init__(Type.LIST, list)
        self.element = element

    def __str__(self):
        return 'list<%s>' % self.element


class SetDescriptor(DataDescriptor):
    '''Internal set descriptor.'''
    def __init__(self, element):
        super(SetDescriptor, self).__init__(Type.SET, set)
        self.element = element

    def __str__(self):
        return 'set<%s>' % self.element


class MapDescriptor(DataDescriptor):
    '''Internal map/dict descriptor.'''
    def __init__(self, key, value):
        super(MapDescriptor, self).__init__(Type.MAP, dict)
        self.key = key
        self.value = value

    def __str__(self):
        return 'map<%s, %s>' % (self.key, self.value)


def list0(element):
    '''Create a list descriptor with an element descriptor.'''
    return ListDescriptor(element)


def set0(element):
    '''Create a set descriptor with an element descriptor.'''
    return SetDescriptor(element)


def map0(key, value):
    '''Create a map (dict) descriptor with key/value descriptors.'''
    return MapDescriptor(key, value)


def message(pyclass, base=None, discriminator_value=None, fields=None, subtypes=None,
            is_form=False):
    '''Create a message descriptor.'''
    return MessageDescriptor(pyclass, base=base, discriminator_value=discriminator_value,
                             fields=fields, subtypes=subtypes, is_form=is_form)


def field(name, type0, is_discriminator=False):
    '''Create a field descriptor.'''
    return FieldDescriptor(name, type0, is_discriminator=is_discriminator)


def enum(pyclass, values):
    '''Create an enum descriptor.'''
    return EnumDescriptor(pyclass, values)


def interface(pyclass, exc=None, methods=None):
    '''Create an interface descriptor.'''
    return InterfaceDescriptor(pyclass, exc=exc, methods=method)


def method(name, result, args=None, is_index=False, is_post=False, *methods, **kwargs):
    '''Create an interface method descriptor.'''
    return MethodDescriptor(name, result=result, args=None, is_index=is_index, is_post=is_post)


def arg(name, descriptor_supplier):
    '''Create a method argument descriptor.'''
    return ArgDescriptor(name, descriptor_supplier)


def _supplier(type_or_lambda):
    if type_or_lambda is None:
        return None

    lambda0 = lambda: None
    lambda_type = type(lambda0)

    if isinstance(type_or_lambda, lambda_type) and type_or_lambda.__name__ == lambda0.__name__:
        # It is already a supplier.
        return type_or_lambda

    return lambda: type_or_lambda


bool0 = DataDescriptor(Type.BOOL, bool)
int16 = DataDescriptor(Type.INT16, int)
int32 = DataDescriptor(Type.INT32, int)
int64 = DataDescriptor(Type.INT64, int)
float0 = DataDescriptor(Type.FLOAT, float)
double0 = DataDescriptor(Type.DOUBLE, float)
string0 = DataDescriptor(Type.STRING, unicode)
void = DataDescriptor(Type.VOID, type(None))
