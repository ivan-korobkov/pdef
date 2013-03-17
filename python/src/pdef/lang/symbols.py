# encoding: utf-8
from collections import deque
from pdef.preconditions import *


class Symbol(object):
    def __init__(self, name):
        super(Symbol, self).__init__()
        self.name = name


class SymbolTable(object):
    def __init__(self):
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
            raise ValueError('Duplicate symbol "%s"' % name)

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

    def as_map(self):
        return dict(self.map)


class Node(Symbol):
    def __init__(self, name):
        super(Node, self).__init__(name)
        self.parent = None
        self.symbols = SymbolTable()

    def link(self):
        pass

    def init(self):
        pass

    @property
    def fullname(self):
        if self.parent:
            return '%s %s' % (self.parent.fullname, self.name)
        return self.name

    def lookup(self, name):
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
