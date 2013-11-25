Pdef Language Guide
===================
Pdef is a statically typed interface definition language. It allows to write interfaces and
data structures once and then to generate code and rpc clients/servers for different languages
(Java, Python, Objective-C).


Contents
--------
- [Packages]
- [Modules]
    - [Imports]
- [Comments]
- [Types overview]
- [Data types]:
    - Primitives
    - Containers
    - Enums
    - Messages and exceptions
        - Simple inheritance
        - Polymorphic inheritance
- [Interfaces]
- [Notes on circular reference implementation]


Packages
--------
In Pdef files must be organized in packages. A package is a named collection of modules
mapped to `*.pdef` files. A package is defined by a YAML file with a name,
dependencies and modules. Dependencies are other packages which can be accessed by this package
modules. Usually, an application is one package.

Package file:
```yaml
package:
  name: example
  description: Example application

  modules:
    - example
    - photos
    - posts
    - users
    - users.events
    - users.profile

  dependencies:
    - package_i_depend_on
    - another_package

```

Package file structure:
```
./example.yaml
./example.pdef
./photos.pdef
./posts.pdef
./users.pdef
./users/events.pdef
./users/profile.pdef
```

*Contraints:*

- All names must start with a latin letter and contain only latin letters, digits and underscores.
- Package names must be unique.
- Module names must be unique in a package.
- Circular package dependencies are forbidden.

One file as a package definition with enumerated modules is convenient,
you can pass it as a URL directly to the compiler.
```bash
# Dowloads, compiles and validates the test pdef package.
# Does not generate any code.
pdefc check https://raw.github.com/pdef/pdef/master/test/test.yaml
```


Modules
-------
Module is a `.pdef` file with **definitions** (enums, messages and interfaces).
Module names are automatically mapped to file names:
the dot is replaced with a path separator `/` plus a `.pdef` extension.
So, `users.events` module is mapped to a `./users/events.pdef` file.

A **absolute module name** is a concatenation of its package name with its relative name. So
the absolute name of `users.events` from the previous example is `example.users.events`.
A module can import other modules by their absolute names.

When a *module name matches its package name*, the absolute module name is the package name.
So `example` is an absolute name of the `example` module, **not** `example.example`.

Example module:
```
import example;
from example import photos, posts;

message User {
    id          int32;
    name        string;
    sex         Sex;
    avatar      photos.Photo;
    location    example.Location;
}

enum Sex {
    MALE,
    FEMALE
}
```


Imports
-------
A module can import other modules to access their definitions. Imports must be specified
at the top of a file before definitions. Modules can only be imported by their absolute name,
even inside the same package. There are two ways to import modules.

The first one is to import a module by its absolute name.
```
import package.module.name;

message NeedMessageFromAnotherModule {
    field   package.module.name.MyMessage;
}
```

The second is to import submodules from a package or from a module.
```
from package import module;
from package.module import submodule0, submodule1;

message MyMessage {
    field0      module.MyMessage;
    field1
}
```

*Constraints:*

- Module aliases are not supported. You cannot write `from package import module as module_alias`.
- Definition imports are not supported. You cannot import a single definition from a module
  (as in Python for example).
- Submodule access is not supported. You cannot write `from package import module` and then
  access its submodules as `module.submodule`. You need to directly import the `submodule`.


Circular imports and references
-------------------------------
Circular imports are allowed. However, there are some constraints to support scripting languages.
Interpreted languages require a serial execution order. Some things can be postponed via lazy
referencing, but inheritance cannot. So two modules can import each other as long as
the messages from the first one do not inherit the messages from the second one and vice versa.
The same is with enum discriminators (see [Polymorphic inheritance]). Usually,
this case is very rare, and this constraint almost never affects development. However,
if you encounter it, break one module into multiple ones.

Circular references are allowed, even self-references are allowed. They are supported
in almost all languages using different techniques such as forward referencing in C and
Objective-C, absolute module names and late referencing in Python, etc.
See [Notes on circular reference implementation].

Possible circular import and reference example:

`users.pdef`
```
from example import photos;

message User {
    bestFriend  User;           // References itself.
    photo       photos.Photo;
}

```

`photos.pdef`
```
from example import users;

message Photo {
    user    users.User;
}
```



Types overview
--------------
Pdef has a simple, yet powerful type system. It is built on a clear separation between
data types and interfaces. It differs from modern object-oriented languages,
which combine data and behaviour. In Pdef data types represent serializable values,
and interfaces provide behaviour. However, Pdef still can be used to create clean object-oriented
 APIs (see [interfaces]).


