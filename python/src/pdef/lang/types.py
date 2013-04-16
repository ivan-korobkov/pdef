# encoding: utf-8
from pdef.lang.symbols import Node


class Type(Node):
    generic = False

    def __init__(self, name, module=None):
        super(Type, self).__init__(name)
        self.module = module
        self.rawtype = self
        self.inited = False

    @property
    def parent(self):
        return self.module

    @property
    def fullname(self):
        return '%s.%s' % (self.parent.fullname, self.simplename) if self.parent else self.simplename

    @property
    def simplename(self):
        return self.name

    def init(self):
        if self.inited:
            return

        self.inited = True
        self._init()

    def _init(self):
        pass

    def parameterize(self, *variables):
        return self


class Variable(Type):
    def __init__(self, name):
        super(Variable, self).__init__(name)
