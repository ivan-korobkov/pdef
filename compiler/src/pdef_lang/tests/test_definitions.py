# encoding: utf-8
import unittest
from pdef_lang.definitions import *
from pdef_lang.modules import *


class TestDefinition(unittest.TestCase):
    def test_validate_is_defined_before__ok(self):
        def0 = Definition(Type.MESSAGE, 'def0')
        def1 = Definition(Type.MESSAGE, 'def1')

        module = Module('module')
        module.add_definition(def0)
        module.add_definition(def1)

        errors = def0._validate_is_defined_before(def0)
        assert not errors

    def test_validate_is_defined_before__but_is_not(self):
        def0 = Definition(Type.MESSAGE, 'def0')
        def1 = Definition(Type.MESSAGE, 'def1')

        module = Module('module')
        module.add_definition(def0)
        module.add_definition(def1)

        errors = def1._validate_is_defined_before(def0)
        assert 'must be defined before' in errors[0].message

    def test_must_be_referenced_before__circular_import(self):
        def0 = Definition(Type.MESSAGE, 'def0')
        def1 = Definition(Type.MESSAGE, 'def1')

        module0 = Module('module0')
        module1 = Module('module1')

        module0.add_definition(def0)
        module1.add_definition(def1)

        module0.add_imported_module('module1', module1)
        module1.add_imported_module('module0', module0)

        errors = def0._validate_is_defined_before(def1)
        assert 'modules circularly import each other' in errors[0].message


class TestNativeTypes(unittest.TestCase):
    def test_get_by_type(self):
        ntypes = NativeTypes._BY_TYPE.values()
        for ntype in ntypes:
            assert NativeTypes.get_by_type(ntype.type) is ntype
