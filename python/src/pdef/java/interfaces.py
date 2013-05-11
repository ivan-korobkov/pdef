# encoding: utf-8
import os.path
from jinja2 import Environment
from pdef.java.refs import JavaRef


IFACE_FILE = os.path.join(os.path.dirname(__file__), '../java2/interface.template')
with open(IFACE_FILE, 'r') as f:
    IFACE_TEMPLATE = f.read()

ENV = Environment(trim_blocks=True)
IFACE = ENV.from_string(IFACE_TEMPLATE)


class JavaInterface(object):
    def __init__(self, iface):
        self.name = iface.name
        self.package = iface.parent.fullname

        self.bases = [JavaRef.from_lang(base) for base in iface.bases]
        self.declared_methods = [JavaMethod(method) for method in iface.declared_methods]

    @property
    def code(self):
        return IFACE.render(**self.__dict__)


class JavaMethod(object):
    def __init__(self, method):
        self.name = method.name
        self.args = [JavaMethodArg(arg) for arg in method.args]

        self.result = JavaRef.from_lang(method.result)
        if not self.result.interface:
            self.result = JavaRef('ListenableFuture<%s>' % self.result.boxed)


class JavaMethodArg(object):
    def __init__(self, arg):
        self.name = arg.name
        self.type = JavaRef.from_lang(arg.type)
