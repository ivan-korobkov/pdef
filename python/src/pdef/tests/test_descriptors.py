# encoding: utf-8
import unittest
from mock import Mock

from pdef import descriptors
from pdef_test.messages import *
from pdef_test.inheritance import *
from pdef_test.interfaces import *


class TestMessageDescriptor(unittest.TestCase):
    def test(self):
        descriptor = TestMessage.DESCRIPTOR

        assert descriptor.pyclass is TestMessage
        assert descriptor.base is None
        assert descriptor.discriminator is None
        assert descriptor.discriminator_value is None
        assert len(descriptor.subtypes) == 0
        assert len(descriptor.fields) == 3

    def test__form(self):
        message = TestMessage.DESCRIPTOR
        form = TestForm.DESCRIPTOR

        assert not message.is_form
        assert form.is_form

    def test__nonpolymorphic_inheritance(self):
        base = TestMessage.DESCRIPTOR
        descriptor = TestDataTypes.DESCRIPTOR

        assert descriptor.pyclass is TestDataTypes
        assert descriptor.base is TestMessage.DESCRIPTOR
        assert descriptor.inherited_fields == base.fields
        assert descriptor.fields == base.fields + descriptor.declared_fields
        assert len(descriptor.subtypes) == 0

    def test__polymorphic_inheritance(self):
        base = Base.DESCRIPTOR
        subtype = Subtype.DESCRIPTOR
        subtype2 = Subtype2.DESCRIPTOR
        msubtype = MultiLevelSubtype.DESCRIPTOR
        discriminator = base.find_field('type')

        assert base.base is None
        assert subtype.base is base
        assert subtype2.base is base
        assert msubtype.base is subtype

        assert base.discriminator is discriminator
        assert subtype.discriminator is discriminator
        assert subtype2.discriminator is discriminator
        assert msubtype.discriminator is discriminator

        assert base.discriminator_value is None
        assert subtype.discriminator_value is PolymorphicType.SUBTYPE
        assert subtype2.discriminator_value is PolymorphicType.SUBTYPE2
        assert msubtype.discriminator_value is PolymorphicType.MULTILEVEL_SUBTYPE

        assert set(base.subtypes) == {subtype, subtype2, msubtype}
        assert set(subtype.subtypes) == {msubtype}
        assert not subtype2.subtypes
        assert not msubtype.subtypes

        assert base.find_subtype(None) is base
        assert base.find_subtype(PolymorphicType.SUBTYPE) is subtype
        assert base.find_subtype(PolymorphicType.SUBTYPE2) is subtype2
        assert base.find_subtype(PolymorphicType.MULTILEVEL_SUBTYPE) is msubtype


class TestFieldDescriptor(unittest.TestCase):
    field = TestMessage.DESCRIPTOR.find_field('string0')

    def test(self):
        string0 = TestMessage.DESCRIPTOR.find_field('string0')
        bool0 = TestMessage.DESCRIPTOR.find_field('bool0')

        assert string0.name == 'string0'
        assert string0.type is descriptors.string0

        assert bool0.name == 'bool0'
        assert bool0.type is descriptors.bool0

    def test_discriminator(self):
        discriminator = Base.DESCRIPTOR.find_field('type')

        assert discriminator.name == 'type'
        assert discriminator.type is PolymorphicType.DESCRIPTOR
        assert discriminator.is_discriminator

    def test_set(self):
        msg = TestMessage(string0='hello')
        self.field.set(msg, 'goodbye')
        assert msg.string0 == 'goodbye'

    def test_get(self):
        msg = TestMessage(string0='hello')
        assert self.field.get(msg) == 'hello'


class TestInterfaceDescriptor(unittest.TestCase):
    def test(self):
        descriptor = TestInterface.DESCRIPTOR
        index_method = descriptor.find_method('testIndex')

        assert descriptor.pyclass is TestInterface
        assert descriptor.exc is TestException.DESCRIPTOR
        assert len(descriptor.methods) == 11
        assert index_method
        assert descriptor.index_method is index_method


class TestMethodDescriptor(unittest.TestCase):
    def test(self):
        method = TestInterface.DESCRIPTOR.find_method('testMessage')

        assert method.name == 'testMessage'
        assert method.result is TestMessage.DESCRIPTOR
        assert len(method.args) == 1
        assert method.args[0].name == 'msg'
        assert method.args[0].type is TestMessage.DESCRIPTOR

    def test_args(self):
        method = TestInterface.DESCRIPTOR.find_method('testIndex')

        assert len(method.args) == 2
        assert method.args[0].name == 'arg0'
        assert method.args[1].name == 'arg1'
        assert method.args[0].type is descriptors.int32
        assert method.args[1].type is descriptors.int32

    def test_index_post_remote(self):
        descriptor = TestInterface.DESCRIPTOR
        index = descriptor.find_method('testIndex')
        remote = descriptor.find_method('testRemote')
        post = descriptor.find_method('testPost')
        interface = descriptor.find_method('testInterface')

        assert index.is_index
        assert index.is_remote
        assert not index.is_post

        assert not remote.is_index
        assert remote.is_remote
        assert not remote.is_post

        assert not post.is_index
        assert post.is_remote
        assert post.is_post

        assert not interface.is_index
        assert not interface.is_remote
        assert not interface.is_post

    def test_invoke(self):
        service = Mock()
        method = TestInterface.DESCRIPTOR.find_method('testIndex')
        method.invoke(service, 1, b=2)
        service.testIndex.assert_called_with(1, b=2)


class TestEnumDescriptor(unittest.TestCase):
    def test(self):
        descriptor = TestEnum.DESCRIPTOR
        assert descriptor.values == ('ONE', 'TWO', 'THREE')

    def test_find_value(self):
        descriptor = TestEnum.DESCRIPTOR
        assert descriptor.find_value('one') == TestEnum.ONE
        assert descriptor.find_value('TWO') == TestEnum.TWO


class TestListDescriptor(unittest.TestCase):
    def test(self):
        list0 = descriptors.list0(descriptors.string0)
        assert list0.element is descriptors.string0


class TestSetDescriptor(unittest.TestCase):
    def test(self):
        set0 = descriptors.set0(descriptors.int32)
        assert set0.element is descriptors.int32


class TestMapDescriptor(unittest.TestCase):
    def test(self):
        map0 = descriptors.map0(descriptors.string0, descriptors.int32)
        assert map0.key is descriptors.string0
        assert map0.value is descriptors.int32
