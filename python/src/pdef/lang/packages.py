# encoding: utf-8
from pdef import ast
from pdef.lang import Node, Module, SymbolTable
from pdef.preconditions import *


class Package(Node):
    @classmethod
    def from_node(cls, node, pdef=None):
        check_isinstance(node, ast.Package)

        package = Package(node.name, version=node.version, pdef=pdef)
        package.node = node

        for mnode in node.modules:
            module = Module.from_node(mnode, package=package)
            package.add_module(module)

        return package

    def __init__(self, name, version=None, pdef=None):
        super(Package, self).__init__(name)
        self.version = version
        self.pdef = pdef
        self.node = None

        self.dependencies = SymbolTable(self)
        self.modules = SymbolTable(self)

        self.linked = False
        self.inited = False

    def link(self):
        if self.linked: return
        self.linked = True
        if not self.node: return

        for depname in self.node.dependencies:
            dep = self.pdef.package(depname)
            self.add_dependency(dep)

        for module in self.modules:
            module.link()

    def init(self):
        if self.inited: return
        self.inited = True

        for module in self.modules:
            module.init()

    def add_dependency(self, dep):
        check_isinstance(dep, Package)
        self.dependencies.add(dep)
        self.symbols.add(dep)

    def add_module(self, module):
        check_isinstance(module, Module)

        if not module.name.startswith(self.name):
            raise ValueError('%s: module "%s" name must start with the package name' %
                             (self, module))
        self.modules.add(module)
        self.symbols.add(module)

    def add_modules(self, *modules):
        map(self.add_module, modules)
