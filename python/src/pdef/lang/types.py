# encoding: utf-8
from collections import deque
from pdef.preconditions import *
from pdef.lang.symbols import SymbolTable, Node


class Type(Node):
    def __init__(self, name, variables=None, module=None):
        super(Type, self).__init__(name)
        self.module = module
        self.rawtype = self
        self.inited = False

        self.variables = SymbolTable()
        for var in (variables if variables else ()):
            check_isinstance(var, Variable)
            self.variables.add(var)
            self.symbols.add(var)

        self._pqueue = deque()
        self._pmap = {}

    @property
    def fullname(self):
        s = '%s.%s' % (self.parent.fullname, self.name) if self.parent else self.name
        if self.variables:
            s += '<' + ', '.join(var.name for var in self.variables) + '>'
        return s

    @property
    def generic(self):
        return bool(self.variables)

    def init(self):
        if self.inited:
            return

        self.inited = True
        self._do_init()
        self._init_parameterized()

    def bind(self, arg_map):
        '''Parameterized types and variables should redefine this method.'''
        return self

    def parameterize(self, *variables):
        '''Create a parameterized type.'''
        vars = tuple(variables)
        if vars in self._pmap:
            return self._pmap[vars]

        ptype = self._do_parameterize(variables)
        self._pmap[vars] = ptype
        self._pqueue.append(ptype)

        if self.inited:
            self._init_parameterized()

        return ptype

    def _init_parameterized(self):
        while self._pqueue:
            ptype = self._pqueue.pop()
            ptype.init()

    def _do_init(self):
        raise NotImplementedError

    def _do_parameterize(self, *variables):
        raise NotImplementedError


class ParameterizedType(Type):
    def __init__(self, rawtype, variables):
        super(ParameterizedType, self).__init__(rawtype.name, module=rawtype.module)
        check_argument(len(rawtype.variables) == len(variables),
                       'Wrong number of variables %s for %s', variables, rawtype)

        self.rawtype = rawtype
        for var, arg in zip(self.rawtype.variables, variables):
            self.variables[var] = arg

    def bind(self, arg_map):
        bvariables = []
        for arg in self.variables:
            barg = arg.bind(arg_map)
            bvariables.append(barg)

        return self.rawtype.parameterize(*bvariables)

    def parameterize(self, *variables):
        raise NotImplementedError('Parameterized types do not support parameterization, use bind')


class Variable(Type):
    def __init__(self, name):
        super(Variable, self).__init__(name)

    def bind(self, arg_map):
        '''Find this variable in the arg map and return the value.'''
        svar = arg_map.get(self)
        if svar:
            return svar

        raise ValueError('Variable %s is not found in %s' % (self, arg_map))
