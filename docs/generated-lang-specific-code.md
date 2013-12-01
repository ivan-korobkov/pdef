Pdef Generated and Language-Specific Code
=========================================
The compiler does not generate any serialization/deserialization or invocation code.
Instead, it generates simple classes and interfaces with static descriptors
which are used to implement dynamic serialization and remote invocation.
Actual formats and RPC implementations are loosely coupled with the generated code.

Contents
--------

- [Descriptors](#descriptors)
- [Invocations](#invocations)
- [Circular references](#circular-references)


Descriptors
-----------
Generated messages, enums and interfaces have static descriptors. The descriptors
provide meta-information about types, fields, subtypes, methods, arguments, etc.
See the implementations for more information:

- [Java descriptors](https://github.com/pdef/pdef-java/tree/master/java/src/main/java/io/pdef/descriptors).
- [Python descriptors](https://github.com/pdef/pdef-python/blob/master/python/src/pdef/descriptors.py).
- [Objective-C descriptors](https://github.com/pdef/pdef-objc/blob/master/Pdef/PDDescriptors.h).

Example Java message descriptor:
```java
public class TestMessage extends io.pdef.AbstractMessage {
    private String string0;
    private Boolean bool0;
    private Integer int0;

    // Constructors and getters/setters are omitted.

    public static final io.pdef.descriptors.MessageDescriptor<TestMessage> DESCRIPTOR = io.pdef.descriptors.MessageDescriptor.<TestMessage>builder()
            .setJavaClass(TestMessage.class)
            .setProvider(new io.pdef.Provider<TestMessage>() {
                public TestMessage get() { return new TestMessage(); }
            })
            .addField(io.pdef.descriptors.FieldDescriptor.<TestMessage, String>builder()
                    .setName("string0")
                    .setType(io.pdef.descriptors.Descriptors.string)
                    .setAccessor(new io.pdef.descriptors.FieldAccessor<TestMessage, String>() {
                        public String get(TestMessage message) { return message.string0; }
                        public void set(TestMessage message, String value) { message.string0 = value; }
                    })
                    .build())
            .addField(io.pdef.descriptors.FieldDescriptor.<TestMessage, Boolean>builder()
                    .setName("bool0")
                    .setType(io.pdef.descriptors.Descriptors.bool)
                    .setAccessor(new io.pdef.descriptors.FieldAccessor<TestMessage, Boolean>() {
                        public Boolean get(TestMessage message) { return message.bool0; }
                        public void set(TestMessage message, Boolean value) { message.bool0 = value; }
                    })
                    .build())
            .addField(io.pdef.descriptors.FieldDescriptor.<TestMessage, Integer>builder()
                    .setName("int0")
                    .setType(io.pdef.descriptors.Descriptors.int32)
                    .setAccessor(new io.pdef.descriptors.FieldAccessor<TestMessage, Integer>() {
                        public Integer get(TestMessage message) { return message.int0; }
                        public void set(TestMessage message, Integer value) { message.int0 = value; }
                    })
                    .build())
            .build();
```

Example Python message descriptor:
```python
class TestMessage(pdef.Message):
    string0 = descriptors.field('string0', lambda: descriptors.string0)
    bool0 = descriptors.field('bool0', lambda: descriptors.bool0)
    int0 = descriptors.field('int0', lambda: descriptors.int32)
    descriptor = descriptors.message(lambda: TestMessage,
        fields=(string0, bool0, int0, )
    )

    has_string0 = string0.has_property
    has_bool0 = bool0.has_property
    has_int0 = int0.has_property

    def __init__(self,
                 string0=None,
                 bool0=None,
                 int0=None):
        self.string0 = string0
        self.bool0 = bool0
        self.int0 = int0
```

Example Objective-C message descriptor:
```objectivec
@implementation TestMessage {
    BOOL _string0_isset;
    BOOL _bool0_isset;
    BOOL _int0_isset;
}
static PDMessageDescriptor *_TestMessageDescriptor;

// Getters/setters are omitted

+ (void)initialize {
    if (self != [TestMessage class]) {
        return;
    }

    _TestMessageDescriptor = [[PDMessageDescriptor alloc]
            initWithClass:[TestMessage class]
                     base:nil
       discriminatorValue:0
         subtypeSuppliers:@[]
                   fields:@[
    [[PDFieldDescriptor alloc] initWithName:@"string0" typeSupplier:^PDDataTypeDescriptor *() { return [PDDescriptors string]; } discriminator:NO],
    [[PDFieldDescriptor alloc] initWithName:@"bool0" typeSupplier:^PDDataTypeDescriptor *() { return [PDDescriptors bool0]; } discriminator:NO],
    [[PDFieldDescriptor alloc] initWithName:@"int0" typeSupplier:^PDDataTypeDescriptor *() { return [PDDescriptors int32]; } discriminator:NO],
                           ]];
}
@end
```

Invocations
-----------
Pdef is RPC-agnostic. It wraps method calls into immutable linked language-specific
invocations, which encapsulate a method descriptor, arguments and a parent invocation.
In Python and Java this is done via dynamic proxies, in Objective-C a generic client
is generated for each protocol.

Proxies capture method calls and convert them into invocations. If the method is terminal
(is void or returns a data type) the result invocation is passed to an invocation handler,
otherwise, a new interface proxy is returned with this invocation as its parent.

RPC implementations need to provide a specific handler which serializes an invocation chain to an
RPC-request, sends it, receives and returns the result.

- [Java invocation](https://github.com/pdef/pdef-java/blob/master/java/src/main/java/io/pdef/Invocation.java)
  and [proxy](https://github.com/pdef/pdef-java/blob/master/java/src/main/java/io/pdef/InvocationProxy.java).
- [Python invocation and proxy](https://github.com/pdef/pdef-python/blob/master/python/src/pdef/invoke.py).
- [Objective-C invocation](https://github.com/pdef/pdef-objc/blob/master/Pdef/PDInvocation.h).


Circular references
-------------------
Message base descriptors can be referenced directly because the compiler guarantees serial
definition and module order in inheritance trees. Fields, methods and arguments can use a simple
lazy referencing technique via lambdas, closures, blocks, etc to support circular references.

Example:
```pdef
message User {
    bestFriend  User;  // References itself.
}
```

Python lambda implementation:
```python
class User(object):
    bestFriend = pdef.descriptors.field('bestFriend', type=lambda: User.descriptor)
    descriptor = pdef.descriptors.message(pyclass=lambda: User, fields=[bestFriend])
```

Java anonymous class implementation:
```java
public class User {
    public static final MessageDescriptor DESCRIPTOR = MessageDescriptor.builder()
        .addField(FieldDescriptor.builder()
            .setName("bestFriend")
            .setType(new Provider<MessageDescriptor> {
                public MessageDescriptor get() {
                    return User.DESCRIPTOR;
                }
            }.build())
        .build();
}
```

Objective-C blocks implementation:
```objectivec
@implementation User {
    static PDMessageDescriptor _UserDescriptor;
}

+ (PDMessageDescriptor *)typeDescriptor {
    return _UserDescriptor;
}

+ (void)initialize {
    if (self != [User class]) {
        return;
    }

    _UserDescriptor = [[PDMessageDescriptor alloc]
            initWithClass:[User class]
                   fields:@[
                            [[PDFieldDescriptor alloc]
                                initWithName:@"bestFriend"
                                typeSupplier:^PDDataTypeDescriptor *() {
                                    return [User typeDescriptor];
                                }]
                           ]];
}
@end
```
