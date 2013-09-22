# encoding: utf-8
import inspect
import os

from jinja2 import Environment


def load_clis():
    '''Dynamically finds and instantiates source code generator command-line interfaces.'''
    pass


class GeneratorCli(object):
    '''Generator CLI interface based on the argparse module.'''
    def get_name(self):
        '''Should return a generator name.

        The name must follow a convention that if the package is pdef_mygenerator then
        the name is mygenerator.
        '''
        pass

    def fill_arg_group(self, group):
        '''Add generator-specific arguments to an argparse group.

        All arguments MUST start with the generator name.
        '''

    def generate(self, args, package):
        '''Generate source code using parsed argparse arguments.'''
        pass


class AbstractGenerator(object):
    '''Abstract source code generator based on Jinja templates.'''
    def __init__(self, out):
        super(AbstractGenerator, self).__init__()
        self.env = Environment(trim_blocks=True, lstrip_blocks=True)

    def generate(self, package):
        '''Generate source code from a pdef package.'''
        pass

    def read_template(self, name):
        '''Read and return a Jinja template relative to this generator module file.'''
        f = inspect.getfile(self.__class__)
        path = os.path.join(os.path.dirname(f), name)
        with open(path, 'r') as f:
            text = f.read()
        return self.env.from_string(text)
