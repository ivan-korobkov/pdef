# encoding: utf-8
from collections import deque
import logging
from pdef.preconditions import *


class Node(object):
    def __init__(self):
        self.parent = None
        self.children = []
        self.symbols = SymbolTable()
        self.errors = []

    def __repr__(self):
        return '<%s %s %s>' % (self.__class__.__name__, self.fullname, hex(id(self)))

    def _add_child(self, child, always_parent=True):
        from pdef.lang import Ref
        if child is None:
            return

        self.children.append(child)
        if always_parent or isinstance(child, Ref):
            child.parent = self

    def _add_symbol(self, symbol):
        check_isinstance(symbol, Symbol, '%s is not a Symbol', symbol)
        self._add_child(symbol)
        self.symbols.add(symbol)

    @property
    def fullname(self):
        if self.parent:
            return '%s %s' % (self.parent.fullname, self.__class__.__name__)
        return self.__class__.__name__

    @property
    def package(self):
        if self.parent is None:
            raise ValueError('Can\'t access the package, %s has no parent' % self)
        return self.parent.package

    def error(self, msg, *args):
        msg = '%s: %s' % (self.fullname, msg % args)
        logging.error(msg)

        # The parent can be absent if the node is not linked.
        if self.parent:
            self.parent.errors.append(msg)
        else:
            self.errors.append(msg)

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


class Symbol(Node):
    def __init__(self, name):
        super(Symbol, self).__init__()
        self.name = name

    @property
    def fullname(self):
        if self.parent:
            return '%s.%s' % (self.parent.fullname, self.name)

        return self.name

    @property
    def generic(self):
        return False


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

    def add(self, item):
        self.add_with_name(item.name, item)

    def add_with_name(self, name, item):
        if name in self.map:
            raise ValueError('Duplicate "%s"' % name)

        self.map[name] = item
        self.items.append(item)

    def as_map(self):
        return dict(self.map)
