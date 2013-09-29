# encoding: utf-8
import logging
from .linker import Linker, LinkerException
from .validator import ValidatorException


class Package(object):
    '''Protocol definition.'''
    def __init__(self, modules=None):
        self.modules = []

        if modules:
            map(self.add_module, modules)

    def __str__(self):
        return 'package'

    def add_module(self, module):
        '''Add a module to this package.'''
        if module.package:
            raise ValueError('Module is already in a package, %s' % module)

        self.modules.append(module)
        module.package = self

        logging.debug('%s: added a module %s', self, module)

    def get_module(self, name):
        '''Find a module by its name.'''
        for module in self.modules:
            if module.name == name:
                return module

    def link(self):
        errors = []
        linker = Linker()

        for module in self.modules:
            errors += module.link_imports(linker)

        for module in self.modules:
            errors += module.link_definitions(linker)

        if errors:
            raise LinkerException(errors)

    def validate(self):
        errors = []
        for module in self.modules:
            errors += module.validate()

        if errors:
            raise ValidatorException(errors)
