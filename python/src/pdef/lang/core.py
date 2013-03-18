# encoding: utf-8
import logging
from pdef.lang import SymbolTable


class Pdef(object):
    def __init__(self):
        self.packages = SymbolTable()
        self.sources = []

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
