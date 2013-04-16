# encoding: utf-8
from pdef import lang


class JavaRef(object):
    @classmethod
    def from_lang(cls, ref):
        if isinstance(ref, lang.Native):
            return NativeJavaRef(ref)
        elif isinstance(ref, lang.ParameterizedNative):
            return ParameterizedJavaRef(ref)
        elif isinstance(ref, lang.Enum):
            return JavaRef.from_enum(ref)
        elif isinstance(ref, lang.Interface):
            return JavaRef.from_interface(ref)
        else:
            return JavaRef.from_message(ref)

    @classmethod
    def from_enum(cls, enum):
        name = enum.parent.fullname + '.' + enum.name if enum.parent else enum.name
        default = JavaRef.from_lang(enum.values.items[0])
        return JavaRef(name, default=default)

    @classmethod
    def from_message(cls, message):
        name = message.parent.fullname + '.' + message.name if message.parent else message.name
        default = '%s.getInstance()' % message.name
        return JavaRef(name, default=default)

    @classmethod
    def from_interface(cls, iface):
        name = iface.parent.fullname + '.' + iface.name if iface.parent else iface.name
        jref = JavaRef(name)
        jref.interface = True
        return jref

    def __init__(self, name, default='null', boxed=None, primitive=False):
        self.name = name
        self.default = default
        self.boxed = boxed if boxed else self
        self.primitive = primitive
        self.interface = False

    def __str__(self):
        return self.name

    @property
    def nullable(self):
        return self.default == 'null'


class NativeJavaRef(JavaRef):
    def __init__(self, native):
        super(NativeJavaRef, self).__init__(name=native.options.java_type)
        self.variables = tuple(JavaRef.from_lang(var) for var in native.variables)

        self.default = native.options.java_default
        self.boxed = JavaRef(native.options.java_boxed)
        self.primitive = native.options.java_is_primitive


class ParameterizedJavaRef(JavaRef):
    def __init__(self, ptype):
        super(ParameterizedJavaRef, self).__init__(ptype.rawtype)
        self.rawtype = JavaRef.from_lang(ptype.rawtype)
        self.name = self.rawtype.name

        self.variables = tuple(JavaRef.from_lang(var) for var in ptype.variables)
        self.default = '%s' % self.rawtype.default

    def __str__(self):
        s = super(ParameterizedJavaRef, self).__str__()
        if self.variables:
            s += '<%s>' % ', '.join(str(var.boxed) for var in self.variables)
        return s


def indent(s, spaces=4):
    return '\n'.join((spaces * ' ') + line for line in s.splitlines())
