# encoding: utf-8
from pdef.preconditions import *
from pdef.lang import errors
from pdef.lang.nodes import Symbol, SymbolTable


class Type(Symbol):
    def __init__(self, name, variables=None):
        super(Type, self).__init__(name)

        self.rawtype = self
        self.variables = SymbolTable(self)
        if variables:
            self.add_variables(*variables)

    @property
    def fullname(self):
        s = super(Type, self).fullname
        if self.variables:
            s += '<' + ', '.join(var.name for var in self.variables) + '>'
        return s

    @property
    def generic(self):
        return bool(self.variables)

    def add_variables(self, *vars):
        for var in vars:
            self.variables.add(var)
            self._add_symbol(var)

    def parameterize(self, *variables):
        '''Create a parameterized type.'''
        raise NotImplementedError('Implement in a subclass')

    def bind(self, arg_map):
        '''Parameterized types and variables should redefine this method.'''
        return self


class ParameterizedType(Type):
    def __init__(self, rawtype, *variables):
        super(ParameterizedType, self).__init__(rawtype.name)
        check_argument(len(rawtype.variables) == len(variables),
                       "wrong number of variables %s", variables)

        self.rawtype = rawtype
        self.variables = SymbolTable(self)
        for var, arg in zip(self.rawtype.variables, variables):
            self.variables.add_with_name(var, arg)
            self._add_child(arg)

    def bind(self, arg_map):
        bvariables = []
        for arg in self.variables:
            barg = arg.bind(arg_map)
            bvariables.append(barg)

        return self.package.parameterized_symbol(self.rawtype, *bvariables)


class Variable(Type):
    def __init__(self, name):
        super(Variable, self).__init__(name)

    def bind(self, arg_map):
        '''Find this variable in the arg map and return the value.'''
        svar = arg_map.get(self)
        if svar:
            return svar

        errors.add(self, 'variable is not found in the variables map')

