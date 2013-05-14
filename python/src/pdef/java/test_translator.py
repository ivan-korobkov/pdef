# encoding: utf-8
import unittest
from pdef.lang import *
from pdef.java.translator import *


class TestJavaEnum(unittest.TestCase):
    def test(self):
        enum = Enum('Number', 'ONE', 'TWO')
        module = Module('test.module')
        module.add_definition(enum)

        translator = JavaTranslator('/dev/null')
        jenum = JavaEnum(enum, translator.enum_template)
        assert jenum.code


class TestInterface(unittest.TestCase):
    def setUp(self):
        base0 = Interface('Base0')
        base1 = Interface('Base1')

        iface = Interface('Interface')
        iface.add_base(base0)
        iface.add_base(base1)
        iface.add_method('ping')
        iface.add_method('pong')
        iface.add_method('echo', Values.STRING, ('text', Values.STRING))
        iface.add_method('sum', Values.INT32, ('z', Values.INT32), ('a', Values.INT32))
        iface.add_method('abc', Values.STRING, ('a', Values.STRING), ('b', Values.STRING),
                         ('c', Values.STRING))

        iface.add_method('base0', base0)
        iface.add_method('base1', base1)

        module0 = Module('test.module0')
        module0.add_definitions(base0, base1)

        module1 = Module('test.module1')
        module1.add_definition(iface)

        translator = JavaTranslator('/dev/null')
        self.jiface = JavaInterface(iface, translator.interface_template)

    def test(self):
        assert self.jiface.code

    def test_async(self):
        assert self.jiface.async_code


class TestMessage(unittest.TestCase):
    def test(self):
        enum = Enum('Type', 'MSG')
        base = Message('Base')
        base.add_field('type', enum, is_discriminator=True)
        base.add_field('field0', Values.BOOL)
        base.add_field('field1', Values.INT16)
        msg0 = Message('Message0')

        msg = Message('Message')
        msg.set_base(base, enum.values['MSG'])
        msg.add_field('field2', Values.INT32)
        msg.add_field('field3', Values.STRING)
        msg.add_field('field4', msg0)
        msg.add_field('field5', base)
        msg.add_field('field6', List(msg))

        module0 = Module('test.module0')
        module0.add_definitions(enum, base, msg0)

        module1 = Module('test.module1')
        module1.add_definition(msg)

        translator = JavaTranslator('/dev/null')
        jmsg = JavaMessage(msg, translator.message_template)
        jbase = JavaMessage(base, translator.message_template)
        assert jbase.code
        assert jmsg.code


class TestRef(unittest.TestCase):
    def test_list(self):
        obj = List(Values.INT32)
        jobj = ref(obj)
        assert str(jobj) == 'java.lang.List<Integer>'
        assert jobj.is_list

    def test_set(self):
        obj = Set(Values.BOOL)
        jobj = ref(obj)
        assert str(jobj) == 'java.lang.Set<Boolean>'
        assert jobj.is_set

    def test_map(self):
        obj = Map(Values.STRING, Values.FLOAT)
        jobj = ref(obj)
        assert str(jobj) == 'java.lang.Map<String, Float>'
        assert jobj.is_map

    def test_native(self):
        jobj = ref(Values.INT64)
        assert jobj.name == 'long'
        assert jobj.boxed == 'Long'
        assert jobj.default == '0L'
        assert jobj.is_primitive
        assert not jobj.is_nullable

    def test_enum_value(self):
        enum = Enum('Number')
        one = enum.add_value('ONE')

        module = Module('test.module')
        module.add_definition(enum)

        jone = ref(one)
        assert str(jone) == 'test.module.Number.ONE'

    def test_interface(self):
        iface = Interface('Interface')
        module = Module('test.module')
        module.add_definition(iface)

        jface = ref(iface)
        assert str(jface) == 'test.module.Interface'
        assert jface.async_name == 'test.module.AsyncInterface'
        assert jface.is_interface

    def test_message(self):
        msg = Message('Message')
        module = Module('test.module')
        module.add_definition(msg)

        jmsg = ref(msg)
        assert str(jmsg) == 'test.module.Message'
        assert jmsg.default == 'test.module.Message.getInstance()'
