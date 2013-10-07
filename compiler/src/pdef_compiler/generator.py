# encoding: utf-8
import importlib
import inspect
import logging
import os
import pkgutil

from jinja2 import Environment


GENERATOR_MODULE_PREFIX = 'pdef_'
GENERATOR_NAME = 'generate_source_code'


class Generator(object):
    def generate(self, package):
        raise NotImplementedError


def generators():
    '''Dynamically load the source code generators, return a dict {name: generator}.'''
    generators = {}

    for module_finder, module_name, ispkg in pkgutil.iter_modules():
        if not module_name.startswith(GENERATOR_MODULE_PREFIX):
            continue

        try:
            module = importlib.import_module(module_name)
        except Exception as e:
            logging.error('Failed to import a possible source code generator module %r, e=%s'
                          % (module_name, e))
            continue

        if not hasattr(module, GENERATOR_NAME):
            continue

        name = module_name[len(GENERATOR_MODULE_PREFIX):]
        generator = getattr(module, GENERATOR_NAME)
        generators[name] = generator
        logging.debug('Loaded a source code generator %r', name)

    return generators


class Templates(object):
    '''Jinja templates relative to a class module.'''
    def __init__(self, dir_or_file):
        '''Create a templates loader relative to a directory or a file.
        Usually, you will define a special function in a generator module:
            >>> def pytemplates():
            ...     return Templates(__file__)
            >>>

        '''
        if os.path.isdir(dir_or_file):
            self._dir = dir_or_file
        else:
            self._dir = os.path.dirname(dir_or_file)

        self._env = Environment(trim_blocks=True, lstrip_blocks=True)
        self._cache = {}

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


class NameMapper(object):
    '''Utility class which maps module names to prefix names.

    Example::
        >>> mapper = NameMapper({'pdef.tests': 'pdef_tests'})
        >>> mapper.map('pdef.tests.messages')
        >>> 'pdef_tests.messages'
    '''
    def __init__(self, name_map=None):
        self.name_map = dict(name_map) if name_map else {}

    def __call__(self, module_name):
        return self.map(module_name)

    def map(self, module_name):
        '''Returns a new module name.'''
        for name, mapped in self.name_map.items():
            if module_name == name:
                # Full match, service.module => service_module.
                return mapped

            if module_name.startswith(name + '.'):
                return mapped + module_name[len(name):]

        return module_name

def upper_first(s):
    '''Uppercase the first letter in a string.'''
    if not s:
        return s
    return s[0].upper() + s[1:]



def mkdir_p(dirname):
    if os.path.exists(dirname):
        return
    os.makedirs(dirname)
