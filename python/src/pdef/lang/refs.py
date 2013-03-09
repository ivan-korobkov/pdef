# encoding: utf-8
from pdef.preconditions import *
from pdef.lang import errors
from pdef.lang.nodes import Symbol


class AbstractRef(Symbol):
    def __init__(self, name):
        super(AbstractRef, self).__init__(name)
        self.delegate = None

    @property
    def fullname(self):
        if self.delegate:
            return self.delegate.fullname
        return super(AbstractRef, self).fullname

    def __getattr__(self, item):
        self._check_delegate()
        return getattr(self.delegate, item)

    def __hash__(self):
        self._check_delegate()
        return hash(self.delegate)

    def __eq__(self, other):
        self._check_delegate()
        return self.delegate == other

    def _check_delegate(self):
        check_state(self.delegate is not None, 'Delegate is not set in %s', self)


class ImportRef(AbstractRef):
    def __init__(self, import_name, alias=None):
        super(ImportRef, self).__init__(alias if alias else import_name)
        self.import_name = import_name

    def link(self):
        package = self.package
        if not self.import_name in package.modules:
            errors.add(self, 'import not found "%s"', self.import_name)
            return

        self.delegate = package._lookup_child(self.import_name)
        self._add_child(self.delegate, always_parent=False)

    def lookup(self, name):
        self._check_delegate()
        return self.delegate.lookup(name)

    def _lookup_child(self, name):
        self._check_delegate()
        return self.delegate._lookup_child(name)


class Ref(AbstractRef):
    def __init__(self, name, *generic_variables):
        super(Ref, self).__init__(name)
        self.variables = []
        self.add_variables(*generic_variables)

    @property
    def fullname(self):
        if self.parent:
            return '%s in %s' % (self.name, self.parent.fullname)
        return self.name

    def add_variables(self, *variables):
        for arg in variables:
            self.variables.append(arg)
            self._add_child(arg)

    def link(self):
        for arg in self.variables:
            arg.link()

        self.delegate = self._lookup_delegate()
        self._add_child(self.delegate, always_parent=False)

    def dereference(self):
        self._check_delegate()
        return self.delegate

    def _lookup_delegate(self):
        rawtype = self.lookup(self.name)
        if not rawtype:
            errors.add(self, 'type not found "%s"', self.name)
            return

        if not rawtype.generic:
            return rawtype

        ptype = self.package.parameterized_symbol(rawtype, *self.variables)
        ptype.parent = self.parent
        return ptype
