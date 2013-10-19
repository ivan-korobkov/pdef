# encoding: utf-8
import logging


class Package(object):
    '''Protocol definition.'''
    def __init__(self, modules=None, includes=None):
        self.modules = []
        self.includes = []

        if modules:
            map(self.add_module, modules)

        if includes:
            map(self.include, includes)

    def __str__(self):
        return 'package'

    def add_module(self, module):
        '''Add a module to this package.'''
        if module.package:
            raise ValueError('Module is already in a package, %s' % module)

        self.modules.append(module)
        module.package = self

        logging.debug('Added a module %r', module.name)

    def include(self, package):
        '''Adds all modules from another package to this package dependencies.'''
        if package is self:
            raise ValueError('Cannot include itself %r' % self)

        map(self.include_module, package.modules)

    def include_module(self, module):
        '''Adds a module to this package dependencies.'''
        self.includes.append(module)

        logging.debug('Included a module %r', module.name)

    def get_module(self, name):
        '''Find a module by its name.'''
        for module in self.modules:
            if module.name == name:
                return module

        for module in self.includes:
            if module.name == name:
                return module

    def compile(self):
        '''Compile this package and return a list of errors.'''
        logging.debug('Compiling the package')

        errors = self.link()
        if errors:
            return errors

        errors = self.build()
        if errors:
            return errors

        errors = self.validate()
        if errors:
            return errors

        return []

    def link(self):
        '''Link this package and return a list of errors.'''
        logging.debug('Linking the package')

        errors = []

        # Prevent duplicate module names.
        names = set()
        for module in self.modules:
            if module.name in names:
                errors.append('Duplicate module %r' % module.name)
            names.add(module.name)

        # Prevent name clashes with included modules.
        names = set(m.name for m in self.includes)
        for module in self.modules:
            if module.name in names:
                errors.append('Module clashes with an included module %r' % module.name)

        if errors:
            return errors

        # Link modules.
        for module in self.modules:
            errors += module.link()

        return errors

    def build(self):
        '''Build this package and return a list of errors.'''
        logging.debug('Building the package')

        errors = []
        for module in self.modules:
            errors += module.build()
        return errors

    def validate(self):
        '''Validate this package and return a list of errors.'''
        logging.debug('Validating the package')

        errors = []
        for module in self.modules:
            errors += module.validate()

        return errors
