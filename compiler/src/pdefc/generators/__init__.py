# encoding: utf-8
import inspect
import logging
import os
import pkg_resources
from jinja2 import Environment


GENERATORS_ENTRY_POINT = 'pdefc.generators'


class Generator(object):
    def generate(self, package):
        raise NotImplementedError


def find_generators(entry_point=GENERATORS_ENTRY_POINT):
    '''Dynamically load the source code generators, return a dict {name: generator}.'''
    result = {}
    for entry_point in pkg_resources.iter_entry_points(group=entry_point):
        name = entry_point.name
        generator = entry_point.load()
        result[name] = generator
        logging.debug('Loaded a source code generator %r', name)

    return result


class Templates(object):
    '''Templates class is a default Jinja templates loader relative to a directory or a file.

    Example::
        >>> templates = Templates(__file__)
        >>> templates.get('my_jinja.template')
    '''
    def __init__(self, dir_or_file, filters=None):
        '''Create a templates loader relative to a directory or a file.'''
        if os.path.isdir(dir_or_file):
            self._dir = dir_or_file
        else:
            self._dir = os.path.dirname(dir_or_file)

        self._env = Environment(trim_blocks=True, lstrip_blocks=True)
        self._cache = {}

        self.add_filter('upper_first', upper_first)
        if isinstance(filters, dict):
            self.add_filters(**filters)
        elif filters:
            self.add_filters_from_methods(filters)

    def add_filter(self, name, filter0):
        self._env.filters[name] = filter0
        logging.debug('Added a template filter "%s"' % name)

    def add_filters(self, **name_to_filter):
        for name, filter0 in name_to_filter.items():
            self.add_filter(name, filter0)

    def add_filters_from_methods(self, obj):
        for name in dir(obj):
            if name.startswith('_'):
                continue

            attr = getattr(obj, name)
            if inspect.ismethod(attr):
                self.add_filter(name, attr)

    def get(self, name):
        '''Read and return a Jinja template, the templates are cached.'''
        if name in self._cache:
            return self._cache[name]

        # Get the template file.
        path = os.path.join(self._dir, name)
        with open(path, 'r') as module_file:
            text = module_file.read()

        template = self._env.from_string(text)
        self._cache[name] = template

        return template


class Namespace(object):
    '''Namespace class is a default namespace mapper, which maps pdef modules names to
    prefix names.

    Example::
        >>> namespace = Namespace({'pdef.tests': 'pdef_tests'})
        >>> namespace.map('pdef.tests.messages')
        >>> 'pdef_tests.messages'
    '''
    def __init__(self, namespace=None):
        self._namespace = dict(namespace) if namespace else {}

    def __call__(self, module_name):
        return self.map(module_name)

    def map(self, name):
        '''Returns a new name.'''
        for prefix, mapped in self._namespace.items():
            if name == prefix:
                # Full match, service.module => service_module.
                return mapped

            if name.startswith(prefix + '.'):
                return mapped + name[len(prefix):]

        return name


def upper_first(s):
    '''Uppercase the first letter in a string.'''
    if not s:
        return s
    return s[0].upper() + s[1:]


def mkdir_p(dirname):
    '''Make directories, ignore errors'''
    if os.path.exists(dirname):
        return
    os.makedirs(dirname)
