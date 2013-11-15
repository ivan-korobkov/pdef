# encoding: utf-8
import logging


class Package(object):
    '''Protocol definition.'''
    def __init__(self, name, info=None, modules=None, dependencies=None):
        self.name = name
        self.info = info
        self.modules = []
        self.dependencies = []

        if modules:
            map(self.add_module, modules)

        if dependencies:
            map(self.add_dependency, dependencies)

    def __str__(self):
        return self.name

    def __repr__(self):
        return '<Package %r at %s>' % (self.name, hex(id(self)))

    def add_dependency(self, package):
        '''Adds all modules from another package to this package dependencies.'''
        if package is self:
            raise ValueError('Cannot include itself %r' % self)

        self.dependencies.append(package)
        logging.debug('%s: added a dependency "%s"', self, package.name)

    def get_dependency(self, name):
        '''Return a package by a name.'''
        for dep in self.dependencies:
            if dep.name == name:
                return dep

    def add_module(self, module):
        '''Add a module to this package.'''
        self.modules.append(module)
        logging.debug('%s: added a module "%s"', self, module.name)

    def get_module(self, relative_name):
        '''Find a module in this package by a name.'''
        for module in self.modules:
            if module.relative_name == relative_name:
                return module

    def lookup_module(self, absolute_name):
        '''Find a module in this package or its dependencies by an absolute name.'''
        if '.' not in absolute_name:
            absolute_name = '%s.%s' % (absolute_name, absolute_name)

        pname, mname = absolute_name.split('.', 1)
        if pname == self.name:
            return self.get_module(mname)

        package = self.get_dependency(pname)
        if package:
            return package.get_module(mname)

        return None

    def compile(self):
        '''Compile this package and return a list of errors.'''
        logging.debug('Compiling the package')

        errors = self._link()
        if errors:
            return errors

        errors = self._build()
        if errors:
            return errors

        errors = self._validate()
        if errors:
            return errors

        return []

    def _link(self):
        '''Link this package and return a list of errors.'''
        logging.debug('Linking the package')

        errors = []

        # Prevent duplicate module names.
        names = set()
        for module in self.modules:
            if module.name in names:
                errors.append(self._error('Duplicate module %r', module.name))
            names.add(module.name)

        if errors:
            return errors

        # Link modules.
        for module in self.modules:
            errors += module.link(self)

        return errors

    def _build(self):
        '''Build this package and return a list of errors.'''
        logging.debug('Building the package')

        errors = []
        for module in self.modules:
            errors += module.build()
        return errors

    def _validate(self):
        '''Validate this package and return a list of errors.'''
        logging.debug('Validating the package')

        errors = []
        for module in self.modules:
            errors += module.validate()

        return errors

    def _error(self, msg, *args):
        record = msg % args if args else msg
        logging.error(record)
        return record


class PackageInfo(object):
    @classmethod
    def from_json(cls, s):
        import json
        d = json.loads(s)
        return PackageInfo.from_dict(d)

    @classmethod
    def from_dict(cls, d):
        name = d.get('name') or 'noname'
        description = d.get('description') or 'No description'
        modules = d.get('modules') or []
        dependencies = d.get('dependencies') or []

        return PackageInfo(name, description, modules=modules, dependencies=dependencies)

    def __init__(self, name, description=None, modules=None, dependencies=None):
        self.name = name
        self.description = description
        self.modules = list(modules) if modules else []
        self.dependencies = list(dependencies) if dependencies else []

    def to_dict(self):
        return {
            'name': self.name,
            'description': self.description,
            'modules': list(self.modules),
            'dependencies': list(self.dependencies)
        }

    def to_json(self):
        import json
        return json.dumps(self.to_dict())
