# encoding: utf-8
from pdef.lang.symbols import Symbol, SymbolTable


class Package(Symbol):
    def __init__(self, name, version=None, pdef=None):
        super(Package, self).__init__(name)
        self.version = version
        self.pdef = pdef

        self.modules = SymbolTable()

    def add_module(self, module):
        if not module.name.startswith(self.name):
            raise ValueError('Module %s name must start with the package name "%s"' %
                             (module, self.name))
        self.modules.add(module)

    def add_modules(self, *modules):
        map(self.add_module, modules)


class Module(Symbol):
    def __init__(self, name, package=None):
        super(Module, self).__init__(name)
        self.imports = SymbolTable()
        self.definitions = SymbolTable()

        self.package = package
        if package:
            package.add_module(self)

    @property
    def pdef(self):
        return self.package.pdef if self.package else None

    @property
    def fullname(self):
        return self.name

    def add_import(self, _import):
        self.imports.add(_import)

    def add_definition(self, definition):
        self.definitions.add(definition)

    def add_definitions(self, *definitions):
        map(self.add_definition, definitions)
