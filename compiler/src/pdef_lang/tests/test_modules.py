# encoding: utf-8
import unittest
from pdef_lang import Definition, Type
from pdef_lang.modules import *
from pdef_lang.packages import Package


class TestModule(unittest.TestCase):
    def test_add_import(self):
        '''Should add a new import to a module.'''
        import0 = AbsoluteImport('imported')
        module = Module('module')
        module.add_import(import0)

        assert module.imports == [import0]

    def test_add_get_definition(self):
        '''Should add a new definition to a module.'''
        def0 = Definition(Type.MESSAGE, 'Test')
        module = Module('test')
        module.add_definition(def0)

        assert module.get_definition('Test') is def0

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
        errors = module.link_imports()

        assert not errors
        assert len(module.imported_modules) == 2
        assert module.get_imported_module('package.module0') is module0
        assert module.get_imported_module('module1') is module1

    def test_validate_module__duplicate_imports(self):
        module = Module('test')
        module.add_imported_module('submodule', Module('module0.submodule'))
        module.add_imported_module('submodule', Module('module1.submodule'))
        errors = module.validate()

        assert len(errors) == 1
        assert 'duplicate import' in errors[0].message

    def test_validate_module__duplicate_definition(self):
        '''Should prevent adding a duplicate definition to a module.'''
        def0 = Definition(Type.MESSAGE, 'Test')
        def1 = Definition(Type.MESSAGE, 'Test')

        module = Module('test')
        module.add_definition(def0)
        module.add_definition(def1)
        errors = module.validate()

        assert len(errors) == 1
        assert 'duplicate definition or import' in errors[0].message

    def test_validate_module__definition_import_clash(self):
        '''Should prevent adding a definition to a module when its name clashes with an import.'''
        module = Module('test')
        module.add_imported_module('clash', Module('imported'))

        def0 = Definition(Type.MESSAGE, 'clash')
        module.add_definition(def0)
        errors = module.validate()

        assert len(errors) == 1
        assert 'duplicate definition or import' in errors[0].message

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


class TestAbsoluteImport(unittest.TestCase):
    def test_link(self):
        module = Module('package.module')
        package = Package()
        package.add_module(module)

        import0 = AbsoluteImport('package.module')
        imodules, errors = import0.link(package)

        assert imodules[0].module is module
        assert not errors

    def test_link__error(self):
        package = Package()
        import0 = AbsoluteImport('package.module')
        imodules, errors = import0.link(package)

        assert not imodules
        assert 'module not found' in errors[0].message


class TestRelativeImport(unittest.TestCase):
    def test_link(self):
        module0 = Module('package.system.module0')
        module1 = Module('package.system.module1')
        package = Package()
        package.add_module(module0)
        package.add_module(module1)

        import0 = RelativeImport('package.system', ['module0', 'module1'])
        imodules, errors = import0.link(package)

        assert imodules[0].module is module0
        assert imodules[1].module is module1
        assert not errors

    def test_link__error(self):
        package = Package()

        import0 = RelativeImport('package.system', ['module0', 'module1'])
        imodules, errors = import0.link(package)

        assert not imodules
        assert "module not found 'package.system.module0'" in errors[0].message
        assert "module not found 'package.system.module1'" in errors[1].message
