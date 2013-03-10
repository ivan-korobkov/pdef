# encoding: utf-8
from pdef.preconditions import *


class Package(object):
    def __init__(self, name, version, modules=None):
        self.name = name
        self.version = version
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


class ImportRef(object):
    def __init__(self, import_name, alias=None):
        self.import_name = import_name
        self.alias = alias if alias else import_name


class Type(object):
    def __init__(self, name, variables=None):
        self.name = name
        self.variables = tuple(variables) if variables else ()


class Native(Type):
    def __init__(self, name, variables=None, options=None):
        super(Native, self).__init__(name, variables)
        self.options = dict(options) if options else {}


class Message(Type):
    def __init__(self, name, variables=None, base=None, base_tree_type=None,
                 tree_field=None, tree_type=None, declared_fields=None):
        super(Message, self).__init__(name, variables)

        self.base = base
        self.base_tree_type = base_tree_type

        self.tree_field = tree_field
        self.tree_type = tree_type

        self.declared_fields = tuple(declared_fields) if declared_fields else ()


class Field(object):
    def __init__(self, name, type):
        self.name = name
        self.type = type


class Enum(Type):
    def __init__(self, name, values=None):
        super(Enum, self).__init__(name)
        self.values = tuple(values) if values else ()
