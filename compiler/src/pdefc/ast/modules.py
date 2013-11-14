# encoding: utf-8
import logging
from collections import deque

from pdefc.ast.common import Located, Validatable
from pdefc.ast.types import NativeType


class ModuleErrors(object):
    '''ModuleErrors class combines a module path/name and its errors into a single error.
    This error supports pretty printing.'''
    def __init__(self, path_or_name, errors):
        self.name = path_or_name
        self.errors = errors

    def __unicode__(self):
        s = [self.name]
        for e in self.errors:
            s.append('  ' + unicode(e))

        return '\n'.join(s)

    def __str__(self):
        return unicode(self).encode('utf8')


class Module(Validatable):
    '''Module is a named scope for definitions. It is usually a *.pdef file.'''
    def __init__(self, name, imports=None, definitions=None, doc=None, path=None):
        self.name = name
        self.doc = doc
        self.path = path
        self.package = None

        self.imports = []
        self.imported_modules = []

        self.definitions = []
        self._definition_map = {}       # Performance optimization

        if imports:
            map(self.add_import, imports)

        if definitions:
            map(self.add_definition, definitions)

    def __str__(self):
        return self.name

    def __repr__(self):
        return '<%s %s at %s>' % (self.__class__.__name__, self.name, hex(id(self)))

    def _module_errors(self, errors):
        if not errors:
            return []

        return [ModuleErrors(self.path or self.name, errors)]

    def add_import(self, import0):
        '''Add a module import to this module.'''
        if import0.module:
            raise ValueError('Import is already in a module, %s' % import0)

        self.imports.append(import0)
        import0.module = self
        logging.debug('%s: added an import \'%s\'', self, import0)

    def add_imported_module(self, alias, module):
        '''Add an imported module to this module.'''
        imported = ImportedModule(alias, module)
        self.imported_modules.append(imported)

        logging.debug('%s: added an imported module %r:%r', self, alias, module)
        return imported

    def get_imported_module(self, alias):
        '''Find a module by its import alias.'''
        for imported_module in self.imported_modules:
            if imported_module.alias == alias:
                return imported_module.module

    def add_definition(self, def0):
        '''Add a new definition to this module.'''
        self.definitions.append(def0)
        self._definition_map[def0.name] = def0
        logging.debug('%s: added a definition %r', self, def0.name)

    def get_definition(self, name):
        '''Return a definition or an enum value in this module by a name.'''

        # Try to get a definition by name.
        if name in self._definition_map:
            return self._definition_map[name]

        # Return if not a relative name.
        if '.' not in name:
            return

        # Try to get an enum value.
        enum_name, value_name = name.split('.', 1)
        if enum_name not in self._definition_map:
            return

        enum = self._definition_map[enum_name]
        if not enum.is_enum:
            return

        return enum.get_value(value_name)

    def lookup(self, name):
        '''Find a definition or enum value in this module or imported modules.'''

        # Try to get a native type.
        def0 = NativeType.get(name)
        if def0:
            return def0

        # Try to find a type or an enum value in the current module.
        def0 = self.get_definition(name)
        if def0:
            return def0

        # Try to find an imported type.
        left = []
        right = name.split('.')
        while right:
            left.append(right.pop(0))
            lname = '.'.join(left)
            rname = '.'.join(right)

            module = self.get_imported_module(lname)
            if not module:
                continue

            # Try to get a definition or an enum value from the imported module.
            def0 = module.get_definition(rname)
            if def0:
                return def0

        return None

    # Link.

    def link(self, package=None):
        '''Link imports and definitions and return a list of errors.'''
        logging.debug('Linking %s as %s', self.path, self)

        if self.package:
            raise ValueError('Module is already linked, module=%s' % self)
        self.package = package

        errors = self._link_imports(package)
        if errors:
            return self._module_errors(errors)

        errors = self._link_definitions()
        return self._module_errors(errors)

    def _link_imports(self, package):
        '''Link imports, must be called before link_module_defs().'''
        errors = []

        for import0 in self.imports:
            imodules, errors0 = import0.link(package)
            self.imported_modules += imodules
            errors += errors0

        return errors

    def _link_definitions(self):
        '''Link imports and definitions.'''
        errors = []
        for def0 in self.definitions:
            errors += def0.link(self)

        return errors

    # Build.

    def build(self):
        '''Build definitions and return a list of errors.'''
        logging.debug('Building %s', self)

        errors = []
        for def0 in self.definitions:
            errors += def0.build()

        return self._module_errors(errors)

    # Validate.

    def _validate(self):
        '''Validate imports and definitions and return a list of errors.'''
        logging.debug('Validating %s', self)
        errors = []

        # Prevent imports with duplicate aliases.
        names = set()
        for imported_module in self.imported_modules:
            alias = imported_module.alias
            if alias in names:
                errors.append('Duplicate import %r' % alias)
            names.add(alias)

        # Prevent definitions and imports with duplicate names.
        for def0 in self.definitions:
            name = def0.name
            if name in names:
                errors.append('Duplicate definition or import %r' % name)
            names.add(name)

        for def0 in self.definitions:
            errors += def0.validate()

        return self._module_errors(errors)

    def _has_import_circle(self, another):
        '''Return true if this module has an import circle with another module.'''
        if another is self:
            return False

        q = deque(imp.module for imp in self.imported_modules)
        while q:
            m = q.pop()
            if m is self:
                return True

            for imp in m.imported_modules:
                q.append(imp.module)

        return False


