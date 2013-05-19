# encoding: utf-8
import unittest
from pdef.lang import *
from pdef.csharp.translator import *


class TestCsharpEnum(unittest.TestCase):
    def test(self):
        enum = Enum('Number', 'ONE', 'TWO')
        module = Module('test.module')
        module.add_definition(enum)

        translator = CsharpTranslator('/tmp')
        csenum = CsharpEnum(enum, translator.enum_template)
        assert csenum.code


class TestInterface(unittest.TestCase):
    def test(self):
        base0 = Interface('Base0')
        base1 = Interface('Base1')

        iface = Interface('Interface')
        iface.add_base(base0)
        iface.add_base(base1)
        iface.add_method('ping')
        iface.add_method('pong')
        iface.add_method('echo', Types.STRING, ('text', Types.STRING))
        iface.add_method('sum', Types.INT32, ('z', Types.INT32), ('a', Types.INT32))
        iface.add_method('abc', Types.STRING, ('a', Types.STRING), ('b', Types.STRING),
                         ('c', Types.STRING))

        iface.add_method('base0', base0)
        iface.add_method('base1', base1)

        module0 = Module('test.module0')
        module0.add_definitions(base0, base1)

        module1 = Module('test.module1')
        module1.add_definition(iface)

        translator = CsharpTranslator('/tmp')
        csface = CsharpInterface(iface, translator.interface_template)

        assert csface.code


class TestMessage(unittest.TestCase):
    def test(self):
        enum = Enum('Type', 'MSG')
        base = Message('Base')
        base.add_field('type', enum, is_discriminator=True)
        base.add_field('field0', Types.BOOL)
        base.add_field('field1', Types.INT16)
        msg0 = Message('Message0')

        msg = Message('Message')
        msg.set_base(base, enum.values['MSG'])
        msg.add_field('field2', Types.INT32)
        msg.add_field('field3', Types.STRING)
        msg.add_field('field4', msg0)
        msg.add_field('field5', base)
        msg.add_field('field6', List(msg))

        module0 = Module('test.module0')
        module0.add_definitions(enum, base, msg0)

        module1 = Module('test.module1')
        module1.add_definition(msg)

        translator = CsharpTranslator('/tmp')
        jmsg = CsharpMessage(msg, translator.message_template)
        jbase = CsharpMessage(base, translator.message_template)
        assert jbase.code
        assert jmsg.code


class TestRef(unittest.TestCase):
    def test_list(self):
        obj = List(Types.INT32)
        cs = ref(obj)
        assert str(cs) == 'IList<int>'

    def test_set(self):
        obj = Set(Types.BOOL)
        cs = ref(obj)
        assert str(cs) == 'ISet<bool>'

    def test_map(self):
        obj = Map(Types.STRING, Types.FLOAT)
        cs = ref(obj)
        assert str(cs) == 'IDictionary<string, float>'

    def test_native(self):
        cs = ref(Types.INT64)
        assert cs == 'long'

    def test_enum_value(self):
        enum = Enum('Number')
        one = enum.add_value('ONE')

        module = Module('test.module')
        module.add_definition(enum)

        cs = ref(one)
        assert str(cs) == 'Number.ONE'

    def test_interface(self):
        iface = Interface('Interface')
        module = Module('test.module')
        module.add_definition(iface)

        cs = ref(iface)
        assert str(cs) == 'IInterface'

    def test_message(self):
        msg = Message('Message')
        module = Module('test.module')
        module.add_definition(msg)

        cs = ref(msg)
        assert str(cs) == 'Message'
