# encoding: utf-8
import inspect
import logging
import os.path
from jinja2 import Environment
from pdef.common import mkdir_p


class AbstractTranslator(object):
    '''Abstract pdef translator.'''

    def __init__(self, out):
        self.out = out
        self.env = Environment(trim_blocks=True)

    def write(self, module_name, file_name, code):
        '''Writes a code to a file in a module directory.'''
        dirs = module_name.split('.')
        fulldir = os.path.join(self.out, os.path.join(*dirs))
        mkdir_p(fulldir)

        fullpath = os.path.join(fulldir, file_name)
        with open(fullpath, 'wt') as f:
            f.write(code)

        logging.info('Created %s', fullpath)

    def read_template(self, name):
        '''Reads and returns a template by name.'''
        f = inspect.getfile(self.__class__)
        path = os.path.join(os.path.dirname(f), name)
        with open(path, 'r') as f:
            text = f.read()
        return self.env.from_string(text)
