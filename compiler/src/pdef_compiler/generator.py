# encoding: utf-8
import importlib
import inspect
import os
import pkgutil

from jinja2 import Environment


GENERATOR_MODULE_PREFIX = 'pdef_'
GENERATOR_MODULE_FACTORY_NAME = 'module'


class Generator(object):
    def generate(self, package):
        raise NotImplementedError


class JinjaGenerator(Generator):
    '''Abstract source code generator based on Jinja2 templates.'''
    def __init__(self):
        self.env = Environment(trim_blocks=True, lstrip_blocks=True)

    def read_template(self, name):
        '''Read and return a Jinja template relative to this generator module file.'''
        f = inspect.getfile(self.__class__)
        path = os.path.join(os.path.dirname(f), name)
        with open(path, 'r') as f:
            text = f.read()
        return self.env.from_string(text)


class GeneratorModule(object):
    '''Generator module interface.'''
    def get_name(self):
        '''Return this generator module name.'''
        module = self.__class__.__module__
        if module.startswith(GENERATOR_MODULE_PREFIX):
            return module[len(GENERATOR_MODULE_PREFIX):]

        raise NotImplementedError

    def fill_cli_group(self, group):
        '''Add generator-specific arguments to an argparse group.'''
        raise NotImplementedError

    def create_generator_from_cli_args(self, args):
        '''Create an optional generator for command-line arguments, and return it or None.'''
        raise NotImplementedError


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


def iter_generator_modules():
    '''Dynamically load source code generator modules.'''
    for module_finder, name, ispkg in pkgutil.iter_modules():
        if not name.startswith(GENERATOR_MODULE_PREFIX):
            continue

        module = importlib.import_module(name)
        if not hasattr(module, GENERATOR_MODULE_FACTORY_NAME):
            continue

        gmodule = getattr(module, GENERATOR_MODULE_FACTORY_NAME)()
        if not isinstance(gmodule, GeneratorModule):
            continue

        yield gmodule


def upper_first(s):
    '''Uppercase the first letter in a string.'''
    if not s:
        return s
    return s[0].upper() + s[1:]



def mkdir_p(dirname):
    if os.path.exists(dirname):
        return
    os.makedirs(dirname)
