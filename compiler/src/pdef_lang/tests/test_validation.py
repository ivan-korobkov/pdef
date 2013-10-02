# encoding: utf-8
import unittest
from pdef_compiler.lang import *
from pdef_compiler.validator import *


class TestValidator(unittest.TestCase):
    def setUp(self):
        self.validator = Validator()

    def test_validate_module__duplicate_imports(self):
        module = Module('test')
        module.add_imported_module('submodule', Module('module0.submodule'))
        module.add_imported_module('submodule', Module('module1.submodule'))

        errors = self.validator._validate_module(module)
        assert len(errors) == 1
        assert 'duplicate import' in errors[0].message

    def test_validate_module__duplicate_definition(self):
        '''Should prevent adding a duplicate definition to a module.'''
        def0 = Definition(Type.REFERENCE, 'Test')
        def1 = Definition(Type.REFERENCE, 'Test')

        module = Module('test')
        module.add_definition(def0)
        module.add_definition(def1)

        errors = self.validator._validate_module(module)
        assert len(errors) == 1
        assert 'duplicate definition' in errors[0].message

    def test_validate_module__definition_import_clash(self):
        '''Should prevent adding a definition to a module when its name clashes with an import.'''
        module = Module('test')
        module.add_imported_module('clash', Module('imported'))

        def0 = Definition(Type.REFERENCE, 'clash')
        module.add_definition(def0)

        errors = self.validator._validate_module(module)
        assert len(errors) == 1
        assert 'duplicate definition or import' in errors[0].message


class TestAbstractValidator(unittest.TestCase):
    def __init__(self):
        self.validator = AbstractValidator()

    def test_has_import_circle__true(self):
        # 0 -> 1 -> 2 -> 0
        module0 = Module('module0')
        module1 = Module('module1')
        module2 = Module('module2')

        module0.add_imported_module('module1', module1)
        module1.add_imported_module('module2', module2)
        module2.add_imported_module('module0', module0)

        assert self.validator._has_import_circle(module0, module2)

    def test_has_import_circle__false(self):
        # 0 -> 1 -> 2
        module0 = Module('module0')
        module1 = Module('module1')
        module2 = Module('module2')

        module0.add_imported_module('module0', module1)
        module1.add_imported_module('module0', module2)

        assert self.validator._has_import_circle(module0, module2) is False
