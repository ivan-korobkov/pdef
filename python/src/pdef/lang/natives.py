# encoding: utf-8
from pdef.lang import Type, Variable, ParameterizedType


class Native(Type):
    @classmethod
    def from_node(cls, node):
        options = NativeOptions(**node.options)
        return Native(node.name, variables=(Variable(var) for var in node.variables),
                      options=options)

    def __init__(self, name, variables=None, options=None, module=None):
        super(Native, self).__init__(name, variables, module=module)
        self.options = options

    def _do_parameterize(self, variables):
        '''Parameterize this native with the given variables and return a new one.'''
        return ParameterizedNative(self, variables)


class NativeOptions(object):
    def __init__(self, java_type=None, java_boxed=None, java_descriptor=None, java_default=None):
        self.java_type = java_type
        self.java_boxed = java_boxed
        self.java_descriptor = java_descriptor
        self.java_default = java_default


class ParameterizedNative(ParameterizedType):
    @property
    def options(self):
        return self.rawtype.options
