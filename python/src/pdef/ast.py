# encoding: utf-8
from pdef.consts import Type
from pdef.preconditions import *


class File(object):
    def __init__(self, name, definitions=None):
        self.name = name
        self.definitions = tuple(definitions) if definitions else ()


class Ref(object):
    def __init__(self, type):
        self.type = type

    def __repr__(self):
        return self.type


class ListRef(Ref):
    def __init__(self, element):
        Ref.__init__(self, Type.LIST)
        self.element = element

    def __repr__(self):
        return 'list<%s>' % self.element


class SetRef(Ref):
    def __init__(self, element):
        Ref.__init__(self, Type.SET)
        self.element = element

    def __repr__(self):
        return 'set<%s>' % self.element


class MapRef(Ref):
    def __init__(self, key, value):
        Ref.__init__(self, Type.MAP)
        self.key = key
        self.value = value

    def __repr__(self):
        return 'map<%s, %s>' % (self.key, self.value)


class EnumValueRef(Ref):
    def __init__(self, enum, value):
        super(EnumValueRef, self).__init__(Type.ENUM_VALUE)
        check_isinstance(enum, DefinitionRef)
        self.enum = enum
        self.value = value

    def __repr__(self):
        return '%s.%s' % (self.enum, self.value)


class DefinitionRef(Ref):
    def __init__(self, name):
        Ref.__init__(self, Type.DEFINITION)
        self.name = name

    def __repr__(self):
        return self.name


class Definition(object):
    def __init__(self, name):
        self.name = name


class Message(Definition):
    def __init__(self, name, base=None, base_type=None, fields=None, is_exception=False):
        super(Message, self).__init__(name)

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
        super(Enum, self).__init__(name)
        self.values = tuple(values) if values else ()


class Interface(Definition):
    def __init__(self, name, bases=None, methods=None):
        super(Interface, self).__init__(name)

        self.bases = tuple(bases) if bases else ()
        self.methods = tuple(methods) if methods else ()


class Method(object):
    def __init__(self, name, args=None, result=None):
        self.name = name
        self.args = tuple(args) if args else ()
        self.result = result


class MethodArg(object):
    def __init__(self, name, type):
        self.name = name
        self.type = type
