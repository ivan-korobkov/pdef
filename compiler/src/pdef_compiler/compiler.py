# encoding: utf-8
import logging
import time

import pdef_lang
import pdef_compiler


def create_compiler():
    '''Creates a compiler, the compiler is reusable but not thread-safe.'''
    return Compiler()


class CompilerException(Exception):
    def __init__(self, message, errors=None):
        self.message = message
        self.errors = errors


class Compiler(object):
    '''Compiler parses and compiles paths into a valid package.
    It is reusable but not thread-safe.'''
    def __init__(self):
        self._generator_modules = None

    @property
    def generator_modules(self):
        if self._generator_modules is None:
            self._generator_modules = list(self._load_generator_modules())
        return self._generator_modules

    def compile(self, *paths):
        '''Parse paths into a package, link, validate and return it.'''
        package, errors = self._parse(paths)
        if errors:
            raise CompilerException('Parsing errors', errors)

        errors = self._compile(package)
        if errors:
            raise CompilerException('Compilation errors', errors)
        return package

    def _parse(self, paths):
        '''Parse a package from paths, return a package and a list of errors.'''
        t0 = time.time()

        parser = pdef_compiler.create_parser()
        package = pdef_lang.Package()
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

        errors = package.link()
        if errors:
            return errors

        errors = package.build()
        if errors:
            return errors

        errors = package.validate()
        if errors:
            return errors

        t = (time.time() - t0) * 1000
        logging.info('Compiled a package in %dms', t)
        return []

    def _load_generator_modules(self):
        '''Dynamically load source code generator modules.'''
        return pdef_compiler.list_generator_modules()
