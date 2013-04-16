# encoding: utf-8
from pdef.preconditions import *


class Package(object):
    def __init__(self, name, version, dependencies=None, modules=None):
        self.name = name
        self.version = version
        self.dependencies = tuple(dependencies) if dependencies else ()
        self.modules = tuple(modules) if modules else ()


class Module(object):
    def __init__(self, name, imports=None, definitions=None):
        self.name = name
        self.imports = tuple(imports) if imports else ()
        self.definitions = tuple(definitions) if definitions else ()


class Ref(object):
    def __init__(self, name, variables=None):
        self.name = check_not_none(name)
        self.variables = tuple(variables) if variables else ()

    def __repr__(self):
        s = self.name
        if self.variables:
            s += '<%s>' % ', '.join(str(v) for v in self.variables)
        return s


class ImportRef(object):
    def __init__(self, name, alias=None):
        self.name = name
        self.alias = alias if alias else name


class Type(object):
    def __init__(self, name):
        self.name = name


class Native(Type):
    def __init__(self, name, variables=None, options=None):
        super(Native, self).__init__(name)
        self.options = dict(options) if options else {}
        self.variables = tuple(variables) if variables else ()


class Message(Type):
    def __init__(self, name, base=None, subtype=None, type_field=None, type=None,
                 declared_fields=None, options=None):
        super(Message, self).__init__(name)

        self.base = base
        self.subtype = subtype

        self.type = type
        self.type_field = type_field

        self.declared_fields = tuple(declared_fields) if declared_fields else ()
        self.options = tuple(options) if options else ()


class Field(object):
    def __init__(self, name, type):
        self.name = name
        self.type = type


class Enum(Type):
    def __init__(self, name, values=None):
        super(Enum, self).__init__(name)
        self.values = tuple(values) if values else ()


class Interface(Type):
    def __init__(self, name, bases=None, methods=None):
        super(Interface, self).__init__(name)

        self.bases = tuple(bases) if bases else ()
        self.declared_methods = tuple(methods) if methods else ()


class Method(object):
    def __init__(self, name, args=None, result=None):
        self.name = name
        self.args = tuple(args) if args else ()
        self.result = result


class MethodArg(object):
    def __init__(self, name, type):
        self.name = name
        self.type = type
