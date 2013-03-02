# encoding: utf-8
from pdef import lang


class JavaRef(object):
    @classmethod
    def from_ref(cls, ref):
        name = ref.name
        package_name = ref.parent.fullname
        variables = [JavaRef.from_ref(var) for var in ref.variables]
        is_variable = isinstance(ref, lang.Variable)
        return JavaRef(name, package_name, variables, is_variable)

    def __init__(self, name, package, variables, is_variable=False):
        self.name = name
        self.package = package
        self.generic = bool(variables)
        self.is_variable = is_variable
        self.variables = variables

    def __str__(self):
        if self.is_variable:
            return self.name

        s = ''
        if self.package:
            s += self.package + '.'

        s += self.name
        if self.variables:
            s += '<%s>' % join_str(', ', self.variables)

        return s

    @property
    def local(self):
        return JavaRef(self.name, None, self.variables, self.is_variable)

    @property
    def raw(self):
        return JavaRef(self.name, self.package, (), self.is_variable)

    @property
    def wildcard(self):
        if self.is_variable:
            return JavaRef('?', None, (), True)

        if not self.variables:
            return self

        vars = [var.wildcard for var in self.variables]
        return JavaRef(self.name, self.package, vars, self.is_variable)


def upper_first(s):
    if not s:
        return s
    return s[0].upper() + s[1:]


def join_str(delimiter, iterable):
    return delimiter.join(str(e) for e in iterable)