Data types
----------
All data types are *nullable*. It is up to a format how to represent null values,
for example, in JSON a field can be `null` or absent, and these ways are equivalent


<h3>Primitives</h3>
- `bool`: a boolean value (true/false)
- `int16`: a signed 16-bit integer
- `int32`: a signed 32-bit integer
- `int64`: a signed 64-bit integer
- `float`: a 32-bit floating point number
- `double`: a 64-bit floating point number
- `string`: a unicode string
- `datetime`: a date and time object without timezone.
- `void`: used in a method without result, it is not a data type or a primitive but is included
  here.

<h3>Containers</h3>
Containers are generic strongly typed containers.

- `list` is an ordered list of elements. An element must be a **data type**.
- `set` is an unordered set of unique elements. An element must be a **data type**.
- `map` is an unordered key-value container (a dict in some languages). A key must be a
**primitive**, a value must be a **data type**.

```pdef
message ContainersExample {
    numbers     list<int32>;
    tweets      list<Tweet>;

    ids         set<int64>;
    colors      set<Color>;

    user_names  map<int64, string>;
    photos      map<string, Photo>;
}
```


<h3>Enums</h3>
Enum is a collection of unique predefined string values. Enums are also used as discriminator
values in polymorphic inheritance, see [polymorphic inheritance].

```pdef
enum Sex {
    MALE, FEMALE;
}

enum EventType {
    USER_REGISTERED,
    USER_BANNED,
    PHOTO_UPLOADED,
    PHOTO_DELETED,
    MESSAGE_RECEIVED;
}
```

Enums backwards compatibility. Sometimes an enum specifies a collection which can be
extended in the future. For example, it can be a type of a newsfeed article. So some
clients or services can known only previous enum values, but not the new ones. In this case,
**unknown enum values must be deserialized as nulls**.


<h3>Messages and exceptions</h3>
Message (an equivalent to a struct) is collection of strongly typed fields. Each field has a
unique name and a type. Messages support [simple] and [polymorphic inheritance].
Exceptions are special messages which can be used in interfaces to declare raised exceptions.

*Constraints:*

- All message fields must have unique names.
- A field type must be a data type, message fields can references other messages.
- A field can have a message it is declared in as its type (see the `friends` field below).

```pdef
/** Example message. */
message User {
    id          int64;
    name        string;
    age         int32;
    profile     Profile;
    friends     set<User>; // References a message it is declared in.
}

/** Example exception. */
exception UserNotFound {
    userId      int64;
}
```


<h4>Simple inheritance</h4>
Simple inheritances allow one message to inherit the fields from another message. However,
in simple inheritance there is no polymorphic serialization/deserialization. In other words,
subtypes cannot be deserialized from a base type.

*Constraints:*

- A message can have only one base.
- Circular inheritance is forbidden.
- A message cannot override its base fields.
- Both a message and its base must be either exceptions or messages.
  One cannot inherit an exception from a message, or a message from an exception.
- A base must be declared before a subtype and cannot be imported from a dependent module
  (see [dependent modules]).

```pdef
message EditableUser {
    name        string;
    sex         Sex;
    birthday    datetime;
}

message User : EditableUser {
    id              int32;
    lastSeen        datetime;
    friendsCount    int32;
    likesCount      int32;
    photosCount     int32;
}

message UserWithDetails : User {
   photos       list<Photo>;
   friends      list<User>;
}
```


<h4>Polymorphic inheritance</h4>
Polymorphic inheritance allow to serialize/deserialize subtypes from a base type.
 A base with all its subtypes form an *inheritance tree*. A subtype can also inherit another
 subtype to form *multi-level inheritance*. To create a polymorphic message:

- Define an `enum` which will serve as a discriminator.
- Add a field of this enum type to a base message and mark it as a `@discriminator`.
- Specify discriminator values in subtypes as `message Subtype : Base(DiscriminatorEnum.VALUE)`.

*Constraints:*

- A `@discriminator` field type must be of an `enum` type.
- A discriminator `enum` must be declared before the base `message` and cannot be imported from
 a dependent module (see [dependent modules]).
- There can be only one `@discriminator` field per inheritance tree.
- Each subtype must have a specified unique discriminator value.
- A base must be declared before subtypes and cannot be imported from a dependent module
  (see [dependent modules]).
- A message cannot inherit a polymorphic message (a base or a subtype)
  without specifying a discriminator value. In other words, simple inheritance is forbidden
  with polymorphic messages.
- All simple inheritance constraints.

