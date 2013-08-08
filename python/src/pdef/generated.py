# encoding: utf-8
from pdef import descriptors
from pdef.types import Message, Type, Enum

# encoding: utf-8
import pdef.types
import pdef.descriptors

class Type(pdef.types.Enum):
    MSG = 'MSG'
    TWO = 'TWO'
    MALE = 'MALE'

    __descriptor__ = pdef.descriptors.enum(lambda: Type,
        MSG, TWO, MALE)


class Base(pdef.types.Message):
    __descriptor__ = pdef.descriptors.message(lambda: Base,
        subtypes=(
            (test.module0.Type.MSG, lambda: test.module0.Message.__descriptor__),
        ),
        fields=(
            pdef.descriptors.field('type', lambda: test.module0.Type.__descriptor__),
            pdef.descriptors.field('field0', lambda: pdef.descriptors.bool0),
            pdef.descriptors.field('field1', lambda: pdef.descriptors.int16)
        ))

    def __init__(self, type=None, field0=None, field1=None,):
        self.type = type
        self.field0 = field0
        self.field1 = field1



class Message0(pdef.types.Message):
    __descriptor__ = pdef.descriptors.message(lambda: Message0,
)

    def __init__(self):
        pass



class Message(pdef.types.Message):
    __descriptor__ = pdef.descriptors.message(lambda: Message,
        fields=(
            pdef.descriptors.field('type', lambda: test.module0.Type.__descriptor__),
            pdef.descriptors.field('field0', lambda: pdef.descriptors.bool0),
            pdef.descriptors.field('field1', lambda: pdef.descriptors.int16),
            pdef.descriptors.field('field2', lambda: pdef.descriptors.int32),
            pdef.descriptors.field('field3', lambda: pdef.descriptors.string),
            pdef.descriptors.field('field4', lambda: test.module0.Message0.__descriptor__),
            pdef.descriptors.field('field5', lambda: test.module0.Base.__descriptor__),
            pdef.descriptors.field('field6', lambda: pdef.descriptors.list(test.module0.Message.__descriptor__))
        ))

    def __init__(self, type=None, field0=None, field1=None, field2=None, field3=None, field4=None, field5=None, field6=None,):
        super(Message, self).__init__(type=type, field0=field0, field1=field1, )
        self.field2 = field2
        self.field3 = field3
        self.field4 = field4
        self.field5 = field5
        self.field6 = field6
