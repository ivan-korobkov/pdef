# encoding: utf-8
from pdef import Type


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
    '''Abstract import class.'''
    pass


class AbsoluteImport(Import):
    def __init__(self, name):
        self.name = name


class RelativeImport(Import):
    def __init__(self, prefix, *names):
        self.prefix = prefix
        self.names = names


class Definition(object):
    def __init__(self, name, type0, doc=None):
        self.name = name
        self.type = type0
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
    def __init__(self, name, type0, is_discriminator=False, is_query=False):
        self.name = name
        self.type = type0
        self.is_discriminator = is_discriminator
        self.is_query = is_query


class Enum(Definition):
    def __init__(self, name, values=None):
        super(Enum, self).__init__(name, Type.ENUM)
        self.values = tuple(values) if values else ()


class Interface(Definition):
    def __init__(self, name, base=None, exc=None, methods=None):
        super(Interface, self).__init__(name, Type.INTERFACE)

        self.base = base
        self.exc = exc
        self.methods = tuple(methods) if methods else ()


class InterfaceOptions(object):
    def __init__(self, base=None, exc=None):
        self.base = base
        self.exc = exc


class InterfaceBase(object):
    def __init__(self, type0):
        self.type = type0


class Method(object):
    def __init__(self, name, args=None, result=None, doc=None,
                 is_index=False, is_post=False):
        self.name = name
        self.args = tuple(args) if args else ()
        self.result = result
        self.doc = doc

        self.is_index = is_index
        self.is_post = is_post


class TypeRef(object):
    def __init__(self, type0):
        self.type = type0

    def __repr__(self):
        return self.type

    @property
    def is_primitive(self):
        return self.type in Type.PRIMITIVES

    @property
    def is_datatype(self):
        return self.type in Type.DATA_TYPES


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
        self.enum = enum
        self.value = value

    def __repr__(self):
        return '%s.%s' % (self.enum, self.value)


class DefRef(TypeRef):
    def __init__(self, name):
        super(DefRef, self).__init__(Type.DEFINITION)
        self.name = name

    def __repr__(self):
        return self.name
