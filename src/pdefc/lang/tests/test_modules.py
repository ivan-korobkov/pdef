# encoding: utf-8
import unittest
from pdefc.lang import SingleImport, BatchImport, Interface
from pdefc.lang.types import Definition, TypeEnum
from pdefc.lang.enums import Enum
from pdefc.lang.modules import *
from pdefc.lang.messages import Message
from pdefc.lang.packages import Package


class TestModule(unittest.TestCase):
    def test_fullname(self):
        module = Module('module')
        assert module.name == 'module'

    def test_fullname__with_package(self):
        package = Package('package')
        module = Module('module')
        module.package = package

        assert module.fullname == 'package.module'

    def test_fullname__matches_with_package_name(self):
        package = Package('project')
        module = Module('project')
        module.package = package

        assert module.fullname == 'project'

    # Imports

    def test_add_import(self):
        '''Should add a new import to a module.'''
        import0 = SingleImport('imported')
        module = Module('module')
        module.add_import(import0)

        assert module.imports == [import0]

    def test_add_get_definition(self):
        '''Should add a new definition to a module.'''
        def0 = Definition(TypeEnum.MESSAGE, 'Test')
        module = Module('test')
        module.add_definition(def0)

        assert module.get_definition('Test') is def0

    def test_imported_definitions(self):
        number = Enum('Number')
        one = number.create_value('ONE')
        base = Message('Base')
        imported = Message('Imported')
        module0 = Module('Module0', definitions=[number, base, imported])
        module0.link()

        message0 = Message('Message0', base=base)
        message1 = Message('Message1', discriminator_value=one)
        message1.create_field('field', message0)

        interface = Interface('Interface')
        interface.create_method('method', arg_tuples=[('arg', imported)])

        module1 = Module('Module1', definitions=[message0, message1, interface])
        module1.link()
        result = module1.imported_definitions
        assert result == {number, base, imported}

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

    def test_lookup__same_namespace__imported_module(self):
        def0 = Definition(TypeEnum.MESSAGE, 'Test')

        module0 = Module('module0', namespace='test', definitions=[def0])
        module1 = Module('module1', namespace='test')
        module1.add_imported_module(module0)

        result = module1.lookup('Test')
        assert result is def0

    def test_lookup___same_namespace__imported_module__enum_value(self):
        '''Should find an imported enum value.'''
        enum = Enum('Number')
        one = enum.create_value('One')

        module0 = Module('module0', namespace='test', definitions=[enum])
        module1 = Module('module1', namespace='test')
        module1.add_imported_module(module0)

        result = module1.lookup('Number.One')
        assert result is one

    def test_lookup__another_namespace(self):
        def0 = Definition(TypeEnum.MESSAGE, 'Test')

        module0 = Module('module0', namespace='another.namespace', definitions=[def0])
        module1 = Module('module1', namespace='namespace')
        module1.add_imported_module(module0)

        result = module1.lookup('another.namespace.Test')
        assert result is def0

    def test_lookup__another_namespace__enum_value(self):
        enum = Enum('Number')
        one = enum.create_value('One')

        module0 = Module('module0', namespace='another.namespace', definitions=[enum])
        module1 = Module('module1', namespace='namespace')
        module1.add_imported_module(module0)

        result = module1.lookup('another.namespace.Number.One')
        assert result is one

    # get_import_path.

    def test_get_import_path(self):
        # 0 -> 1 -> 2
        module0 = Module('module0')
        module1 = Module('module1')
        module2 = Module('module2')

        module0.add_imported_module(module1)
        module1.add_imported_module(module2)

        assert module0._get_import_path(module2) == [module0, module1, module2]
        assert module0._get_import_path(module1) == [module0, module1]
        assert module2._get_import_path(module0) == []

    # Linking.

    def test_link_imports(self):
        '''Should link module imports.'''
        module0 = Module('module0')
        module1 = Module('module1')

        module = Module('module')
        module.add_import(SingleImport('package.module0'))
        module.add_import(BatchImport('package', ['module1']))

        package = Package('package')
        package.add_module(module)
        package.add_module(module0)
        package.add_module(module1)
        errors = module.link(package)

        assert not errors
        assert len(module.imported_modules) == 2
        assert module.imported_modules == [module0, module1]

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

    def test_validate_name(self):
        module0 = Module('wrong-module')
        module1 = Module('_wrong')
        module2 = Module('1234_wrong')

        errors0 = module0._validate()
        errors1 = module1._validate()
        errors2 = module2._validate()

        assert 'Wrong module name' in errors0[0]
        assert 'Wrong module name' in errors1[0]
        assert 'Wrong module name' in errors2[0]

    def test_validate__duplicate_module_imports(self):
        module0 = Module('test')
        module1 = Module('test')
        module1.add_imported_module(module0)
        module1.add_imported_module(module0)
        errors = module1.validate()

        assert len(errors) == 1
        assert 'Duplicate module import' in str(errors[0])

    def test_validate__duplicate_definition(self):
        '''Should prevent adding a duplicate definition to a module.'''
        def0 = Definition(TypeEnum.MESSAGE, 'Test')
        def1 = Definition(TypeEnum.MESSAGE, 'Test')

        module = Module('test')
        module.add_definition(def0)
        module.add_definition(def1)
        errors = module.validate()

        assert len(errors) == 1
        assert 'Duplicate definition or import' in str(errors[0])
