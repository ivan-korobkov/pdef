# encoding: utf-8
import unittest
from pdefc.ast import AbsoluteImport, RelativeImport
from pdefc.ast.types import Definition, TypeEnum
from pdefc.ast.enums import Enum
from pdefc.ast.modules import *
from pdefc.ast.messages import Message
from pdefc.ast.packages import Package


class TestModule(unittest.TestCase):
    def test_add_import(self):
        '''Should add a new import to a module.'''
        import0 = AbsoluteImport('imported')
        module = Module('module')
        module.add_import(import0)

        assert module.imports == [import0]

    def test_add_get_definition(self):
        '''Should add a new definition to a module.'''
        def0 = Definition(TypeEnum.MESSAGE, 'Test')
        module = Module('test')
        module.add_definition(def0)

        assert module.get_definition('Test') is def0

    # Lookup

    def test_lookup(self):
        '''Should find up a user-defined definition by its reference.'''
        def0 = Definition(TypeEnum.MESSAGE, 'Test')

        module = Module('test')
        module.add_definition(def0)

        result = module.lookup('Test')
        assert result is def0

    def test_lookup__enum_value(self):
        '''Should find an enum value by its name.'''
        enum = Enum('Number')
        one = enum.create_value('One')

        module = Module('test')
        module.add_definition(enum)

        result = module.lookup('Number.One')
        assert result is one

    def test_lookup__imported_definition(self):
        '''Should find an imported definition.'''
        def0 = Definition(TypeEnum.MESSAGE, 'Test')

        module0 = Module('test.module0')
        module0.add_definition(def0)

        module1 = Module('module1')
        module1.add_imported_module('test.module0', module0)

        result = module1.lookup('test.module0.Test')
        assert result is def0

    def test_lookup__imported_enum_value(self):
        '''Should find an imported enum value.'''
        enum = Enum('Number')
        one = enum.create_value('One')

        module0 = Module('test.module0')
        module0.add_definition(enum)

        module1 = Module('module1')
        module1.add_imported_module('module0', module0)

        result = module1.lookup('module0.Number.One')
        assert result is one

    # Linking.

    def test_link_imports(self):
        '''Should link module imports.'''
        module0 = Module('package.module0')
        module1 = Module('package.module1')

        module = Module('module')
        module.add_import(AbsoluteImport('package.module0'))
        module.add_import(RelativeImport('package', ['module1']))

        package = Package()
        package.add_module(module)
        package.add_module(module0)
        package.add_module(module1)
        errors = module.link(package)

        assert not errors
        assert len(module.imported_modules) == 2
        assert module.get_imported_module('package.module0') is module0
        assert module.get_imported_module('module1') is module1

    def test_link_definitions(self):
        msg0 = Message('Message0')
        msg0.create_field('field0', 'Message1')

        msg1 = Message('Message1')
        msg1.create_field('field1', 'Message0')

        module = Module('module')
        module.add_definition(msg0)
        module.add_definition(msg1)
        module._link_definitions()

        assert msg0.fields[0].type is msg1
        assert msg1.fields[0].type is msg0

    # Validation.

    def test_validate_module__duplicate_imports(self):
        module = Module('test', path='/home/ivan/test.pdef')
        module.add_imported_module('submodule', Module('module0.submodule'))
        module.add_imported_module('submodule', Module('module1.submodule'))
        errors = module.validate()

        assert len(errors) == 1
        assert 'Duplicate import' in str(errors[0])

    def test_validate_module__duplicate_definition(self):
        '''Should prevent adding a duplicate definition to a module.'''
        def0 = Definition(TypeEnum.MESSAGE, 'Test')
        def1 = Definition(TypeEnum.MESSAGE, 'Test')

        module = Module('test')
        module.add_definition(def0)
        module.add_definition(def1)
        errors = module.validate()

        assert len(errors) == 1
        assert 'Duplicate definition or import' in str(errors[0])

    def test_validate_module__definition_import_clash(self):
        '''Should prevent adding a definition to a module when its name clashes with an import.'''
        module = Module('test')
        module.add_imported_module('clash', Module('imported'))

        def0 = Definition(TypeEnum.MESSAGE, 'clash')
        module.add_definition(def0)
        errors = module.validate()

        assert len(errors) == 1
        assert 'Duplicate definition or import' in str(errors[0])

    def test_has_import_circle__true(self):
        # 0 -> 1 -> 2 -> 0
        module0 = Module('module0')
        module1 = Module('module1')
        module2 = Module('module2')

        module0.add_imported_module('module1', module1)
        module1.add_imported_module('module2', module2)
        module2.add_imported_module('module0', module0)

        assert module0._has_import_circle(module2)

    def test_has_import_circle__false(self):
        # 0 -> 1 -> 2
        module0 = Module('module0')
        module1 = Module('module1')
        module2 = Module('module2')

        module0.add_imported_module('module0', module1)
        module1.add_imported_module('module0', module2)

        assert module0._has_import_circle(module2) is False
