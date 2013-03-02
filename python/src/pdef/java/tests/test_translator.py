# encoding: utf-8
import unittest
from pdef import lang
from pdef.java import *


class TestJavaRef(unittest.TestCase):
    def setUp(self):
        module = lang.Module('module')
        self.msg = lang.Message('Message', variables=[lang.Variable('T')])
        self.msg.parent = module

    def test(self):
        ref = JavaRef.from_lang(self.msg)
        assert str(ref) == 'module.Message<T>'

    def test_local(self):
        ref = JavaRef.from_lang(self.msg).local
        assert str(ref) == 'Message<T>'

    def test_raw(self):
        ref = JavaRef.from_lang(self.msg).raw
        assert str(ref) == 'module.Message'

    def test_wildcards(self):
        ref = JavaRef.from_lang(self.msg).wildcard
        assert str(ref) == 'module.Message<?>'

    def test_local_wildcars(self):
        ref = JavaRef.from_lang(self.msg).local.wildcard
        assert str(ref) == 'Message<?>'


class TestJavaEnum(unittest.TestCase):
    def setUp(self):
        module = lang.Module('module')
        self.enum = lang.Enum('ObjectType',
                              values=[lang.EnumValue('OBJECT'), lang.EnumValue('USER')])
        self.enum.parent = module

    def test(self):
        enum = JavaEnum(self.enum)
        assert str(enum.name) == 'ObjectType'
        assert str(enum.package) == 'module'

    def test_code(self):
        enum = JavaEnum(self.enum)
        assert enum.code
