# encoding: utf-8
import unittest
from pdef.lang import *
from pdef.java import *


class TestJavaEnum(unittest.TestCase):
    def setUp(self):
        self.enum = Enum('ObjectType',values=[lang.EnumValue('OBJECT'), lang.EnumValue('USER')])
        module = Module('module')
        module.add_definitions(self.enum)

    def test(self):
        enum = JavaEnum(self.enum)
        assert str(enum.name) == 'ObjectType'
        assert str(enum.package) == 'module'

    def test_code(self):
        enum = JavaEnum(self.enum)
        print enum.code
        assert enum.code


class TestJavaMessage(unittest.TestCase):
    def setUp(self):
        int32 = Native('int32', options=NativeOptions(
            java_type='int',
            java_boxed='Integer',
            java_descriptor='pdef.provided.NativeValueDescriptors.getInt32()',
            java_default='0'
        ))
        string = Native('String', options=NativeOptions(
            java_type='String',
            java_boxed='String',
            java_descriptor='pdef.provided.NativeValueDescriptors.getString()',
            java_default='null'
        ))

        self.msg = Message('Message', declared_fields=[
            Field('field1', int32),
            Field('field2', string)
        ])
        module = Module('module')
        module.add_definitions(self.msg)

    def test_code(self):
        jmsg = JavaMessage(self.msg)
        print jmsg.code
        assert jmsg.code


class TestGenericInheritedJavaMessage(unittest.TestCase):
    def setUp(self):
        int32 = Native('int32', options=NativeOptions(
            java_type='int',
            java_boxed='Integer',
            java_descriptor='pdef.provided.NativeValueDescriptors.getInt32()',
            java_default='0'
        ))

        base_var = Variable('R')
        base = Message('Base', variables=[base_var],
            declared_fields=[Field('field0', base_var)])

        var = Variable('T')
        msg = Message('Example', variables=[var], base=base.parameterize(var),
            base_type='type',
            declared_fields=[Field('field1', var), Field('field2', int32)])

        module = Module('pdef.fixtures')
        module.add_definitions(base, msg)

        self.base = base
        self.msg = msg

    def test_base(self):
        jbase = JavaMessage(self.base)
        print jbase.code

    def test_code(self):
        jmsg = JavaMessage(self.msg)
        print jmsg.code
        assert jmsg.code


class TestPolymorphicJavaMessage(unittest.TestCase):
    def setUp(self):
        base_type = EnumValue('BASE')
        user_type = EnumValue('USER')
        photo_type = EnumValue('PHOTO')
        enum = Enum('Type')
        enum.add_values(base_type, user_type, photo_type)

        field = Field('discriminator', enum)
        base = Message('Base', polymorphism=MessagePolymorphism(field, base_type))
        base.add_fields(field)

        user = Message('User', base=base, base_type=user_type)
        photo = Message('Photo', base=base, base_type=photo_type)

        base.compile_polymorphism()
        user.compile_base_type()
        photo.compile_base_type()

        module = Module('pdef.fixtures')
        module.add_definitions(enum, base, user, photo)
        self.type = enum
        self.base = base
        self.user = user
        self.photo = photo

    def test(self):
        jtype = JavaEnum(self.type)
        print jtype.code

        jbase = JavaMessage(self.base)
        print jbase.code

        juser = JavaMessage(self.user)
        print juser.code

        jphoto = JavaMessage(self.photo)
        print jphoto.code
