# encoding: utf-8


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
    def __init__(self, name, doc=None):
        self.name = name
        self.doc = doc
        self.location = None


class Message(Definition):
    def __init__(self, name, base=None, discriminator_value=None, fields=None, is_exception=False,
                 is_form=False):
        super(Message, self).__init__(name)

        self.base = base
        self.discriminator_value = discriminator_value

        self.fields = tuple(fields) if fields else ()
        self.is_exception = is_exception
        self.is_form = is_form


class Field(object):
    def __init__(self, name, type0, is_discriminator=False):
        self.name = name
        self.type = type0
        self.is_discriminator = is_discriminator
        self.is_query = False


class Enum(Definition):
    def __init__(self, name, values=None):
        super(Enum, self).__init__(name)
        self.values = tuple(values) if values else ()


class Interface(Definition):
    def __init__(self, name, base=None, exc=None, methods=None):
        super(Interface, self).__init__(name)

        self.base = base
        self.exc = exc
        self.methods = tuple(methods) if methods else ()


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


class MethodArg(object):
    def __init__(self, name, type0):
        self.name = name
        self.type = type0


class TypeRef(object):
    location = None


class ListRef(TypeRef):
    def __init__(self, element):
        self.element = element

    def __repr__(self):
        return 'list<%s>' % self.element


class SetRef(TypeRef):
    def __init__(self, element):
        self.element = element

    def __repr__(self):
        return 'set<%s>' % self.element


class MapRef(TypeRef):
    def __init__(self, key, value):
        self.key = key
        self.value = value

    def __repr__(self):
        return 'map<%s, %s>' % (self.key, self.value)


class DefRef(TypeRef):
    def __init__(self, name):
        self.name = name

    def __repr__(self):
        return self.name


class ValueRef(TypeRef):
    def __init__(self, type0):
        self.type = type0
