# encoding: utf-8
from collections import deque
from pdef.lang import errors
from pdef.lang.nodes import Symbol, SymbolTable
from pdef.lang.walker import Walker


class Builder(object):
    def __init__(self, root):
        self.root = root
        self.walker = Walker(root)

    def build(self):
        self.link_module_refs()
        if errors.present():
            return

        self.link_refs()
        if errors.present():
            return

        self.built_pmessages()
        if errors.present():
            return

        self.check_circular_inheritance()
        if errors.present():
            return

        self.compile_polymorphism()
        if errors.present():
            return

        self.compile_base_type()
        if errors.present():
            return

        self.compile_fields()
        if errors.present():
            return

    def link_module_refs(self):
        for module_ref in self.walker.module_refs():
            module_ref.link()

    def link_refs(self):
        for ref in self.walker.refs():
            print(ref)
            ref.link()

    def check_circular_inheritance(self):
        for message in self.walker.messages():
            message.check_circular_inheritance()

    def built_pmessages(self):
        # Parameterized types are created only in the package their are defined in.
        # So, after building all ptypes from the package, no other ptypes will be created in it.
        for pkg in self.walker.packages():
            pkg.build_parameterized()

    def compile_polymorphism(self):
        for message in self.walker.messages():
            message.compile_polymorphism()

    def compile_base_type(self):
        for message in self.walker.messages():
            message.compile_base_type()

    def compile_fields(self):
        for message in self.walker.messages():
            message.compile_fields()


class Package(Symbol):
    def __init__(self, name, version=None, builtin_package=None):
        super(Package, self).__init__(name)
        self.version = version

        self.modules = SymbolTable(self)
        self.builtin = builtin_package
        self.parameterized = {}
        self.pqueue = deque()

        if builtin_package:
            for module in builtin_package.modules:
                for d in module.definitions:
                    self._add_symbol(d)

    @property
    def package(self):
        return self

    def add_modules(self, *modules):
        for module in modules:
            if not module.name.startswith(self.name):
                errors.add(module, 'module name must start with the package name "%s"', self.name)
                continue

            self.modules.add(module)
            self._add_symbol(module)

    def build(self):
        builder = Builder(self)
        builder.build()

    def build_parameterized(self):
        from pdef.lang import ParameterizedMessage

        while self.pqueue:
            ptype = self.pqueue.pop()
            if isinstance(ptype, ParameterizedMessage):
                ptype.build()

    def parameterized_symbol(self, rawtype, *variables):
        variables = tuple(variables)
        key = (rawtype, variables)
        if key in self.parameterized:
            return self.parameterized[key]

        if tuple(rawtype.variables) == variables:
            self.parameterized[key] = rawtype
            return rawtype

        # Create a proxy to allow parameterized circular references, i.e.:
        # Node<V>:
        #   Node<V> next
        ptype = rawtype.parameterize(*variables)

        self.parameterized[key] = ptype
        self.pqueue.append(ptype)
        return ptype


class Module(Symbol):
    def __init__(self, name, imports=None, definitions=None):
        super(Module, self).__init__(name)

        self.imports = SymbolTable(self)
        self.definitions = SymbolTable(self)

        if imports:
            self.add_imports(*imports)

        if definitions:
            self.add_definitions(*definitions)

    @property
    def fullname(self):
        return self.name

    def add_imports(self, *imports):
        for imp in imports:
            self.imports.add(imp)
            self._add_symbol(imp)

    def add_definitions(self, *definitions):
        for d in definitions:
            self.definitions.add(d)
            self._add_symbol(d)
