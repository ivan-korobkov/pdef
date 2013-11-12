# encoding: utf-8
import unittest
from pdefc.ast.types import NativeType
from pdefc.ast.enums import Enum
from pdefc.ast.messages import Message
from pdefc.ast.modules import *
from pdefc.ast.packages import *


class TestPackage(unittest.TestCase):
    def test_add_get_module(self):
        module = Module('package.module')

        package = Package()
        package.add_module(module)

        assert package.modules == [module]
        assert package.get_module('package.module') is module

    def test_include_package_get_module(self):
        module = Module('included.module')
        package_to_include = Package()
        package_to_include.add_module(module)

        package = Package()
        package.include(package_to_include)

        assert package.get_module('included.module') is module
        assert package.modules == []

    def test_link__duplicate_modules(self):
        module0 = Module('package.module')
        module1 = Module('package.module')

        package = Package()
        package.add_module(module0)
        package.add_module(module1)
        errors = package.link()

        assert len(errors) == 1
        assert 'Duplicate module' in errors[0]

    def test_link__module_clashes_with_included_one(self):
        module = Module('module')
        included = Module('module')

        package = Package()
        package.add_module(module)
        package.include_module(included)
        errors = package.link()

        assert len(errors) == 1
        assert 'Module clashes with an included module' in errors[0]

    def test_build(self):
        enum = Enum('Enum')
        one_type = enum.create_value('ONE')

        zero = Message('Zero')
        one = Message('One', base=zero, discriminator_value=one_type)

        module = Module('module')
        module.add_definition(zero)
        module.add_definition(one)

        package = Package()
        package.add_module(module)

        package.link()
        package.build()

        assert one in zero.subtypes

    def test_validate(self):
        msg = Message('Message')
        msg.create_field('field', NativeType.VOID)  # It's an error, not a data type.

        module = Module('module')
        module.add_definition(msg)

        package = Package()
        package.add_module(module)
        errors = package.validate()

        assert len(errors) == 1
