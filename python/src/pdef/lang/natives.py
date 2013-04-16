# encoding: utf-8
from pdef.lang import Type, Variable, SymbolTable
from pdef.preconditions import check_isinstance, check_argument


class Native(Type):
    @classmethod
    def from_node(cls, node, module=None):
        options = NativeOptions(**node.options)
        return Native(node.name, variables=(Variable(var) for var in node.variables),
                      options=options, module=module)

    def __init__(self, name, variables=None, options=None, module=None):
        super(Native, self).__init__(name, module=module)
        self.options = options

        self.variables = SymbolTable()
        for var in (variables if variables else ()):
            check_isinstance(var, Variable)
            self.variables.add(var)
            self.symbols.add(var)

        self._pmap = {}

    @property
    def simplename(self):
        if not self.variables:
            return self.name
        return self.name + '<' + ', '.join(var.simplename for var in self.variables) + '>'

    @property
    def generic(self):
        return bool(self.variables)

    def parameterize(self, *variables):
        '''Create a parameterized type.'''
        vars = tuple(variables)
        if vars in self._pmap:
            return self._pmap[vars]

        ptype = ParameterizedNative(self, variables)
        return ptype


class NativeOptions(object):
    def __init__(self, java_type=None, java_boxed=None, java_descriptor=None, java_default=None,
                 java_is_primitive=False):
        self.java_type = java_type
        self.java_boxed = java_boxed
        self.java_descriptor = java_descriptor
        self.java_default = java_default
        self.java_is_primitive = java_is_primitive


class ParameterizedNative(Type):
    def __init__(self, rawtype, variables):
        super(ParameterizedNative, self).__init__(rawtype.name, module=rawtype.module)
        check_argument(len(rawtype.variables) == len(variables),
                       '%s: wrong number of variables %s', rawtype, variables)

        self.rawtype = rawtype
        self.variables = SymbolTable()
        for var, arg in zip(self.rawtype.variables, variables):
            self.variables[var] = arg

    @property
    def options(self):
        return self.rawtype.options
