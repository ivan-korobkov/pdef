# encoding: utf-8
import unittest
from pdef.lang import *
from pdef.java import *


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
        print enum.code
        assert enum.code


class TestJavaMessage(unittest.TestCase):
    def setUp(self):
        int32 = Native('int32', options=NativeOptions(
            java_type='int',
            java_boxed='Integer',
            java_descriptor='pdef.provided.NativeValueDescriptors.getInt32()',
            java_default='0'
        ))
        string = Native('String', options=NativeOptions(
            java_type='String',
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
