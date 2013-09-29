# encoding: utf-8
from collections import deque
import logging

from .validator import ValidatorError


class Module(object):
    '''Module in a pdef package, usually, a module is parsed from one file.'''
    def __init__(self, name, imports=None, definitions=None):
        self.name = name

        self.imports = []
        self.imported_modules = []
        self.definitions = []

        if imports:
            map(self.add_import, imports)

        if definitions:
            map(self.add_definition, definitions)

    def add_import(self, import0):
        '''Add a module import to this module.'''
        if import0.module:
            raise ValueError('Import is already in a module, %s' % import0)

        self.imports.append(import0)
        import0.module = self

    def add_imported_module(self, alias, module):
        '''Add an imported module to this module.'''
        imported = ImportedModule(alias, module)
        self.imported_modules.append(imported)
        return imported

    def get_imported_module(self, alias):
        '''Find a module by its import alias.'''
        for imported_module in self.imported_modules:
            if imported_module.alias == alias:
                return imported_module.module

    def add_definition(self, def0):
        '''Add a new definition to this module.'''
        if def0.module:
            raise ValueError('Definition is already in a module, def=%s,' % def0)

        self.definitions.append(def0)
        def0.module = self

        logging.debug('%s: added a definition, def=%s', self, def0.name)

    def add_definitions(self, *defs):
        '''Add definitions to this module.'''
        for def0 in defs:
            self.add_definition(def0)

    def get_definition(self, name):
        '''Find a definition in this module by a name.'''
        for d in self.definitions:
            if d.name == name:
                return d

    def link_imports(self, linker):
        '''Link imports, must be called before link_module_defs().'''
        errors = []

        for import0 in self.imports:
            imported_modules, errors0 = import0.link(linker)
            self.imported_modules += imported_modules
            errors += errors0

        return errors

    def link_definitions(self, linker):
        '''Link imports and definitions.'''
        errors = []

        for def0 in self.definitions:
            errors += def0.link(linker)

        return errors

    def validate(self):
        '''Validate imports and definitions.'''
        errors = []

        names = set()
        # Check duplicate imports.
        for imported_module in self.imported_modules:
            alias = imported_module.alias
            if alias in names:
                errors.append(ValidatorError(imported_module, 'duplicate import %r' % alias))

            names.add(alias)

        # Check definition duplicates.
        for def0 in self.definitions:
            name = def0.name
            if name in names:
                errors.append(ValidatorError(def0, 'duplicate definition or import %r' % name))

            names.add(name)

        for def0 in self.definitions:
            errors += def0.validate()

        return errors

    def _has_import_circle(self, module, another):
        '''Return true if a module has an import circle with another module.'''
        if another is self:
            return False

        q = deque(imp.module for imp in module.imported_modules)
        while q:
            m = q.pop()
            if m is module:
                return True

            for imp in m.imported_modules:
                q.append(imp.module)

        return False


class AbstractImport(object):
    def __init__(self):
        self.module = None
        self.module_names = ()

    def link(self):
        '''Link an import and return a tuple of imported modules and errors.'''
        module = self.module
        if not module:
            raise ValueError('Declaring module is None in %s' % self)

        package = module.package
        if not package:
            raise ValueError('Declaring package is None in %s' % module)

        errors = []
        imodules = []

        for name in self.module_names:
            imodule = package.get_module(name)
            if imodule:
                imodules.append(ImportedModule(name, imodule))
            else:
                errors.append(name)

        return imodules, errors


class AbsoluteImport(AbstractImport):
    def __init__(self, name):
        super(AbsoluteImport, self).__init__()

        self.name = name
        self.module_names = (name,)


class RelativeImport(AbstractImport):
    def __init__(self, prefix, relative_names):
        super(RelativeImport, self).__init__()

        self.prefix = prefix
        self.relative_names = relative_names
        self.module_names = tuple(prefix + '.' + name for name in relative_names)


class ImportedModule(object):
    '''Alias/module pair, i.e. from package.module import submodule.'''
    def __init__(self, alias, module):
        self.alias = alias
        self.module = module
