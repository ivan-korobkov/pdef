# encoding: utf-8
from collections import deque
from pdef.preconditions import *


class Symbol(object):
    def __init__(self, name):
        super(Symbol, self).__init__()
        self.name = name


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


class Node(Symbol):
    parent = None
    generic = False
    variables = ()

    def __init__(self, name):
        super(Node, self).__init__(name)
        self.symbols = SymbolTable()

    def __repr__(self):
        return '<%s %s>' % (self.__class__.__name__, self.fullname)

    def __str__(self):
        return self.fullname

    def init(self):
        pass

    @property
    def simplename(self):
        return self.name

    @property
    def fullname(self):
        if self.parent:
            return '%s %s' % (self.parent.fullname, self.name)
        return self.name

    def lookup(self, name_or_ref):
        from pdef.ast import Ref
        if isinstance(name_or_ref, Ref):
            node = self._lookup_ref(name_or_ref)
        else:
            node = self._lookup_name(name_or_ref)

        if node:
            node.init()

        return node

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
