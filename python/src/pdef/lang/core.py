# encoding: utf-8
import logging
import os.path
from pdef.lang import SymbolTable
from pdef.sources import DirectorySource

PDEF_DIR = os.path.dirname(__file__)
GLOBALS_PACKAGE = 'pdef'
GLOBALS_MODULE = 'pdef.lang'


class Pdef(object):
    def __init__(self, globals_package=GLOBALS_PACKAGE, globals_module=GLOBALS_MODULE):
        self.packages = SymbolTable()
        self.sources = [DirectorySource(PDEF_DIR)]
        self._globals_package = globals_package
        self._globals_module = globals_module

        self.globals = None
        self.globals = self._load_globals()

    def package(self, name):
        if name in self.packages:
            return self.packages[name]

        package = self._load(name)
        self.add_package(package)

        package.link()
        package.init()
        return package

    def add_package(self, package):
        self.packages.add(package)
        logging.info('Added a package %s', package)

    def add_packages(self, *packages):
        map(self.add_package, packages)

    def add_source(self, source):
        self.sources.append(source)

    def add_sources(self, *sources):
        map(self.add_source, sources)

    def _load(self, name):
        from pdef.lang.packages import Package
        node = None
        for source in self.sources:
            node = source.get(name)
            if node:
                break
        if not node:
            raise ValueError('Package "%s" is not found' % name)

        return Package.from_node(node, pdef=self)

    def _load_globals(self):
        package = self.package(self._globals_package)
        module = package.modules[self._globals_module]
        return module.symbols
