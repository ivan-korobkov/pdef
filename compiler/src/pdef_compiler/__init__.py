import logging
import time

from pdef_compiler.lang import Package
from pdef_compiler.exc import PdefCompilerException


class Compiler(object):
    def __init__(self):
        self._generators = None

    @property
    def generators(self):
        if self._generators is None:
            self._generators = self._load_generators()

        return self._generators

    def parse_paths(self, paths):
        '''Parse paths into a package. The package is not linked no validated. '''
        t0 = time.time()

        package = Package()
        for path in paths:
            package.parse_path(path)

        t = (time.time() - t0) * 1000
        logging.info('Parsed files in %dms', t)
        return package

    def check(self, package):
        '''Link and validate a package.'''
        t0 = time.time()

        package.link()
        package.validate()

        t = (time.time() - t0) * 1000
        logging.info('Checked a package in %dms', t)

    def compile(self, package, **kwargs):
        '''Parse paths into a package, link and validate it, then generate source code.'''
        self.check(package)
        self.generate(package, **kwargs)
        return package

    def generate(self, package, **kwargs):
        '''Generate source code.'''
        t0 = time.time()

        for generator in self.generators:
            generator.generate(package, **kwargs)

        t = (time.time() - t0) * 1000
        logging.info('Generated source code in %dms', t)

    def _load_generators(self):
        '''Dynamically load source code generators.'''
        pass
