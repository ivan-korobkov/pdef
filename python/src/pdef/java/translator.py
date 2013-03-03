# encoding: utf-8
from jinja2 import Environment
import os.path
from pdef import lang

ENUM_FILE = os.path.join(os.path.dirname(__file__), "enum.template")
with open(ENUM_FILE, "r") as f:
    ENUM_TEMPLATE = f.read()


MESSAGE_FILE = os.path.join(os.path.dirname(__file__), "message.template")
with open(MESSAGE_FILE, "r") as f:
    MESSAGE_TEMPLATE = f.read()


class JavaRef(object):
    @classmethod
    def from_lang(cls, ref):
        if isinstance(ref, lang.Native):
            return NativeJavaRef(ref)
        elif isinstance(ref, lang.Variable):
            return VariableJavaRef(ref)
        else:
            return TypeJavaRef(ref)

    def __init__(self, name):
        self.name = name
        self.package = None
        self.variables = ()

        self.descriptor = None
        self.boxed = self
        self.default = 'null'

    def __str__(self):
        s = ''
        if self.package:
            s += self.package + '.'

        s += self.name
        if self.variables:
            s += '<%s>' % join_str(', ', self.variables)

        return s

    @property
    def generic(self):
        return bool(self.variables)

    @property
    def local(self):
        return SimpleJavaRef(self.name, None, self.variables)

    @property
    def raw(self):
        return SimpleJavaRef(self.name, self.package)

    @property
    def wildcard(self):
        if not self.variables:
            return self

        vars = [var.wildcard for var in self.variables]
        return SimpleJavaRef(self.name, self.package, vars)


class NativeJavaRef(JavaRef):
    def __init__(self, ref):
        super(NativeJavaRef, self).__init__(name=ref.options.java)
        self.variables = ref.variables

        self.descriptor = ref.options.java_descriptor
        self.default = ref.options.java_default
        self.boxed = SimpleJavaRef(ref.options.java_boxed)


class VariableJavaRef(JavaRef):
    def __init__(self, var):
        super(VariableJavaRef, self).__init__(var.name)
        self.boxed = self.name
        self.descriptor = 'var%s' % self.name


class TypeJavaRef(JavaRef):
    def __init__(self, ref):
        super(TypeJavaRef, self).__init__(ref.name)
        self.package = ref.parent.fullname if ref.parent else None
        self.variables = [JavaRef.from_lang(var) for var in ref.variables]
        self.descriptor = '%s.%s.Descriptor.getInstance()' % (ref.parent, self.name)


class SimpleJavaRef(JavaRef):
    def __init__(self, name, package=None, variables=()):
        super(SimpleJavaRef, self).__init__(name)
        self.package = package
        self.variables = variables


class JavaEnum(object):
    def __init__(self, enum):
        self.name = enum.name
        self.package = enum.parent.fullname
        self.values = [val.name for val in enum.values]

    @property
    def code(self):
        env = Environment()
        template = env.from_string(ENUM_TEMPLATE)
        return template.render(**self.__dict__)


class JavaMessage(object):
    def __init__(self, msg):
        self.name = msg.name
        self.package = msg.parent.fullname
        self.declared_fields = [JavaField(field) for field in msg.declared_fields]

    @property
    def code(self):
        env = Environment(trim_blocks=True)
        template = env.from_string(MESSAGE_TEMPLATE)
        return template.render(**self.__dict__)


class JavaField(object):
    def __init__(self, field):
        self.name = field.name
        self.type = JavaRef.from_lang(field.type)
        self.getter = 'get%s' % upper_first(self.name)
        self.setter = 'set%s' % upper_first(self.name)
        self.clearer = 'clear%s' % upper_first(self.name)


def upper_first(s):
    if not s:
        return s
    return s[0].upper() + s[1:]


def join_str(delimiter, iterable):
    return delimiter.join(str(e) for e in iterable)
