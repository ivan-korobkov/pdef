# encoding: utf-8
from pdef import lang


class JavaRef(object):
    @classmethod
    def from_lang(cls, ref):
        if isinstance(ref, lang.Native):
            return NativeJavaRef(ref)
        elif isinstance(ref, lang.Variable):
            return VariableJavaRef(ref)
        elif isinstance(ref, lang.ParameterizedType):
            return ParameterizedJavaRef(ref)
        elif isinstance(ref, lang.Enum):
            return EnumJavaRef(ref)
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
            s += '<%s>' % ', '.join(str(e) for e in self.variables)

        return s

    @property
    def generic(self):
        return bool(self.variables)

    @property
    def nullable(self):
        return self.default == 'null'

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

        vars = tuple(var.wildcard for var in self.variables)
        return SimpleJavaRef(self.name, self.package, vars)


class VariableJavaRef(JavaRef):
    def __init__(self, var):
        super(VariableJavaRef, self).__init__(var.name)
        self.boxed = SimpleJavaRef('Object')
        self.descriptor = 'variable%s' % self.name

    @property
    def wildcard(self):
        return '?'


class TypeJavaRef(JavaRef):
    def __init__(self, ref):
        super(TypeJavaRef, self).__init__(ref.name)
        self.package = ref.parent.fullname if ref.parent else None
        self.variables = tuple(JavaRef.from_lang(var) for var in ref.variables)

        if ref.parent:
            self.descriptor = '%s.%s.getClassDescriptor()' % (ref.parent.fullname, self.name)
        else:
            self.descriptor = '%s.getClassDescriptor()' % self.name


class EnumJavaRef(TypeJavaRef):
    def __init__(self, enum):
        super(EnumJavaRef, self).__init__(enum)
        self.default = JavaRef.from_lang(enum.values.items[0])


class NativeJavaRef(JavaRef):
    def __init__(self, native):
        super(NativeJavaRef, self).__init__(name=native.options.java_type)
        self.variables = tuple(JavaRef.from_lang(var) for var in native.variables)

        self.descriptor = native.options.java_descriptor
        self.default = native.options.java_default
        self.boxed = SimpleJavaRef(native.options.java_boxed, variables=self.variables)


class SimpleJavaRef(JavaRef):
    def __init__(self, name, package=None, variables=()):
        super(SimpleJavaRef, self).__init__(name)
        self.package = package
        self.variables = variables


class ParameterizedJavaRef(JavaRef):
    def __init__(self, ptype):
        super(ParameterizedJavaRef, self).__init__(ptype.rawtype)
        self.rawtype = JavaRef.from_lang(ptype.rawtype)
        self.name = self.rawtype.name
        self.package = self.rawtype.package

        self.variables = tuple(JavaRef.from_lang(var) for var in ptype.variables)
        self.descriptor = self._create_descriptor()

    def _create_descriptor(self):
        descriptor = self.rawtype.descriptor
        vars = ',\n'.join(indent(var.descriptor) for var in self.variables)
        return '%s.parameterize(\n%s)' % (descriptor, vars)


def indent(s, spaces=4):
    return '\n'.join((spaces * ' ') + line for line in s.splitlines())
