# encoding: utf-8
import logging
import time

import pdefc
from pdefc.exc import CompilerException
from pdefc.lang.packages import Package


def create_compiler(paths=None):
    '''Creates a compiler, the compiler is reusable but not thread-safe.'''
    sources = pdefc.create_sources(paths)
    return Compiler(sources)


class Compiler(object):
    '''Compiler parses and compiles packages. It is reusable but not thread-safe.'''
    def __init__(self, sources=None, parser=None):
        self.parser = parser or pdefc.create_parser()
        self.sources = sources or pdefc.create_sources()
        self.packages = {}

        self._generators = None

    @property
    def generators(self):
        if self._generators is None:
            self._generators = dict(pdefc.find_generators())
        return self._generators

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

    def generate(self, path, generator_name, out, module_names=None, prefixes=None):
        '''Generate a package from a path.

        @package_path       Path or url to a package yaml file.
        @generator_name     Generator name.
        @module_names       List of tuples, [('pdef.module', 'language.module')].
        @prefixes           List of tuples, [('pdef.module', 'ClassPrefix')].
        '''

        # Fail fast, get a generator factory.
        factory = self.generators.get(generator_name)
        if not factory:
            raise CompilerException('Source code generator not found: %s' % generator_name)

        # Parse and compile the package.
        package = self.compile(path)

        # Create a generator and generate source code.
        t0 = time.time()
        generator = factory(out, module_names=module_names, prefixes=prefixes)
        generator.generate(package)

        # Measure and long the code generation time.
        t = (time.time() - t0) * 1000
        logging.info('Generated %s code in %dms', generator_name, t)
