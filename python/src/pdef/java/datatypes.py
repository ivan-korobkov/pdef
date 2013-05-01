# encoding: utf-8
import os.path
from jinja2 import Environment
from pdef.java.refs import JavaRef
from pdef.utils import upper_first


ENUM_FILE = os.path.join(os.path.dirname(__file__), 'enum.template')
MESSAGE_FILE = os.path.join(os.path.dirname(__file__), 'message.template')
with open(ENUM_FILE, 'r') as f:
    ENUM_TEMPLATE = f.read()
with open(MESSAGE_FILE, 'r') as f:
    MESSAGE_TEMPLATE = f.read()

ENV = Environment(trim_blocks=True)
ENUM = ENV.from_string(ENUM_TEMPLATE)
MESSAGE = ENV.from_string(MESSAGE_TEMPLATE)


class JavaMessage(object):
    def __init__(self, msg):
        self.name = msg.name
        self.package = msg.parent.fullname

        if msg.base:
            base = msg.base
            self.base = JavaRef.from_lang(base)
            self.base_class = self.base
            self.base_builder = JavaRef(str(self.base) + '.Builder')
        else:
            self.base = None
            self.base_class = None
            self.base_builder = None

        self.subtypes = JavaSubtypes(msg.subtypes) if msg.subtypes else None

        fields = []
        dfields = []
        for field in msg.fields:
            jfield = JavaField(field)
            fields.append(jfield)
            if (field.is_declared):
                dfields.append(jfield)
        self.fields = tuple(fields)
        self.declared_fields = tuple(dfields)
        self.is_exception = msg.is_exception

    @property
    def code(self):
        return MESSAGE.render(**self.__dict__)


class JavaField(object):
    def __init__(self, field):
        self.name = field.name
        self.type = JavaRef.from_lang(field.type)

        self.is_type = field.is_type
        self.is_subtype = field.is_subtype
        self.type_value = JavaRef.from_lang(field.type_value) if field.type_value else None
        self.is_overriden = field.is_overriden
        self.is_declared = field.is_declared

        self.get = 'get%s' % upper_first(self.name)
        self.set = 'set%s' % upper_first(self.name)
        self.clear = 'clear%s' % upper_first(self.name)
        self.present = 'has%s' % upper_first(self.name)


class JavaSubtypes(object):
    def __init__(self, subtypes):
        self.type = JavaRef.from_lang(subtypes.type)
        self.items = tuple((k.name.lower(), JavaRef.from_lang(v))
            for k, v in subtypes.as_map().items())
        self.field = JavaField(subtypes.field)
        self.is_root = subtypes.is_root


class JavaEnum(object):
    def __init__(self, enum):
        self.name = enum.name
        self.package = enum.parent.fullname
        self.values = [val.name for val in enum.values]

    @property
    def code(self):
        return ENUM.render(**self.__dict__)
