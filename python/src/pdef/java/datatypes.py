# encoding: utf-8
import os.path
from jinja2 import Environment
from pdef.java.refs import JavaRef, SimpleJavaRef


ENUM_FILE = os.path.join(os.path.dirname(__file__), "enum.template")
MESSAGE_FILE = os.path.join(os.path.dirname(__file__), "message.template")
with open(ENUM_FILE, "r") as f:
    ENUM_TEMPLATE = f.read()
with open(MESSAGE_FILE, "r") as f:
    MESSAGE_TEMPLATE = f.read()

ENV = Environment(trim_blocks=True)
ENUM = ENV.from_string(ENUM_TEMPLATE)
MESSAGE = ENV.from_string(MESSAGE_TEMPLATE)


class JavaMessage(object):
    def __init__(self, msg,):
        self.name = msg.name
        self.type = JavaRef.from_lang(msg).local
        self.package = msg.parent.fullname
        self.builder = SimpleJavaRef('Builder', variables=self.type.variables)

        if msg.base:
            base = msg.base
            self.base = JavaRef.from_lang(base)
            self.base_class = self.base
            self.base_builder = SimpleJavaRef('Builder', str(self.base.raw),
                    variables=tuple(JavaRef.from_lang(var) for var in base.variables))
        else:
            self.base = None
            self.base_class = SimpleJavaRef('pdef.generated.GeneratedMessage')
            self.base_builder = SimpleJavaRef('pdef.generated.GeneratedMessage.Builder')

        self.variables = tuple(JavaRef.from_lang(var) for var in msg.variables)

        index = 0
        fields = []
        dfields = []
        for field in msg.fields:
            jfield = JavaField(field, index)
            fields.append(jfield)
            if (field.is_declared):
                dfields.append(jfield)
            index += 1
        self.fields = tuple(fields)
        self.declared_fields = tuple(dfields)

        self.base_tree = JavaMessageTree(msg.base_tree) if msg.base_tree else None
        self.root_tree = JavaMessageTree(msg.root_tree) if msg.root_tree else None

        self.is_generic = bool(self.variables)
        self.get_instance = 'getInstanceOf%s()' % self.name if self.is_generic else 'getInstance()'
        self.get_instance_vars = '<%s>' % ', '.join(str(var) for var in self.variables) \
                if self.is_generic else''
        self.create_builder = 'builderOf%s()' % self.name if self.is_generic else 'builder()'
        self.has_value_of = not self.is_generic

    @property
    def code(self):
        return MESSAGE.render(**self.__dict__)


class JavaField(object):
    def __init__(self, field, index=-1):
        self.index = index
        self.name = field.name
        self.type = JavaRef.from_lang(field.type)

        self.is_type = field.is_type
        self.is_subtype = field.is_subtype
        self.type_value = JavaRef.from_lang(field.type_value) if field.type_value else None
        self.is_overriden = field.is_overriden
        self.is_declared = field.is_declared

        self.is_set = 'has%s' % upper_first(self.name)
        self.get = 'get%s' % upper_first(self.name)
        self.set = 'set%s' % upper_first(self.name)
        self.do_set = 'doSet%s' % upper_first(self.name)
        self.clear = 'clear%s' % upper_first(self.name)


class JavaMessageTree(object):
    def __init__(self, tree):
        self.type = JavaRef.from_lang(tree.type)
        self.subtypes = tuple((JavaRef.from_lang(k), JavaRef.from_lang(v))
            for k, v in tree.as_map().items())
        self.field = JavaField(tree.field)


class JavaEnum(object):
    def __init__(self, enum):
        self.name = enum.name
        self.package = enum.parent.fullname
        self.values = [val.name for val in enum.values]

    @property
    def code(self):
        return ENUM.render(**self.__dict__)


def upper_first(s):
    if not s:
        return s
    return s[0].upper() + s[1:]
