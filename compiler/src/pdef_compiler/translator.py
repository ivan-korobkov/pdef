# encoding: utf-8
import inspect
import os

from jinja2 import Environment


class AbstractTranslator(object):
    '''Abstract pdef translator.'''

    def __init__(self, out):
        self.out = out
        self.env = Environment(trim_blocks=True, lstrip_blocks=True)

    def translate(self, package):
        raise NotImplementedError

    def read_template(self, name):
        '''Reads and returns a template by name.'''
        f = inspect.getfile(self.__class__)
        path = os.path.join(os.path.dirname(f), name)
        with open(path, 'r') as f:
            text = f.read()
        return self.env.from_string(text)


class NameMapper(object):
    '''Maps module names to prefix names.

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
