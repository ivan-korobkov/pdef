# encoding: utf-8
import unittest
from pdefc.lang import Message, Enum, Interface
from pdefc.lang.types import *
from pdefc.lang.modules import *


class TestDefinition(unittest.TestCase):
    def imported_definitions(self):
        number = Enum('Number')
        one = number.create_value('ONE')
        base = Message('Base')
        simple = Message('Simple')
        imported = Module('imported', definitions=[number, base, simple])
        imported.link()

        message = Message('Message', base=base, discriminator_value=one)
        message.create_field('simple', simple)
        interface = Interface('Interface')
        interface.create_method('method0', base)
        interface.create_method('method1', message)

        module = Module('module', definitions=[message, interface])
        module.link()

        assert message.imported_definitions == {number, base, simple}
        assert interface.imported_definitions == {base}

    def test_is_defined_after(self):
        def0 = Definition(TypeEnum.MESSAGE, 'Def0')
        def1 = Definition(TypeEnum.MESSAGE, 'Def1')

        module = Module('module', definitions=[def0, def1])
        module.link()

        assert def0._is_defined_after(def1) is False
        assert def1._is_defined_after(def0) is True

    def test_is_defined_after__different_modules(self):
        def0 = Definition(TypeEnum.MESSAGE, 'Def0')
        def1 = Definition(TypeEnum.MESSAGE, 'Def1')

        module0 = Module('module0', definitions=[def0])
        module1 = Module('module0', definitions=[def1])
        module0.link()
        module1.link()

        assert def0._is_defined_after(def1) is True
        assert def1._is_defined_after(def0) is True


class TestNativeTypes(unittest.TestCase):
    def test_get_by_type(self):
        ntypes = NativeType._BY_TYPE.values()
        for ntype in ntypes:
            assert NativeType.get(ntype.type) is ntype
