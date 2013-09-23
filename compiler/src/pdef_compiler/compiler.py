# encoding: utf-8
import logging
import time

from pdef_compiler import lang, generator


class Compiler(object):
    def __init__(self):
        self._generator_modules = None

    @property
    def generator_modules(self):
        if self._generator_modules is None:
            self._generator_modules = list(self._iter_generator_modules())
        return self._generator_modules

    def compile(self, paths):
        '''Parse paths into a package, link, validate and return it.'''
        package = self._parse(paths)

        t0 = time.time()
        self._link_validate(package)
        t = (time.time() - t0) * 1000

        logging.info('Compiled a package in %dms', t)
        return package

    def _parse(self, paths):
        '''Parse paths into a package. The package is not linked no validated. '''
        t0 = time.time()

        package = lang.Package()
        for path in paths:
            package.parse_path(path)

        t = (time.time() - t0) * 1000
        logging.info('Parsed files in %dms', t)
        return package

    def _link_validate(self, package):
        package.link()
        package.validate()

    def _iter_generator_modules(self):
        '''Dynamically load source code generator modules.'''
        return generator.iter_generator_modules()


class CompilerException(Exception):
    '''Compiler exception.'''
    pass
