# encoding: utf-8
import logging
from collections import deque
from pdef_lang import definitions, exc


class Module(object):
    '''Module is a named scope for definitions. It is usually a *.pdef file.'''
    def __init__(self, name, imports=None, definitions=None):
        self.name = name
        self.package = None

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

        logging.debug('%s: added a definition %s', self, def0)

    def get_definition(self, name):
        '''Find a definition in this module by a name.'''
        for d in self.definitions:
            if d.name == name:
                return d

    def link(self):
        '''Link imports and definitions and return a list of errors.'''
        errors = self._link_imports()
        if errors:
            return errors

        return self._link_definitions()

    def _link_imports(self):
        '''Link imports, must be called before link_module_defs().'''
        if not self.package:
            raise ValueError('Module must be in a package, %s' % self)

        errors = []
        package = self.package

        for import0 in self.imports:
            imodules, errors0 = import0.link(package)
            self.imported_modules += imodules
            errors += errors0

        return errors

    def _link_definitions(self):
        '''Link imports and definitions.'''
        scope = lambda name: self._find(name)

        errors = []
        for def0 in self.definitions:
            errors += def0.link(scope)

        return errors

    def validate(self):
        '''Validate imports and definitions and return a list of errors.'''
        errors = []

        # Prevent imports with duplicate aliases.
        names = set()
        for imported_module in self.imported_modules:
            alias = imported_module.alias
            if alias in names:
                errors.append(exc.error(imported_module, 'duplicate import %r' % alias))
            names.add(alias)

        # Prevent definitions and imports with duplicate names.
        for def0 in self.definitions:
            name = def0.name
            if name in names:
                errors.append(exc.error(def0, 'duplicate definition or import %r' % name))
            names.add(name)

        for def0 in self.definitions:
            errors += def0.validate()

        return errors

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

    def _find(self, name):
        '''Find a type by a name.'''

        # Try to get a native type.
        def0 = definitions.NativeType.get(name)
        if def0:
            return def0

        # Try to find a type or an enum value in the current module.
        def0 = self._find_definition(name)
        if def0:
            return def0

        # Try to find an imported type.
        left = []
        right = name.split('.')
        while right:
            left.append(right.pop(0))
            lname = '.'.join(left)
            rname = '.'.join(right)

            imodule = self.get_imported_module(lname)
            if not imodule:
                continue

            # Try to get a definition or an enum value from the imported module.
            def0 = imodule._find_definition(rname)
            if def0:
                return def0

                # Still can have more imports to check, i.e.:
                # import com.project
                # import com.project.submodule

        return None

    def _find_definition(self, name):
        '''Find a definition or an enum value by a name inside this module.'''
        if '.' not in name:
            # It must be a user-defined type.
            return self.get_definition(name)

        # It can be an enum value.
        left, right = name.split('.', 1)

        enum = self.get_definition(left)
        if enum and enum.is_enum:
            return enum.get_value(right)

        return None


class AbstractImport(object):
    '''AbstractImport is a base class for module imports.'''
    def __init__(self):
        self.module = None

    def link(self, package):
        '''Link this import and return a list of imported modules and a list errors.'''
        errors = []
        imodules = []
        return imodules, errors


class AbsoluteImport(AbstractImport):
    '''AbsoluteImport references a single module by its absolute name.'''
    def __init__(self, name):
        super(AbsoluteImport, self).__init__()
        self.name = name

    def __str__(self):
        return self.name

    def __repr__(self):
        return '<%s %s at %s>' % (self.__class__.__name__, self.name, hex(id(self)))

    def link(self, package):
        imodule = package.get_module(self.name)
        if imodule:
            return [ImportedModule(self.name, imodule)], []

        return [], [exc.error(self, 'module not found %r', self.name)]


class RelativeImport(AbstractImport):
    '''RelativeImport references modules with a prefix and multiple relative names,
    i.e, from my_package import module0, module1.'''
    def __init__(self, prefix, relative_names):
        super(RelativeImport, self).__init__()

        self.prefix = prefix
        self.relative_names = tuple(relative_names)

    def __str__(self):
        return '"from ' + self.prefix + ' import ' + ', '.join(self.relative_names) + '"'

    def __repr__(self):
        return '<%s %s at %s>' % (self.__class__.__name__, self, hex(id(self)))

    def link(self, package):
        imodules = []
        errors = []

        for rname in self.relative_names:
            fullname = self.prefix + '.' + rname

            imodule = package.get_module(fullname)
            if imodule:
                imodules.append(ImportedModule(rname, imodule))
            else:
                errors.append(exc.error(self, 'module not found %r', fullname))

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
