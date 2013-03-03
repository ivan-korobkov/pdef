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

        self.variables = tuple(JavaRef.from_lang(var) for var in msg.variables)
        self.declared_fields = tuple(JavaField(field) for field in msg.declared_fields)
        self.builder = SimpleJavaRef('Builder', variables=self.type.variables)

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
