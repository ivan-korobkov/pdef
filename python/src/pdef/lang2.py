# encoding: utf-8
import logging
from collections import deque
from pdef import ast
from pdef.preconditions import *


class Definition(object):
    def __init__(self, name, module=None):
        self.name = name
        self.module = module
        self._inited = False

    def __repr__(self):
        return '<%s %s>' % (self.__class__.__name__, self.fullname)

    @property
    def fullname(self):
        if self.module:
            return '%s %s' % (self.module.name, self.name)
        return self.name

    def init(self):
        if self._inited:
            return

        self._inited = True
        self._init()

    def _init(self):
        pass


class List(Definition):
    def __init__(self, element):
        super(List, self).__init__('List')
        self.element = element

    def _init(self):
        self.element = self.module.lookup(self.element)


class Set(Definition):
    def __init__(self, element):
        super(Set, self).__init__('Set')
        self.element = element

    def _init(self):
        self.element = self.module.lookup(self.element)


class Map(Definition):
    def __init__(self, key, value):
        super(Map, self).__init__('Map')
        self.key = key
        self.value = value


class Enum(Definition):
    @classmethod
    def from_ast(cls, ast, module=None):
        check_isinstance(ast, ast.Enum)
        return Enum(ast.name, module=module, values=ast.values)

    def __init__(self, name, module=None, values=None):
        super(Enum, self).__init__(name, module=module)
        self.values = SymbolTable(self)
        if values:
            self.add_values(*values)

    def add_value(self, value_name):
        self.values.add(EnumValue(self, value_name))

    def add_values(self, *value_names):
        map(self.add_value, value_names)

    def __contains__(self, enum_value):
        return enum_value in self.values.as_map().values()


class EnumValue(object):
    def __init__(self, enum, name):
        self.enum = enum
        self.name = name


class Module(object):
    def __init__(self, name, definitions=None):
        self.name = name
        self.definitions = SymbolTable(self)

        self._ast = None
        self._linked = False
        self._inited = False

        if definitions:
            map(self.add_definition, definitions)

    def link_imports(self):
        if self._linked: return
        self._linked = True
        if not self._ast: return

    #        for node in self._ast.imports:
    #            imported = self.package.lookup(node.name)
    #            if not imported:
    #                raise ValueError('Import not found "%s"' % node.name)
    #
    #            self.add_import(imported, node.alias)

    def init(self):
        if self._inited: return
        self._inited = True

        for definition in self.definitions:
            definition.init()

    def add_definition(self, definition):
        check_isinstance(definition, Definition)

        self.definitions.add(definition)
        logging.info('%s: added "%s"', self, definition.simplename)

    def add_definitions(self, *definitions):
        map(self.add_definition, definitions)

    def lookup(self, name_or_ref):
        from pdef.ast import Ref
        if isinstance(name_or_ref, Ref):
            d = self._lookup_ref(name_or_ref)
        else:
            d = self._lookup_name(name_or_ref)

        if d and isinstance(d, Definition):
            d.init()

        return d

    def _lookup_ref(self, ref):
        from pdef.ast import Ref
        check_isinstance(ref, Ref)

        rawtype = self.lookup(ref.name)
        if not rawtype:
            raise ValueError('%s: type "%s" is not found' % (self, ref))

        if not rawtype.generic:
            if ref.variables:
                raise ValueError('%s: type "%s" does not have generic variables, got %s' %
                                 (self, rawtype, ref))
            return rawtype

        vars = tuple(self.lookup(var) for var in ref.variables)
        return rawtype.parameterize(*vars)

    def _lookup_name(self, name):
        symbol = self._lookup_child(name)
        if symbol is not None:
            return symbol

        if self.parent:
            return self.parent.lookup(name)

    def _lookup_child(self, name):
        if name in self.symbols:
            return self.symbols[name]

        if '.' not in name:
            return

        child_parts = deque(name.split('.'))
        parent_parts = [child_parts.popleft()]

        while child_parts:
            parent_name = '.'.join(parent_parts)
            if parent_name not in self.symbols:
                parent_parts.append(child_parts.popleft())
                continue

            child_name = '.'.join(child_parts)
            parent = self.symbols[parent_name]
            return parent._lookup_child(child_name)


class SymbolTable(object):
    def __init__(self, parent=None):
        self.parent = parent
        self.items = []
        self.map = {}

    def __eq__(self, other):
        if not isinstance(other, SymbolTable):
            return False
        return self.items == other.items

    def __iter__(self):
        return iter(self.items)

    def __len__(self):
        return len(self.items)

    def __contains__(self, key):
        return key in self.map

    def __getitem__(self, key):
        return self.map[key]

    def __setitem__(self, name, item):
        if name in self.map:
            s = 'duplicate symbol "%s"' % name
            if self.parent:
                s = '%s: %s' % (self.parent, s)
            raise ValueError(s)

        self.map[name] = item
        self.items.append(item)

    def __add__(self, other):
        new = SymbolTable()
        new += self
        new += other
        return new

    def __iadd__(self, other):
        for item in other:
            self.add(item)
        return self

    def add(self, item, name=None):
        name = name if name else item.name
        self[name] = item

    def get(self, name, default=None):
        return self.map.get(name, default)

    def as_map(self):
        return dict(self.map)
