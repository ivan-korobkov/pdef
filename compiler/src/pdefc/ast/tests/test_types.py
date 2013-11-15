# encoding: utf-8
import unittest
from pdefc.ast.types import *
from pdefc.ast.modules import *


class TestDefinition(unittest.TestCase):
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
