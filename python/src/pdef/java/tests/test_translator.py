# encoding: utf-8
import unittest
from pdef.lang import *
from pdef.java import *


class TestJavaRef(unittest.TestCase):
    def setUp(self):
        self.msg = Message('Message', variables=[lang.Variable('T')])
        module = Module('module')
        module.add_definitions(self.msg)

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
        self.enum = Enum('ObjectType',values=[lang.EnumValue('OBJECT'), lang.EnumValue('USER')])
        module = Module('module')
        module.add_definitions(self.enum)

    def test(self):
        enum = JavaEnum(self.enum)
        assert str(enum.name) == 'ObjectType'
        assert str(enum.package) == 'module'

    def test_code(self):
        enum = JavaEnum(self.enum)
        assert enum.code


class TestJavaMessage(unittest.TestCase):
    def setUp(self):
        int32 = Native('int', options=NativeOptions(
            java='int',
            java_boxed='Integer',
            java_descriptor='pdef.provided.NativeValueDescriptors.getInt32()',
            java_default='0'
        ))
        string = Native('String', options=NativeOptions(
            java='String',
            java_boxed='String',
            java_descriptor='pdef.provided.NativeValueDescriptors.getString()',
            java_default='null'
        ))

        self.msg = Message('Message', declared_fields=[
            Field('field1', int32),
            Field('field2', string)
        ])
        module = Module('module')
        module.add_definitions(self.msg)

    def test_code(self):
        jmsg = JavaMessage(self.msg)
        print jmsg.code
        assert jmsg.code
