# encoding: utf-8
import inspect
import logging
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


def upper_first(s):
    '''Uppercase the first letter in a string.'''
    if not s:
        return s
    return s[0].upper() + s[1:]



def mkdir_p(dirname):
    if os.path.exists(dirname):
        return
    os.makedirs(dirname)
