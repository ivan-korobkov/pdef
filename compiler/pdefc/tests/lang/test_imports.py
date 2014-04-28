# encoding: utf-8
import unittest
from pdefc.lang import Module
from pdefc.lang.imports import SingleImport, BatchImport
from pdefc.lang.packages import Package


class TestAbsoluteImport(unittest.TestCase):
    def test_link(self):
        module = Module('module')
        package = Package('package')
        package.add_module(module)

        import0 = SingleImport('package.module')
        errors = import0.link(package)

        assert not errors
        assert import0.modules == [module]

    def test_link__error(self):
        package = Package('package')
        import0 = SingleImport('module')
        errors = import0.link(package)

        assert 'Module not found' in errors[0]
        assert not import0.modules


class TestRelativeImport(unittest.TestCase):
    def test_link(self):
        module0 = Module('system.module0')
        module1 = Module('system.module1')
        package = Package('package')
        package.add_module(module0)
        package.add_module(module1)

        import0 = BatchImport('package.system', ['module0', 'module1'])
        errors = import0.link(package)

        assert not errors
        assert import0.modules == [module0, module1]

    def test_link__error(self):
        package = Package('package')

        import0 = BatchImport('package.system', ['module0', 'module1'])
        errors = import0.link(package)

        assert not import0.modules
        assert 'Module not found "package.system.module0"' in errors[0]
        assert 'Module not found "package.system.module1"' in errors[1]

