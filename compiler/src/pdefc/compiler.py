# encoding: utf-8
import logging
import time

import pdefc
from pdefc import ast


def create_compiler():
    '''Creates a compiler, the compiler is reusable but not thread-safe.'''
    return Compiler()


class CompilerException(Exception):
    def __init__(self, message, errors=None):
        super(CompilerException, self).__init__(message)
        self.errors = errors or ()


class Compiler(object):
    '''Compiler parses and compiles paths into a valid package.
    It is reusable but not thread-safe.'''
    def __init__(self):
        self._generators = None

    @property
    def generators(self):
        if self._generators is None:
            self._generators = dict(pdefc.find_generators())
        return self._generators

    def compile(self, paths, include_paths=None):
        '''Parse paths into a package, then link, validate and return it.'''
        package = self._parse(paths)

        if include_paths:
            another = self.compile(include_paths)
            package.include(another)
        
        errors = package.compile()
        if errors:
            raise CompilerException('Compilation errors', errors)

        return package

    def generate(self, generator_name, paths, out, namespace=None, include_paths=None):
        '''Parse a package and generate source code.'''
        generator = self.generators.get(generator_name)
        if not generator:
            raise CompilerException('Source code generator is not found: %r' % generator_name)

        t0 = time.time()

        package = self.compile(paths, include_paths=include_paths)
        namespace = namespace or {}
        generator(package, out, namespace=namespace)

        t = (time.time() - t0) * 1000
        logging.info('Generated %s code in %dms' % (generator_name, t))

    def _parse(self, paths):
        '''Parse a package and return it.'''
        t0 = time.time()

        parser = pdefc.create_parser()
        package = ast.Package()
        errors = []

        for path in paths:
            modules, errors0 = parser.parse_path(path)
            errors += errors0

            for module in modules:
                package.add_module(module)

        t = (time.time() - t0) * 1000
        logging.info('Parsed files in %dms', t)

        if errors:
            raise CompilerException('Parsing errors', errors)

        return package
