# encoding: utf-8
import logging
from pdef import ast
from pdef.lang.symbols import SymbolTable, Node
from pdef.preconditions import *


class Module(Node):
    @classmethod
    def from_node(cls, node, package=None):
        from pdef.lang.enums import Enum
        from pdef.lang.messages import Message
        from pdef.lang.natives import Native
        check_isinstance(node, ast.Module)

        module = Module(node.name, package=package)
        module.node = node

        for dnode in node.definitions:
            if isinstance(dnode, ast.Message):
                definition = Message.from_node(dnode, module=module)
            elif isinstance(dnode, ast.Enum):
                definition = Enum.from_node(dnode, module=module)
            elif isinstance(dnode, ast.Native):
                definition = Native.from_node(dnode, module=module)
            else:
                raise ValueError('%s: unsupported definition node %s' % (self, dnode))
            module.add_definition(definition)

        return module

    def __init__(self, name, package=None):
        super(Module, self).__init__(name)
        self.imports = SymbolTable(self)
        self.definitions = SymbolTable(self)
        self.package = package

        self.node = None
        self.linked = False
        self.inited = False

    def link(self):
        if self.linked: return
        self.linked = True
        if not self.node: return

        for node in self.node.imports:
            imported = self.package.lookup(node.name)
            if not imported:
                raise ValueError('Import not found "%s"' % node.name)

            self.add_import(imported, node.alias)

    def init(self):
        if self.inited: return
        self.inited = True

        for definition in self.definitions:
            definition.init()

    @property
    def fullname(self):
        return self.name

    def add_import(self, module, alias=None):
        check_isinstance(module, Module)
        self.imports.add(module, alias)
        self.symbols.add(module, alias)
        logging.info('%s: added an import "%s"', self, module)

    def add_definition(self, definition):
        from pdef.lang.types import Type
        check_isinstance(definition, Type)
        self.definitions.add(definition)
        self.symbols.add(definition)
        logging.info('%s: added a definition "%s"', self, definition)

    def add_definitions(self, *definitions):
        map(self.add_definition, definitions)
