# encoding: utf-8
import unittest
from pdef_compiler.lang import *
from pdef_compiler.java.translator import *


class TestJavaEnum(unittest.TestCase):
    def test(self):
        enum = Enum('Number')
        enum.add_value('ONE')
        enum.add_value('TWO')
        module = Module('test.module')
        module.add_definition(enum)

        translator = JavaTranslator('/tmp')
        jenum = JavaEnum(enum, translator)
        assert jenum.code


class TestInterface(unittest.TestCase):
    def setUp(self):
        base0 = Interface('Base0')
        base1 = Interface('Base1')

        iface = Interface('Interface')
        iface.set_base(base0)
        iface.set_base(base1)
        iface.create_method('ping')
        iface.create_method('pong')
        iface.create_method('echo', NativeTypes.STRING, ('text', NativeTypes.STRING))
        iface.create_method('sum', NativeTypes.INT32, ('z', NativeTypes.INT32),
                            ('a', NativeTypes.INT32))
        iface.create_method('abc', NativeTypes.STRING, ('a', NativeTypes.STRING),
                            ('b', NativeTypes.STRING), ('c', NativeTypes.STRING))

        iface.create_method('base0', base0)
        iface.create_method('base1', base1)

        module0 = Module('test.module0')
        module0.add_definition(base0)
        module0.add_definition(base1)

        module1 = Module('test.module1')
        module1.add_definition(iface)

        translator = JavaTranslator('/tmp')
        self.jiface = JavaInterface(iface, translator)

    def test(self):
        assert self.jiface.code


class TestMessage(unittest.TestCase):
    def test(self):
        enum = Enum('Type')
        enum.add_value('MSG')

        base = Message('Base')
        base.create_field('type', enum, is_discriminator=True)
        base.create_field('field0', NativeTypes.BOOL)
        base.create_field('field1', NativeTypes.INT16)
        msg0 = Message('Message0')

        msg = Message('Message')
        msg.set_base(base, enum.find_value('MSG'))
        msg.create_field('field2', NativeTypes.INT32)
        msg.create_field('field3', NativeTypes.STRING)
        msg.create_field('field4', msg0)
        msg.create_field('field5', base)
        msg.create_field('field6', List(msg))

        module0 = Module('test.module0')
        module0.add_definitions(enum, base, msg0)

        module1 = Module('test.module1')
        module1.add_definition(msg)

        translator = JavaTranslator('/tmp')
        jmsg = JavaMessage(msg, translator)
        jbase = JavaMessage(base, translator)
        assert jbase.code
        assert jmsg.code


class TestRef(unittest.TestCase):
    def setUp(self):
        self.translator = JavaTranslator('/tmp')

    def test_list(self):
        obj = List(NativeTypes.INT32)
        jobj = self.translator.ref(obj)
        assert str(jobj) == 'java.util.List<Integer>'
        assert jobj.is_list

    def test_set(self):
        obj = Set(NativeTypes.BOOL)
        jobj = self.translator.ref(obj)
        assert str(jobj) == 'java.util.Set<Boolean>'
        assert jobj.is_set

    def test_map(self):
        obj = Map(NativeTypes.STRING, NativeTypes.FLOAT)
        jobj = self.translator.ref(obj)
        assert str(jobj) == 'java.util.Map<String, Float>'
        assert jobj.is_map

    def test_native(self):
        jobj = self.translator.ref(NativeTypes.INT64)
        assert jobj.name == 'Long'
        assert jobj.unboxed == 'long'
        assert jobj.default == '0L'
        assert jobj.is_primitive

    def test_enum_value(self):
        enum = Enum('Number')
        one = enum.add_value('ONE')

        module = Module('test.module')
        module.add_definition(enum)

        jone = self.translator.ref(one)
        assert str(jone) == 'test.module.Number.ONE'

    def test_interface(self):
        iface = Interface('Interface')
        module = Module('test.module')
        module.add_definition(iface)

        jface = self.translator.ref(iface)
        assert str(jface) == 'test.module.Interface'
        assert jface.is_interface

    def test_message(self):
        msg = Message('Message')
        module = Module('test.module')
        module.add_definition(msg)

        jmsg = self.translator.ref(msg)
        assert str(jmsg) == 'test.module.Message'
        assert jmsg.default == 'test.module.Message.instance()'
