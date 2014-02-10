Pdef 1.1 Language Guide
=======================
Pdef is a statically typed interface definition language. It allows to write interfaces and
data structures once and then to generate code and RPC clients/servers for different languages
(Java, Python, Objective-C).


Contents
--------
- [Syntax](#syntax)
    - [Comments](#comments)
- [Packages and modules](#packages-and-modules)
    - [Packages](#packages)
    - [Modules and namespaces](#modules-and-namespaces)
    - [Imports](#imports)
    - [Circular imports](#circular-imports)
    - [Name resolution](#name-resolution)
- [Types](#types)
    - [Void](#void)
    - [Data types](#data-types):
        - [Primitives](#primitives)
        - [Containers](#containers)
        - [Enums](#enums)
        - [Messages and exceptions](#messages-and-exceptions)
        - [Inheritance](#inheritance)
        - [Polymorphic inheritance](#polymorphic-inheritance)
    - [Interfaces](#interfaces)
        - [Interface inheritance](#interface-inheritance)
        - [Interface exceptions](#interface-exceptions)
- [Nulls and defaults](#nulls-and-defaults)


Syntax
------
Pdef syntax is similar to Java/C++ with the inverted type/identifier order in fields and arguments.
All identifiers must start with a latin letter and contain only latin letters,
digits and underscores. See the [grammar specification](grammar.bnf).

```pdef
namespace example;

interface MyInterface {
    method(arg0 int32, arg1 string) list<string>;
}

message MyMessage  {
    field0      int32;
    field1      string;
}

enum Number {
    ONE, TWO;
}
```


### Comments
There are two types of comments: single-line comments and multi-line docstrings.
Docstrings can be placed at the beginning of a file, before a definition (enum, message,
interface) or before a method.

```pdef
/**
 * This is a multi-line module docstring.
 * It is a available to the code generators.
 *
 * Start each line with a star because it is used
 * as line whitespace/text delimiter when
 * the docstring is indented (as method docstrings).
 */
 namespace example;

// This is a one line comment, it is stripped from the source code.

/** Interface docstring. */
interface ExampleInterface {
    /**
     * Method docstring.
     */
    hello(name string) string;
}
```


Packages and modules
--------------------

### Packages
Pdef files are organized into packages. A package is defined in a `.yaml` file which
contains a package name, dependencies, module names and additional information.
Package names must start with a latin letter and contain only latin letters, digits
and underscores. Circular package dependencies are forbidden. Dependencies are specified
by package names with an optional default path separated by a space. Modules names
are mapped to directories and `.pdef` files, for example, `users.profile` is mapped to
`users/profile.pdef`.

Package file:
```yaml
package:
  # Package name
  name: example

  # Additional information
  version: 1.1
  url: https://github.com/pdef/pdef/
  author: Ivan Korobkov <ivan.korobkov@gmail.com>
  description: Example application

  # Module files.
  modules:
    - example
    - photos
    - users
    - users.profile

  # Other packages this package depends on.
  dependencies:
    - common
    - pdef_test https://raw.github.com/pdef/pdef/1.1/test/test.yaml
```

File structure:
```
api/
    example.yaml
    example.pdef
    photos.pdef
    users.pdef
    users/profile.pdef
```


### Modules and namespaces
Module is a `.pdef` file with enums, messages and interfaces (definitions).
Each module must declare its `namespace`. Definitions in a namespace must have unique names.

Pdef namespaces are broader then Java or C# ones and they should not be mapped to a directory
structure. Usually, one namespace per project is enough. Different packages can share the same
namespaces.

```pdef
namespace myproject;

message Hello {
    text    string;
}
```


### Imports
Imports are similar to includes in C++, they allow to access definitions from other modules.
Imports must be specified at the top of a file after the namespace. Modules are imported by their
package names and file paths without the `.pdef` extension, separated by the dots. When a module
name matches its package name it can be imported by the package name alone.

Single import:
```pdef
namespace example;
import package;           // Equivalent to "import package.package" when package/module names match.
import package.module;
```

Batch import:
```pdef
namespace example;
from package.module import submodule0, submodule1;
```

### Circular imports
Circular imports are allowed as long as the types from the first module do not inherit the
types from the second module and vice versa. Usually, this is very rare and almost never
affects the development. However, if you encounter it, break one module into multiple ones,
or merge two modules into one.

Circular references are allowed, even self-references are allowed. They can be implemented
in almost all languages via forward/lazy referencing.
See [circular references](./generated-lang-specific-code.md#circular-references)
in the [generated code guide](./generated-lang-specific-code.md).

`users.pdef`
```
namespace example;
from example import photos;     // Circular import.

message User {
    bestFriend  User;           // References a declaring type.
    photo       Photo;          // References a type from another module.
}

```

`photos.pdef`
```
namespace example;
from example import users;      // Circular import.

message Photo {
    user    User;               // References a user from another module.
}
```

### Name resolution
A type should be referenced by its name in the same namespace, and by `namespace.TypeName`
in other namespaces.

`users.pdef`
```pdef
namespace myproject;                    // Both modules share the same namespace.

import twitter.accounts;                // Assume it has the "twitter" namespace.
from myproject import images;           // Include all types from the images module.

message User {
    avatar          Image;              // Access a type in the same namespace by its name.
    twitterAccount  twitter.Account;    // Access a type in another namespace by its absolute name.
}
```

`images.pdef`
```pdef
namespace myproject;

message Image {
    id  int64;
    url string;
}
```


Types
-----
Pdef has a simple, yet powerful type system built on a clear separation between
data types and interfaces.

### Void
`void` is a special type which indicates that a method returns no result.

### Data types

#### Primitives
- `bool`: a boolean value (true/false)
- `int16`: a signed 16-bit integer
- `int32`: a signed 32-bit integer
- `int64`: a signed 64-bit integer
- `float`: a 32-bit floating point number
- `double`: a 64-bit floating point number
- `string`: a unicode string
- `datetime`: a date and time object without a time zone.

#### Containers
Containers are generic strongly typed containers.

- `list` is an ordered list of elements. An element must be a data type.
- `set` is an unordered set of unique elements. An element must be a data type.
- `map` is an unordered key-value container (a dict in some languages). A key must be a
non-null primitive, a value must be a data type.

```pdef
message Containers {
    numbers     list<int32>;
    tweets      list<Tweet>;

    ids         set<int64>;
    colors      set<Color>;

    userNames   map<int64, string>;
    photos      map<string, Photo>;
}
```


#### Enums
Enum is a collection of unique predefined string values. Enums are also used as discriminator
values in polymorphic inheritance, see [polymorphic inheritance](#polymorphic-inheritance).

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

Unknown enum values are deserialized as nulls. In some languages enums are represented as
numbers (not as nullabe objects). In this case, a code generator must create a default
`UNDEFINED` enum value (and set it to `0` if possible).


#### Messages and exceptions
Message (an equivalent to a struct) is collection of strongly typed fields. Each field has a
unique name and a type. Messages support [simple](#inheritance)
and [polymorphic inheritance](#polymorphic-inheritance).
Messages marked as `exceptions` can be used in interfaces to declare thrown exceptions.

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
    friends     set<User>;
}

/** Example exception. */
exception UserNotFound {
    userId      int64;
}
```


#### Inheritance
Inheritances allow one message/exception to inherit the fields from another message/exception.
In simple inheritance subtypes cannot be deserialized from a base type,
see [polymorphic inheritance](#polymorphic-inheritance).

- Circular inheritance is forbidden.
- A message can have only one base.
- A message cannot override its base fields.
- Both a message and its base must be either exceptions or messages.
  One cannot inherit an exception from a message, or a message from an exception.
- A base must be declared before a subtype and cannot be imported from a dependent module
  (see [circular imports](#circular-imports)).

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


#### Polymorphic inheritance
Polymorphic inheritance provide subtype serialization/deserialization from a base type. A base
with all its subtypes form an inheritance tree. A subtype can also inherit another subtype to
form multi-level inheritance.

- Base and subtypes must be declared in the same package.
- A `@discriminator` field type must be of an `enum` type.
- A discriminator `enum` must be declared before the base `message` and cannot be imported from
 a dependent module (see [circular imports](#circular-imports)).
- There can be only one `@discriminator` field per inheritance tree.
- Each subtype must have a specified unique discriminator value.
- A non-polymorphic message cannot inherit a polymorphic message.
- All simple inheritance constraints.

To create a polymorphic message:

- Define an `enum` which will serve as a discriminator.
- Add a field of this enum type to a base message and mark it as a `@discriminator`.
- Specify discriminator values in subtypes as `message Subtype : Base(DiscriminatorEnum.VALUE)`.

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
    type   EventType @discriminator;    // The type field marked as @discriminator
    time   datetime;
}

/** Base user event. */
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


### Interfaces
An interface is a collection of strongly typed methods. Each method has a unique name,
a number of or zero arguments, and a result. The result can be a data type, `void` or an
interface.

A method is called a *terminal method* when it returns a data type or is `void`.
A method is called an *interface method* when it returns another interface.
Multiple chained methods form an *invocation chain*, i.e. `example.users().register("John Doe")`.

- Methods must have unique names.
- Method arguments must have unique names.
- An argument must be of a data type.
- The last method of an invocation chain must be terminal.
- Terminal methods can be marked as `@post` to to distinguish between mutators and accessors.

```pdef
interface Application {
    void0() void;                                                   // Void method.

    service(arg int32) Service;                                     // Interface method.

    method(arg0 int32, arg1 string, arg2 list<string>) string;      // Method with 3 args.
}

interface Service {
    query(limit int32, offset int32) list<string>;

    @post
    mutator(arg0 int32, arg1 string) string;
}
```


#### Interface inheritance
Interfaces can inherit other interfaces.

- Method overriding is forbidden.
- Method overloading is forbidden.
- A subinterface can have only one base.
- If a base specifies an exception then its subinterface exceptions must match it or be empty.

```pdef
interface BaseInterface {
    method() void;
}

interface SubInterface : BaseInterface {
    anotherMethod() void;
}
```


#### Interface Exceptions
Exceptions are specified at root interfaces via `@throws(Exception)`. The root interface
is the one which invocations start from. Exceptions from other interfaces in an invocation
chain are ignored. Usually, there is one root application interface (i.e. `Twitter` or `Github`)
and one application exception. Use polymorphic or composite exceptions when multiple exceptions 
are required for an application.

Polymorphic exceptions, when all exceptions subclass a base application exception.
```pdef
@throws(AppException)
interface Application {
    users() Users;
    photos() Photos;
    search() Search;
}

enum AppExceptionCode {
    AUTH_EXC,
    VALIDATION_EXC,
    FORBIDDEN_EXC
}

exception AppException {
    type AppExceptionCode @discriminator;
}
exception AuthExc : AppException(AppExceptionCode.AUTH_EXC) {}
exception ValidationExc : AppException(AppExceptionCode.VALIDATION_EXC) {}
exception ForbiddenExc : AppException(AppExceptionCode.FORBIDDEN_EXC) {}
```

A composite exception which wraps service exceptions; clients use the first non-null field
(this requires custom invocation logic, not implemented in the official clients/servers).
```pdef
@throws(AppException)
interface Application {
    users() Users;
    photos() Photos;
}

exception AppException {
    auth        AuthException;
    validation  ValidationExc;
    user        UserException;
    photo       PhotoException;
}
exception AuthException {}
exception ValidationException {}
exception UserException {}
exception PhotoException {}
```

Nulls and defaults
------------------
Non-present or null fields, arguments and collection elements are set to default values.

|type       |default value     |
|-----------|------------------|
|bool       | `false`          |
|numbers    | `0` or `0.0`     |
|string     | `null`           |
|datetime   | `null`           |
|enums      | `null`, `UNDEFINED` (a special enum value) or `0` |
|messages   | `null`           |
|containers | `null`           |

Messages can use bitfields to specify set fields.
```java
MyMessage message = new MyMessage()
    .setField0(1)
    .clearField1();

assert message.hasField0();
assert not message.hasField1();
```
