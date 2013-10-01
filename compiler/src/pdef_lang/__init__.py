# encoding: utf-8
from .collections import List, Set, Map
from .definitions import Type, Definition, NativeType, NativeTypes
from .enums import Enum, EnumValue
from .interfaces import Interface, Method, MethodArg
from .messages import Message, Field
from .modules import Module, ImportedModule, AbstractImport, AbsoluteImport, RelativeImport
from .packages import Package


class Location(object):
    def __init__(self, path, line=0):
        self.path = path
        self.line = line

    def __str__(self):
        s = self.path if self.path else 'nofile'
        return '%s, line %s' % (s, self.line) if self.line else s
