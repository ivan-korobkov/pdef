# encoding: utf-8
from pdef.common import Type
from pdef.preconditions import *


class Location(object):
    def __init__(self, path, line=0):
        self.path = path
        self.line = line

    def __str__(self):
        s = self.path if self.path else 'nofile'
        return '%s, line %s' % (s, self.line) if self.line else s


class File(object):
    def __init__(self, name, imports=None, definitions=None):
        self.name = name
        self.imports = tuple(imports) if imports else ()
        self.definitions = tuple(definitions) if definitions else ()
        self.location = None


class Import(object):
    def __init__(self, module_name, *names):
        self.module_name = module_name
        self.names = names


class Definition(object):
    def __init__(self, name, type, doc=None):
        self.name = name
        self.type = type
        self.doc = doc
        self.location = None


class Message(Definition):
    def __init__(self, name, base=None, base_type=None, fields=None, is_exception=False):
        super(Message, self).__init__(name, Type.MESSAGE)

        self.base = base
        self.base_type = base_type

        self.fields = tuple(fields) if fields else ()
        self.is_exception = is_exception


class Field(object):
    def __init__(self, name, type, is_discriminator=False):
        self.name = name
        self.type = type
        self.is_discriminator = is_discriminator


class Enum(Definition):
    def __init__(self, name, values=None):
        super(Enum, self).__init__(name, Type.ENUM)
        self.values = tuple(values) if values else ()


class Interface(Definition):
    def __init__(self, name, base=None, methods=None):
        super(Interface, self).__init__(name, Type.INTERFACE)

        self.base = base
        self.methods = tuple(methods) if methods else ()


class InterfaceOptions(object):
    def __init__(self, base=None, exc=None):
        self.base = base
        self.exc = exc


class InterfaceBase(object):
    def __init__(self, type):
        self.type = type


class Method(object):
    def __init__(self, name, args=None, result=None, doc=None):
        self.name = name
        self.args = tuple(args) if args else ()
        self.result = result
        self.doc = doc


class TypeRef(object):
    def __init__(self, type):
        self.type = type

    def __repr__(self):
        return self.type

    @property
    def is_primitive(self):
        return self.type in Type.PRIMITIVES

    @property
    def is_datatype(self):
        return self.type in Type.DATATYPES


class ListRef(TypeRef):
    def __init__(self, element):
        super(ListRef, self).__init__(Type.LIST)
        self.element = element

    def __repr__(self):
        return 'list<%s>' % self.element


class SetRef(TypeRef):
    def __init__(self, element):
        super(SetRef, self).__init__(Type.SET)
        self.element = element

    def __repr__(self):
        return 'set<%s>' % self.element


class MapRef(TypeRef):
    def __init__(self, key, value):
        super(MapRef, self).__init__(Type.MAP)
        self.key = key
        self.value = value

    def __repr__(self):
        return 'map<%s, %s>' % (self.key, self.value)


class EnumValueRef(TypeRef):
    def __init__(self, enum, value):
        super(EnumValueRef, self).__init__(Type.ENUM_VALUE)
        check_isinstance(enum, DefType)
        self.enum = enum
        self.value = value

    def __repr__(self):
        return '%s.%s' % (self.enum, self.value)


class DefType(TypeRef):
    def __init__(self, name):
        super(DefType, self).__init__(Type.DEFINITION)
        self.name = name

    def __repr__(self):
        return self.name
