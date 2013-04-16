# encoding: utf-8
from pdef import ast
from pdef.preconditions import *
from pdef.lang.symbols import SymbolTable
from pdef.lang.types import Type


class Interface(Type):
    @classmethod
    def from_node(cls, node, module=None):
        check_isinstance(node, ast.Interface)
        interface = Interface(node.name, module=module)
        interface.node = node
        return interface

    def __init__(self, name, module=None):
        super(Interface, self).__init__(name, module)

        self.bases = []
        self.methods = SymbolTable(self)
        self.declared_methods = SymbolTable(self)

        self.node = None

    @property
    def parent(self):
        return self.module

    @property
    def all_bases(self):
        for base in self.bases:
            yield base
            for b in base.all_bases:
                yield b

    def _init(self):
        if not self.node:
            return
        node = self.node
        map(self.add_base, [self.lookup(base) for base in node.bases])

        for method_node in node.declared_methods:
            mname = method_node.name
            args = []
            for arg in method_node.args:
                arg_name = arg.name
                arg_type = self.lookup(arg.type)
                args.append(MethodArg(arg_name, arg_type))

            result = self.lookup(method_node.result) if method_node.result else None
            method = Method(mname, args, result)
            self.add_method(method)

    def add_base(self, base):
        check_isinstance(base, Interface)
        check_argument(base is not self, '%s: self inheritance', self)
        check_argument(base not in self.bases, '%s: duplicate base %s', self, base)
        all_bases = set(base.all_bases)
        check_argument(self not in all_bases, '%s: circular inheritance with %s', self, base)
        # TODO: multiple bases share the same base interface

        self.bases.append(base)
        for method in base.methods:
            self.methods.add(method)

    def add_method(self, method):
        check_isinstance(method, Method)
        self.methods.add(method)
        self.declared_methods.add(method)


class Method(object):
    is_declared = True
    def __init__(self, name, args=None, result=None):
        self.name = name
        self.args = SymbolTable(self)
        self.result = result
        if args:
            map(self.add_arg, args)

    def __repr__(self):
        return '<%s %s>' % (self.__class__.__name__, self)

    def __str__(self):
        return '"%s" %s returns %s' % (self.name, self.args, self.result)

    def add_arg(self, arg):
        check_isinstance(arg, MethodArg)
        self.args.add(arg)


class MethodArg(object):
    def __init__(self, name, type):
        self.name = name
        self.type = type

    def bind(self, vmap):
        btype = self.type.bind(vmap)
        if btype == self.type:
            return self
        return MethodArg(self.name, btype)
