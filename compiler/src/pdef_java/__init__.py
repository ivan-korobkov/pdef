# encoding: utf-8
from pdef_java.generator import JavaGenerator


def generate_source_code(package, out, namespaces=None, **kwargs):
    '''Java source code generator'''
    return JavaGenerator(out).generate(package)
