# encoding: utf-8
from pdef import ast
from pdef.lang import Type, SymbolTable
from pdef.preconditions import *


class Enum(Type):
    @classmethod
    def from_node(cls, node):
        check_isinstance(node, ast.Enum)
        enum = Enum(node.name)
        for name in node.values:
            EnumValue(name, enum)

        return enum

    def __init__(self, name, module=None, values=None):
        super(Enum, self).__init__(name, module=module)
        self.values = SymbolTable(self)
        if values:
            self.add_values(*values)

    def add_value(self, value):
        self.values.add(value)
        self.symbols.add(value)

    def add_values(self, *values):
        map(self.add_value, values)

    def __contains__(self, enum_value):
        return enum_value in self.values.as_map().values()


class EnumValue(Type):
    def __init__(self, name, enum):
        super(EnumValue, self).__init__(name, module=enum.module)
        self.enum = enum
        enum.add_value(self)

    @property
    def parent(self):
        return self.enum
