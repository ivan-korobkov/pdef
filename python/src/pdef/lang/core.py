# encoding: utf-8
import logging
from pdef.lang import SymbolTable


class Pdef(object):
    def __init__(self):
        self.packages = SymbolTable()
        self.sources = []

    def package(self, name):
        if name not in self.packages:
            self._load_package(name)

        return self.packages[name]

    def add_package(self, package):
        self.packages.add(package)
        logging.info('Added a package %s', package)

    def add_packages(self, *packages):
        map(self.add_package, packages)

    def _load_package(self, package_name):
        for source in self.sources:
            node = source.get(package_name)
            if node:
                break

        package = parse_package(node)
        self.packages.add(package)
        package.init()
