# encoding: utf-8
import unittest
from pdef_lang import exc, NativeType
from pdef_lang.messages import Message
from pdef_lang.modules import *
from pdef_lang.packages import *


class TestPackage(unittest.TestCase):
    def test_add_get_module(self):
        module = Module('package.module')

        package = Package()
        package.add_module(module)

        assert package.modules == [module]
        assert package.get_module('package.module') is module

    def test_link__duplicate_modules(self):
        module0 = Module('package.module')
        module1 = Module('package.module')

        package = Package()
        package.add_module(module0)
        package.add_module(module1)

        try:
            package.link()
            self.fail()
        except exc.LinkingException as e:
            assert len(e.errors) == 1
            assert 'duplicate module' in e.errors[0].message

    def test_validate(self):
        msg = Message('Message')
        msg.create_field('field', NativeType.VOID)  # It's an error, not a data type.

        module = Module('module')
        module.add_definition(msg)

        package = Package()
        package.add_module(module)

        try:
            package.validate()
            self.fail()
        except exc.ValidationException as e:
            assert len(e.errors) == 1
