# encoding: utf-8
import logging
from pdef_lang import exc


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
        '''Link this package and return a list of errors.'''
        errors = []

        # Prevent duplicate module names.
        names = set()
        for module in self.modules:
            if module.name in names:
                errors.append(exc.error(self, 'duplicate module %r', module.name))
            names.add(module.name)

        if errors:
            raise exc.LinkingException(errors)

        # Link modules.
        for module in self.modules:
            errors += module.link()

        if errors:
            raise exc.LinkingException(errors)

    def build(self):
        '''Build this package and return a list of errors.'''
        errors = []
        for module in self.modules:
            errors += module.build()
        return errors

    def validate(self):
        '''Validate this package and return a list of errors.'''
        errors = []
        for module in self.modules:
            errors += module.validate()

        if errors:
            raise exc.ValidationException(errors)
