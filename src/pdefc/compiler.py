# encoding: utf-8
import logging
import time

import pdefc
from pdefc.exc import CompilerException
from pdefc.lang.packages import Package


def create_compiler():
    '''Creates a compiler, the compiler is reusable but not thread-safe.'''
    return Compiler()


class Compiler(object):
    '''Compiler parses and compiles packages. It is reusable but not thread-safe.'''
    def __init__(self, sources=None, parser=None):
        self.parser = parser or pdefc.create_parser()
        self.sources = sources or pdefc.create_sources()
        self.packages = {}

        self._generator_classes = None

    @property
    def generator_classes(self):
        '''Return {name: GeneratorClass}.'''
        if self._generator_classes is None:
            self._generator_classes = dict(pdefc.find_generator_classes())
        return self._generator_classes

    def add_paths(self, *paths):
        '''Add source paths.'''
        for path in paths:
            self.sources.add_path(path)

    def check(self, path):
        '''Compile a package, return True if correct, else raise a CompilerException.'''
        package = self.compile(path)
        logging.info('%s is valid', package.name)
        return True

    def compile(self, path):
        '''Compile a package from a path.'''
        t0 = time.time()

        source = self.sources.add_path(path)
        package = self.package(source.name)

        t = (time.time() - t0) * 1000
        logging.info('Fetched and compiled %s in %dms', package.name, t)
        return package

    def package(self, name, _names=None):
        '''Return a compiled package by its name or raise a CompilerException.'''

        # Prevent circular dependencies.
        names = _names or set()
        if name in names:
            raise CompilerException('Circular package dependencies in %s' % name)
        names.add(name)

        # Try to get a compiled package.
        if name in self.packages:
            return self.packages[name]

        # Find a package source.
        source = self.sources.get(name)

        # Parse the source.
        package = self._parse(source)
        self.packages[name] = package

        # Compile the dependencies.
        for dname in package.info.dependencies:
            dep = self.package(dname, _names=names)
            package.add_dependency(dep)

        # Compile the package.
        package.compile()
        return package

    def _parse(self, source):
        '''Parse and return a package, but not its dependencies.'''
        logging.info('Parsing %s', source)

        # Parse module sources.
        modules = self.parser.parse_sources(source.module_sources)

        # Create the package.
        return Package(source.name, info=source.info, modules=modules)
