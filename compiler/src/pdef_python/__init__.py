# encoding: utf-8
from pdef_python.generator import PythonGenerator


def generate_source_code(package, out, namespaces=None, **kwargs):
    '''Python source code generator.'''
    return PythonGenerator(out, namespaces).generate(package)
