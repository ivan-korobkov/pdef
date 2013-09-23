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

    def parse_cli_args(self, args):
        '''Parse generator-specific arguments from an argparse Namespace.

        @return a dictionary used as **kwargs.
        '''
        raise NotImplementedError

    def create_generator(self, **kwargs):
        '''Create a source code generator.'''
        raise NotImplementedError


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