class AbstractImport(Located):
    '''AbstractImport is a base class for module imports.'''
    def __init__(self, location=None):
        self.module = None
        self.location = location

    def link(self, package):
        '''Link this import and return a list of imported modules and a list errors.'''
        errors = []
        imodules = []
        return imodules, errors


class AbsoluteImport(AbstractImport):
    '''AbsoluteImport references a single module by its absolute name.'''
    def __init__(self, name, location=None):
        super(AbsoluteImport, self).__init__(location=location)
        self.name = name

    def __str__(self):
        return self.name

    def __repr__(self):
        return '<%s %s at %s>' % (self.__class__.__name__, self.name, hex(id(self)))

    def link(self, package):
        logging.debug('Linking %s', self)

        imodule = package.get_module(self.name)
        if imodule:
            return [ImportedModule(self.name, imodule)], []

        return [], [self._error('Module not found %r', self.name)]


class RelativeImport(AbstractImport):
    '''RelativeImport references modules with a prefix and multiple relative names,
    i.e, from my_package import module0, module1.'''
    def __init__(self, prefix, relative_names, location=None):
        super(RelativeImport, self).__init__(location=location)

        self.prefix = prefix
        self.relative_names = tuple(relative_names)

    def __str__(self):
        return '"from ' + self.prefix + ' import ' + ', '.join(self.relative_names) + '"'

    def __repr__(self):
        return '<%s %s at %s>' % (self.__class__.__name__, self, hex(id(self)))

    def link(self, package):
        logging.debug('Linking \'%s\'', self)

        imodules = []
        errors = []

        for rname in self.relative_names:
            fullname = self.prefix + '.' + rname

            imodule = package.get_module(fullname)
            if imodule:
                imodules.append(ImportedModule(rname, imodule))
            else:
                errors.append(self._error('Module not found %r', fullname))

        return imodules, errors


class ImportedModule(object):
    '''ImportedModule is a pair of an alias and a module. For example,
    in "from package import submodule" the alias is "submodule" and the module is "package.module".
    '''
    def __init__(self, alias, module):
        self.alias = alias
        self.module = module

    def __str__(self):
        return self.alias

    def __repr__(self):
        return '<%s %s at %s>' % (self.__class__.__name__, self.alias, hex(id(self)))
