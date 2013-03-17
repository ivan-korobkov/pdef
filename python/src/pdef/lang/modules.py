# encoding: utf-8
from pdef import ast
from pdef.lang.symbols import SymbolTable, Node
from pdef.preconditions import *


class Package(Node):
    @classmethod
    def from_node(cls, node, pdef=None):
        check_isinstance(node, ast.Package)

        package = Package(node.name, version=node.version, pdef=pdef)
        package.node = node

        for mnode in node.modules:
            module = Module.from_node(mnode)
            package.add_module(module)

        return package

    def __init__(self, name, version=None, pdef=None):
        super(Package, self).__init__(name)
        self.version = version
        self.pdef = pdef
        self.node = None

        self.dependencies = SymbolTable()
        self.modules = SymbolTable()

        self.linked = False
        self.inited = False

    def link(self):
        if self.linked:
            return
        self.linked = True

        if not self.node:
            return

        for depname in self.node.dependencies:
            dep = self.pdef.package(depname)
            self.add_dependency(dep)

        for module in self.modules:
            module.link()

    def add_dependency(self, dep):
        check_isinstance(dep, Package)
        self.dependencies.add(dep)
        self.symbols.add(dep)

    def add_module(self, module):
        check_isinstance(module, Module)

        if not module.name.startswith(self.name):
            raise ValueError('Module %s name must start with the package name "%s"' %
                             (module, self.name))
        self.modules.add(module)
        self.symbols.add(module)

    def add_modules(self, *modules):
        map(self.add_module, modules)


class Module(Node):
    @classmethod
    def from_node(cls, node, package=None):
        from pdef.lang.datatypes import Message, Enum, Native
        check_isinstance(node, ast.Module)

        module = Module(node.name, package=package)
        module.node = node

        for dnode in node.definitions:
            if isinstance(dnode, ast.Message):
                definition = Message.from_node(dnode)
            elif isinstance(dnode, ast.Enum):
                definition = Enum.from_node(dnode)
            elif isinstance(dnode, ast.Native):
                definition = Native.from_node(dnode)
            else:
                raise ValueError('Unsupported definition node %s' % dnode)
            module.add_definition(definition)

        return module

    def __init__(self, name, package=None):
        super(Module, self).__init__(name)
        self.imports = SymbolTable()
        self.definitions = SymbolTable()
        self.package = package

        self.node = None
        self.linked = False
        self.inited = False

    def link(self):
        if self.linked:
            return
        self.linked = True

        if not self.node:
            return

        for node in self.node.imports:
            imported = self.package.lookup(node.name)
            if not imported:
                raise ValueError('Import not found "%s"' % node.name)

            self.add_import(imported, node.alias)

    @property
    def fullname(self):
        return self.name

    def add_import(self, module, alias=None):
        check_isinstance(module, Module)
        self.imports.add(module, alias)
        self.symbols.add(module, alias)

    def add_definition(self, definition):
        from pdef.lang.types import Type
        check_isinstance(definition, Type)
        self.definitions.add(definition)
        self.symbols.add(definition)

    def add_definitions(self, *definitions):
        map(self.add_definition, definitions)
