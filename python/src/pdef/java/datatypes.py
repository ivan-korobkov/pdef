# encoding: utf-8
import os.path
from jinja2 import Environment
from pdef.java.refs import JavaRef, SimpleJavaRef


ENUM_FILE = os.path.join(os.path.dirname(__file__), "enum.template")
with open(ENUM_FILE, "r") as f:
    ENUM_TEMPLATE = f.read()


MESSAGE_FILE = os.path.join(os.path.dirname(__file__), "message.template")
with open(MESSAGE_FILE, "r") as f:
    MESSAGE_TEMPLATE = f.read()


class JavaMessage(object):
    def __init__(self, msg):
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

        index = len(msg.base.fields) if msg.base else 0
        dfields = []
        for field in msg.declared_fields:
            dfields.append(JavaField(field, index))
            index += 1
        self.declared_fields = tuple(dfields)

        self.base_tree = JavaMessageTree(msg.base_tree) if msg.base_tree else None
        self.root_tree = JavaMessageTree(msg.root_tree) if msg.root_tree else None

        self.is_generic = bool(self.variables)
        self.getInstance = 'getInstanceOf%s()' % self.name if self.is_generic else 'getInstance()'
        self.getInstanceVars = '<%s>' % ', '.join(str(var) for var in self.variables) \
                if self.is_generic else''
        self.createBuilder = 'builderOf%s()' % self.name if self.is_generic else 'builder()'

    @property
    def code(self):
        env = Environment(trim_blocks=True)
        template = env.from_string(MESSAGE_TEMPLATE)
        return template.render(**self.__dict__)


class JavaField(object):
    def __init__(self, field, index=-1):
        self.index = index
        self.name = field.name
        self.type = JavaRef.from_lang(field.type)
        self.is_set = 'has%s' % upper_first(self.name)
        self.get = 'get%s' % upper_first(self.name)
        self.set = 'set%s' % upper_first(self.name)
        self.do_set = 'doSet%s' % upper_first(self.name)
        self.clear = 'clear%s' % upper_first(self.name)
        self.is_type_field = field.is_type_field
        self.type_value = JavaRef.from_lang(field.type_value) if field.type_value else None


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
        env = Environment(trim_blocks=True)
        template = env.from_string(ENUM_TEMPLATE)
        return template.render(**self.__dict__)


def upper_first(s):
    if not s:
        return s
    return s[0].upper() + s[1:]
