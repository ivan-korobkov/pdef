# encoding: utf-8
from jinja2 import Environment
import os.path
from pdef import lang

ENUM_FILE = os.path.join(os.path.dirname(__file__), "enum.template")
with open(ENUM_FILE, "r") as f:
    ENUM_TEMPLATE = f.read()


class JavaRef(object):
    @classmethod
    def from_lang(cls, ref):
        name = ref.name
        package_name = ref.parent.fullname if ref.parent else None
        variables = [JavaRef.from_lang(var) for var in ref.variables]
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


class JavaPackageRef(object):
    @classmethod
    def from_lang(cls, package):
        return JavaPackageRef(package.name)

    def __init__(self, name):
        self.name = name

    def __str__(self):
        return self.name


class JavaEnum(object):
    def __init__(self, enum):
        self.name = enum.name
        self.package = JavaPackageRef.from_lang(enum.parent)
        self.values = [val.name for val in enum.values]

    @property
    def code(self):
        env = Environment(trim_blocks=True)
        template = env.from_string(ENUM_TEMPLATE)
        return template.render(**self.__dict__)


def upper_first(s):
    if not s:
        return s
    return s[0].upper() + s[1:]


def join_str(delimiter, iterable):
    return delimiter.join(str(e) for e in iterable)
