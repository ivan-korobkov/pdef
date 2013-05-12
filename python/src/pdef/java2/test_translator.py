# encoding: utf-8
import unittest
from pdef.lang import *
from pdef.java2.translator import *


class TestJavaEnum(unittest.TestCase):
    def test(self):
        enum = Enum('Number', 'ONE', 'TWO')
        module = Module('test.module')
        module.add_definition(enum)

        translator = JavaTranslator('/dev/null')
        jenum = JavaEnum(enum, translator.enum_template)
        print jenum.code
        assert jenum.code


class TestInterface(unittest.TestCase):
    def test(self):
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

        module0 = Module('test.module0')
        module0.add_definitions(base0, base1)

        module1 = Module('test.module1')
        module1.add_definition(iface)

        translator = JavaTranslator('/dev/null')
        jiface = JavaInterface(iface, translator.interface_template)
        print jiface.code
        assert jiface.code


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

        module0 = Module('test.module0')
        module0.add_definitions(enum, base, msg0)

        module1 = Module('test.module1')
        module1.add_definition(msg)

        translator = JavaTranslator('/dev/null')
        jmsg = JavaMessage(msg, translator.message_template)
        jbase = JavaMessage(base, translator.message_template)
        print jbase.code
        #print jmsg.code
        #assert jmsg.code
