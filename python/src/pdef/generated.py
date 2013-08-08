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
        subtypes={
            test.module0.Type.MSG: lambda: test.module0.Message.__descriptor__,
        },
        declared_fields=(
            pdef.descriptors.field('type', lambda: test.module0.Type.__descriptor__),
            pdef.descriptors.field('field0', lambda: pdef.descriptors.bool0),
            pdef.descriptors.field('field1', lambda: pdef.descriptors.int16)
        )
    )

    def __init__(self, type=None, field0=None, field1=None,):
        self.type = type
        self.field0 = field0
        self.field1 = field1



class Message0(pdef.types.Message):
    __descriptor__ = pdef.descriptors.message(lambda: Message0,
    )

    def __init__(self):
        pass



class Message(test.module0.Base):
    __descriptor__ = pdef.descriptors.message(lambda: Message,
        base=test.module0.Base.__descriptor__,
        base_type=test.module0.Type.MSG,
        declared_fields=(
            pdef.descriptors.field('field2', lambda: pdef.descriptors.int32),
            pdef.descriptors.field('field3', lambda: pdef.descriptors.string),
            pdef.descriptors.field('field4', lambda: test.module0.Message0.__descriptor__),
            pdef.descriptors.field('field5', lambda: test.module0.Base.__descriptor__),
            pdef.descriptors.field('field6', lambda: pdef.descriptors.list(test.module0.Message.__descriptor__))
        )
    )

    def __init__(self, type=None, field0=None, field1=None, field2=None, field3=None, field4=None, field5=None, field6=None,):
        super(Message, self).__init__(type=type, field0=field0, field1=field1, )
        self.field2 = field2
        self.field3 = field3
        self.field4 = field4
        self.field5 = field5
        self.field6 = field6


class Base0(pdef.types.Interface):
    __descriptor__ = pdef.descriptors.interface(lambda: Base0,
        declared_methods=(
            pdef.descriptors.method('ping', lambda: pdef.descriptors.void, args=()),
        )
    )

    def ping(self,):
        pass


class Base1(pdef.types.Interface):
    __descriptor__ = pdef.descriptors.interface(lambda: Base1,
        declared_methods=(
            pdef.descriptors.method('pong', lambda: pdef.descriptors.void, args=()),
            )
        )

    def pong(self,):
        pass


class Interface(test.module0.Base1):
    __descriptor__ = pdef.descriptors.interface(lambda: Interface,
        base=test.module0.Base1,
        declared_methods=(
            pdef.descriptors.method('echo', lambda: pdef.descriptors.string, args=(
                ('text', lambda: pdef.descriptors.string))),
            pdef.descriptors.method('sum', lambda: pdef.descriptors.int32, args=(
                ('z', lambda: pdef.descriptors.int32),
                ('a', lambda: pdef.descriptors.int32))),
            pdef.descriptors.method('abc', lambda: pdef.descriptors.string, args=(
                ('a', lambda: pdef.descriptors.string),
                ('b', lambda: pdef.descriptors.string),
                ('c', lambda: pdef.descriptors.string))),
            pdef.descriptors.method('base0', lambda: test.module0.Base0.__descriptor__, args=()),
            pdef.descriptors.method('base1', lambda: test.module0.Base1.__descriptor__, args=()),
            )
        )

    def echo(self, text=None,):
        pass

    def sum(self, z=None, a=None,):
        pass

    def abc(self, a=None, b=None, c=None,):
        pass

    def base0(self,):
        pass

    def base1(self,):
        pass
