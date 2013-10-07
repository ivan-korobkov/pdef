# encoding: utf-8
import logging
import time

import pdef_compiler
from pdef_compiler import ast


def create_compiler():
    '''Creates a compiler, the compiler is reusable but not thread-safe.'''
    return Compiler()


class CompilerException(Exception):
    def __init__(self, message, errors=None):
        super(CompilerException, self).__init__(message)
        self.errors = errors or ()

    def __unicode__(self):
        s = [self.message]
        for e in self.errors:
            s.append(unicode(e))

        return '\n'.join(s)

    def __str__(self):
        return unicode(self).encode('utf-8')


class Compiler(object):
    '''Compiler parses and compiles paths into a valid package.
    It is reusable but not thread-safe.'''
    def __init__(self):
        self._generators = None

    @property
    def generators(self):
        if self._generators is None:
            self._generators = dict(pdef_compiler.generators())
        return self._generators

    def check(self, paths):
        '''Parse and validate a package, then return True or throw CompilerException.'''
        package, errors = self._parse(paths)
        if errors:
            raise CompilerException('Parsing errors', errors)

        return True

    def compile(self, paths):
        '''Parse paths into a package, then link, validate and return it.'''
        package, errors = self._parse(paths)
        if errors:
            raise CompilerException('Parsing errors', errors)

        errors = self._compile(package)
        if errors:
            raise CompilerException('Compilation errors', errors)
        return package

    def generate(self, paths, outs, namespaces=None):
        '''Parse a package and generate source code.'''
        namespaces = namespaces or {}
        package = self.compile(paths)

        for gname, gout in outs.items():
            gnamespaces = namespaces.get(gname)
            self._generate(package, gname, out=gout, namespaces=gnamespaces)

    def _parse(self, paths):
        '''Parse a package from paths, return a package and a list of errors.'''
        t0 = time.time()

        parser = pdef_compiler.create_parser()
        package = ast.Package()
        errors = []

        for path in paths:
            modules, errors0 = parser.parse_path(path)
            errors += errors0

            for module in modules:
                package.add_module(module)

        t = (time.time() - t0) * 1000
        logging.info('Parsed files in %dms', t)
        return package, errors

    def _compile(self, package):
        t0 = time.time()

        errors = package.compile()
        if errors:
            return errors

        t = (time.time() - t0) * 1000
        logging.info('Compiled a package in %dms', t)
        return []

    def _generate(self, package, name, out, namespaces=None):
        logging.debug('Running %s generator' % name)
        generator = self.generators.get(name)
        if not generator:
            logging.error('Source code generator is not found %r' % name)
            return

        generator(package, out, namespaces=namespaces)
        logging.debug('Generated the source by %s generator' % name)
