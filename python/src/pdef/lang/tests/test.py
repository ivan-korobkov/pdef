# encoding: utf-8
import unittest
from pdef.lang import *


class Test(unittest.TestCase):
    def test(self):
        int32 = Native('int32')
        string = Native('string')
        List = Native('List')
        List.add_variables(Variable('E'))

        builtin_types = Module('pdef.types')
        builtin_types.add_definitions(int32, string, List)

        builtin = Package('pdef', None)
        builtin.add_modules(builtin_types)

        msg = Message('Message1')
        msg.add_fields(Field('int', Ref('int32')))
        msg.add_fields(Field('str', Ref('string')))
        msg.add_fields(Field('list', Ref('List', Ref('string'))))
        msg.add_fields(Field('msg2', Ref('module2.Message2')))

        module1 = Module('test.module1')
        module1.add_imports(ImportRef('test.module2', 'module2'))
        module1.add_definitions(msg)

        msg2 = Message('Message2', declared_fields=[
            Field('circular', Ref('test.module1.Message1'))
        ])
        module2 = Module('test.module2',
            imports=[ImportRef('test.module1')],
            definitions=[msg2])

        pkg = Package('test', builtin)
        pkg.add_modules(module1, module2)
        pkg.build()

        msg_fields = msg.declared_fields
        assert msg_fields['int'].type == int32
        assert msg_fields['str'].type == string
        assert msg_fields['msg2'].type == msg2
        assert msg_fields['list'].type.rawtype == List
        assert list(msg_fields['list'].type.variables) == [string]

        msg2_fields = msg2.declared_fields
        assert msg2_fields['circular'].type == msg


