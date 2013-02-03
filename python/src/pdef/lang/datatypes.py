# encoding: utf-8
from pdef.lang.core import *


class Native(Definition):
    def __init__(self, name, variables, options):
        super(Native, self).__init__(name, variables)

        self.java_type = options.java_type

    def build_special(self):
        rawtype = self.rawtype
        self.java_type = rawtype.java_type


class Enum(Definition):
    def __init__(self, name, values):
        super(Enum, self).__init__(name)

        self.values = set(values) if values else set()


class EnumValue(Node):
    def __init__(self, type, value):
        super(EnumValue, self).__init__(value)
        self.type = type
        self.value = value

    def link(self):
        self.type = self.type.link()
        if not isinstance(self.type, Enum):
            self.error('%s: wrong type %s, must be an enum',self, self.type)
            return

        if self.value not in self.type.value_set:
            self.error('%s: enum value %s is not found', self, self.value)
            return

        return self


class Message(Definition):
    def __init__(self, name, variables=None, base=None, declared_fields=None):
        super(Message, self).__init__(name, variables)

        self.base = base
        self.declared_fields = ListMap(declared_fields) if declared_fields else ListMap()
        self.fields = ListMap()

        self.type_field = None
        self.is_type_base = None
        self.subtypes = []

    def symbol(self, name):
        if name in self.variables.map:
            return self.variables.map[name]

        return super(Message, self).symbol(name)

    def link(self):
        if self.base:
            self.base = self.base.link()

        for field in self.declared_fields:
            field.link()

        return self

    def build_special(self):
        if not self.arguments:
            raise ValueError("Not a special %s" % self)

        rawtype = self.rawtype
        arg_map = dict((var, arg) for (var, arg) in zip(rawtype.variables, self.arguments))

        base = rawtype.base
        if base:
            sbase = base.special(arg_map, self.pool)
            self.base = sbase

        declared_fields = []
        for field in rawtype.declared_fields:
            type = field.type
            stype = type.special(arg_map, self.pool)

            sfield = Field(field.name, self)
            sfield.type = stype
            sfield.options = field.options

            declared_fields.append(sfield)
        self.declared_fields = declared_fields


class Field(Node):
    def __init__(self, name, type):
        super(Field, self).__init__(name)

        self.type = type
        self.read_only = False
        self.is_type_field = False
        self.is_type_base_field = False

    def link(self):
        self.type = self.type.link()
        return self