```pdef
/** Discriminator enum. */
enum EventType {
    USER_EVENT,
    USER_REGISTERED,
    USER_BANNED,
    PHOTO_UPLOADED,
}

/** Base event with a discriminator field. */
message Event {
    type   EventType @discriminator;
    time   datetime;
}

/** Base user event with a specified base and a discriminator value. */
message UserEvent : Event(EventType.USER_EVENT) {
    user    User;
}

message UserRegistered : UserEvent(EventType.USER_REGISTERED) {
    ip      string;
    browser string;
    device  string;
}

message UserBanned : UserEvent(EventType.USER_BANNED) {
    moderatorId int64;
    reason      string;
}

message PhotoUploaded : Event(EventType.PHOTO_UPLOADED) {
    photo   Photo;
    userId  int64;
}
```


Interfaces
----------
An interface is a collection of strongly typed methods. Each method has a unique name,
a number of or zero arguments, and a result. The result can be a data type, `void` or an
interface.

**Terminal/interface methods.**
A method is called a *terminal method* when it returns a data type or is `void`.
A method is called an *interface method* when it returns another interface.
Multiple chained interface methods form an *invocation chain*,
i.e. `client.users().register("John Doe")`. The last method of an invocation chain
must be a terminal one.

**@post methods.** Terminal methods can be marked as `@post` to distinguish between mutators and
accessors. HTTP RPC sends these methods as POST requests.

**@post and @query args.** Terminal method arguments can be marked as `@post` or `@query`,
so that HTTP RPC can send them as URL query args or as post form args.

**Exceptions.**
An interface can specify an exception all its methods can throw via `@throws(Exception)`.
It is impossible to specify different exceptions for different methods in an interface.
In an invocation chain child invocations *inherit a parent interface exception*
if they do no declare their own.

*Constraints:*

- Methods must have unique names.
- Arguments must have unique names.
- An argument must be of a data type.
- Only terminal methods can have `@query` arguments.
- Only `@post` terminal methods can have `@post` arguments.
- An argument cannot be `@post` and `@query` at the same time.
- Interfaces do not support inheritance (at least now).

```pdef
/** Example application interface. */
@throws(ExampleException)
interface ExampleApplication {
    /** Echoes back the provided string. */
    echo(text string) string;

    /** Returns a users interface. */
    users() Users;

    /** Returns a posts interface. */
    posts() Posts;
}

/** Application users. */
interface Users {
    /** Creates a new user, it's a mutator method marked as @post. */
    @post
    create(
        name        string @post,
        birthday    datetime @post,
        sex         Sex @post) User;

    /** Find a user by id. */
    find(id int32) User;

    /** Lists users, it's a terminal method with @query arguments. */
    list(limit int32 @query, offset int32 @query) list<User>;
}

/** Application posts. */
interface Posts {
    /** Creates a new post. */
    @post
    create(title string @post, text string @post) Post;

    /** Returns a post by its id. */
    find(id int32) Post;

    /** Like a post. */
    like(postId int64) void;
}

/** Exception discriminator enum. */
enum ExampleExceptionCode {
    WRONG_AUTH,
    NOT_FOUND,
    INVALID_DATA
}

/** Base polymorphic exception. */
exception ExampleException {
    type    ExampleExceptionCode @discriminator;
    text    string;
}

exception WrongAuthException : ExampleException(ExampleExceptionCode.WRONG_AUTH);
exception NotFoundException : ExampleException(ExampleExceptionCode.NOT_FOUND);
exception InvalidDataException : ExampleException(ExampleExceptionCode.INVALID_DATA);
```


Interface exceptions
--------------------
Exceptions are specified per interfaces. Each interface can have only one application exception
which all its methods can raise. If a chained method interface specifies another exception,
then it overrides the parent one, otherwise, the chained method inherits the parent exception.

Exception inheritance:
```pdef
@throws(ParentException)
interface Parent {
    child() Child;
}

interface Child {
    method() void;
}
```
The `Child` interface does not specify an exception, so the expected application exception
in an invocation chain `parent.child().method()` is inherited from the parent and is
`ParentException`.


Exception overriding:
```pdef
@throws(ParentException)
interface Parent {
    child() OverridingChild;
}

@throws(ChildException)
interface OverridingChild {
    method() void;
}
```
The `OverridingChild` specifies its own exception, so the expected application exception
in an invocation chain `parent.child().method()` is `ChildException`.


Notes on circular reference implementation
------------------------------------------
These are notes for the code generator developers, others can skip.

Generated code must contain static descriptors with definition meta-data which can reference
other static descriptors. Message bases can be referenced directly because inheritance tree
guarantees serial module order. Fields, methods and arguments can use a simple late
referencing technique via lambdas, closures, blocks, etc.

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
