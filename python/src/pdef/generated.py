# encoding: utf-8
from pdef import descriptors
from pdef.types import Message, Type


class TestMessage(Message):
    __descriptor__ = descriptors.message(lambda: TestMessage,
         subtypes={
             Type.ENUM: lambda: descriptors.enum,
         },
         fields=(
             descriptors.field('name', lambda: descriptors.int16),
             descriptors.field('name', lambda: descriptors.int32),
             descriptors.field('name', lambda: descriptors.bool0),
         ))

    def __init__(self, field0=None, field1=None, field2=None, field3=None):
        self.field0 = field0
        self.field1 = field1
        self.field2 = field2
        self.field3 = field3


class TestMessage2(TestMessage):
    __descriptor__ = descriptors.message(lambda: TestMessage2,
         fields=(
             descriptors.field('name', lambda: descriptors.string),
             descriptors.field('name', lambda: descriptors.int32),
         ))

    def __init__(self, field0=None, field1=None, field2=None, field4=None):
        super(TestMessage2, self).__init__(field0, field1, field2)
        self.field4 = field4


class TestEnum(object):
    ONE = 'one'
    TWO = 'two'
    THREE = 'three'

    __descriptor__ = descriptors.enum(lambda: TestEnum, ONE, TWO, THREE)
