# encoding: utf-8
import unittest
from pdef.lang import *
from pdef.python.translator import PythonTranslator, PythonInterface


class TestInterface(unittest.TestCase):
    def setUp(self):
        base0 = Interface('Base0')
        base0.create_method('ping')
        base1 = Interface('Base1')
        base1.create_method('pong')

        iface = Interface('Interface')
        iface.set_base(base0)
        iface.set_base(base1)
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
        module0.add_definition(iface)

        self.translator = PythonTranslator('/tmp')
        self.module0 = module0

    def test(self):
        self.translator.translate_module(self.module0)


class TestMessage(unittest.TestCase):
    def _test(self):
        enum = Enum('Type')
        enum.add_value('MSG')
        enum.add_value('TWO')
        enum.add_value('MALE')

        base = Message('Base')
        base.create_field('type', enum, is_discriminator=True)
        base.create_field('field0', NativeTypes.BOOL)
        base.create_field('field1', NativeTypes.INT16)
        msg0 = Message('Message0')

        msg = Message('Message')
        msg.set_base(base, enum.values['MSG'])
        msg.create_field('field2', NativeTypes.INT32)
        msg.create_field('field3', NativeTypes.STRING)
        msg.create_field('field4', msg0)
        msg.create_field('field5', base)
        msg.create_field('field6', List(msg))

        module0 = Module('test.module0')
        module0.add_definitions(enum, base, msg0, msg)
        module0.link_imports()
        module0.link_definitions()

        translator = PythonTranslator('/tmp')
        translator.translate_module(module0)

