# encoding: utf-8
import unittest
from pdefc.ast.types import NativeType
from pdefc.ast.enums import Enum
from pdefc.ast.messages import Message
from pdefc.ast.modules import *
from pdefc.packages import *


class TestPackage(unittest.TestCase):
    def test_lookup_module(self):
        module = Module('module')

        package = Package('package')
        package.add_module(module)

        assert package.modules == [module]
        assert package.lookup_module('package.module') is module

    def test_lookup_module__from_dependency(self):
        module = Module('module')

        dep = Package('dependency')
        dep.add_module(module)

        package = Package('package')
        package.add_dependency(dep)

        assert package.lookup_module('dependency.module') is module

    def test_link__duplicate_modules(self):
        module0 = Module('module')
        module1 = Module('module')

        package = Package('package')
        package.add_module(module0)
        package.add_module(module1)
        errors = package._link()

        assert len(errors) == 1
        assert 'Duplicate module' in errors[0]

    def test_build(self):
        enum = Enum('Enum')
        one_type = enum.create_value('ONE')

        zero = Message('Zero')
        one = Message('One', base=zero, discriminator_value=one_type)

        module = Module('module')
        module.add_definition(zero)
        module.add_definition(one)

        package = Package('package')
        package.add_module(module)

        package._link()
        package._build()

        assert one in zero.subtypes

    def test_validate(self):
        msg = Message('Message')
        msg.create_field('field', NativeType.VOID)  # It's an error, not a data type.

        module = Module('module')
        module.add_definition(msg)

        package = Package('package')
        package.add_module(module)
        errors = package._validate()

        assert len(errors) == 1

    def test_validate_name__required(self):
        package = Package('')
        errors = package._validate()

        assert 'Package name required' in errors[0]

    def test_validate_name__wrong_name(self):
        package0 = Package('wrong-package')
        package1 = Package('_wrong')
        package2 = Package('1234_wrong')

        errors0 = package0._validate()
        errors1 = package1._validate()
        errors2 = package2._validate()

        assert 'Wrong package name' in errors0[0]
        assert 'Wrong package name' in errors1[0]
        assert 'Wrong package name' in errors2[0]
