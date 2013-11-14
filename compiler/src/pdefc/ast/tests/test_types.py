# encoding: utf-8
import unittest
from pdefc.ast import Package
from pdefc.ast.types import *
from pdefc.ast.modules import *


class TestDefinition(unittest.TestCase):
    def test_validate_is_defined_after__ok(self):
        def0 = Definition(TypeEnum.MESSAGE, 'Def0')
        def1 = Definition(TypeEnum.MESSAGE, 'Def1')

        module = Module('module')
        module.add_definition(def0)
        module.add_definition(def1)

        errors = def1._validate_is_defined_after(def0)
        assert not errors

    def test_validate_is_defined_before__but_is_not(self):
        def0 = Definition(TypeEnum.MESSAGE, 'Def0')
        def1 = Definition(TypeEnum.MESSAGE, 'Def1')

        module = Module('module', definitions=[def0, def1])
        module.link()

        errors = def0._validate_is_defined_after(def1)
        assert 'Def0 must be defined after Def1' in errors[0]

    def test_must_be_referenced_before__circular_import(self):
        def0 = Definition(TypeEnum.MESSAGE, 'Def0')
        def1 = Definition(TypeEnum.MESSAGE, 'Def1')

        module0 = Module('module0')
        module1 = Module('module1')

        module0.add_definition(def0)
        module1.add_definition(def1)

        module0.add_imported_module('module1', module1)
        module1.add_imported_module('module0', module0)

        package = Package(modules=[module0, module1])
        package._link()

        errors = def1._validate_is_defined_after(def0)
        assert 'modules circularly import each other' in errors[0]


class TestNativeTypes(unittest.TestCase):
    def test_get_by_type(self):
        ntypes = NativeType._BY_TYPE.values()
        for ntype in ntypes:
            assert NativeType.get(ntype.type) is ntype
