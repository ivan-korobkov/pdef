Pdef Language Guide
===================
Pdef is a statically typed interface definition language. It allows to write interfaces and
data structures once and then to generate code and rpc clients/servers for different languages
(Java, Python, Objective-C).


Contents
--------
- [Syntax](#syntax)
    - [Comments](#comments)
- [Packages](#packages)
- [Modules](#modules)
    - [Imports](#imports)
    - [Circular imports and references](#circular-imports-and-references)
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
    - [Interface exceptions](#interface-exceptions)


Syntax
------
Pdef syntax is similar to Java/C++ with the inverted type/identifier order in fields and arguments.
The first is the identifier the second is the type. All identifiers must start with a latin
letter and contain only latin letters, digits and underscores.
See the [grammar](grammar.bnf) for the specification.

Syntax example:
```pdef
from world import continents, space;    // Import two modules from a package.

/**
 * The world interface.
 * A god-like person can use it to rule the world.
 */
interface World {
    /** Switches the light. */
    switchDayNight() void;

    /** Creates a new human. */
    @post
    createHuman(name string, sex Sex) Human;
}

/** Human is a primate of the family Hominidae. */
message Human  {
    id          int64;
    name        string;
    birthday    datetime;
    sex         Sex;
}

enum Sex {
    MALE,
    FEMALE;
}
```


### Comments
There are two types of comments: single-lines and multi-line docstrings.
Docstrings can be placed at the beginning of a file, before a definition (enum, message,
interface) or before a method.

```pdef
/**
 * This is a multi-line module docstring.
 * It is a available to the code generators.
 */

// This is a one line comment, it is stripped from the source code.

/** Interface docstring. */
interface ExampleInterface {
    /** Method docstring. */
    hello(name string) string;
}

/** Message docstring. */
message ExampleMessage {
    id  int32;
}
```


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
  version: 1.0-dev
  url: https://github.com/pdef/pdef/
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

*Constraints:*

- Package and module names must start with a latin letter and contain only latin letters,
  digits and underscores.
- Package names must be unique.
- Module names must be unique in a package.
- Circular package dependencies are forbidden.

One file as a package definition with enumerated modules is convenient,
you can pass it as a URL directly to the compiler.
```bash
# Downloads and validates the test pdef package.
# Does not generate any code.
pdefc check https://raw.github.com/pdef/pdef/master/test/test.yaml
```


Modules
-------
Module is a `.pdef` file with **definitions** (enums, messages and interfaces).
Module names are automatically mapped to file names:
the dot is replaced with a path separator `/` plus a `.pdef` extension.
So, `users.events` module is mapped to a `./users/events.pdef` file.

A **absolute module name** is a concatenation of a package name with a module name.
The absolute name of `users.events` from the previous example is `example.users.events`.
A module can import other modules by their absolute names.

When a *module name matches its package name*, the absolute module name is the package name.
`example` is an absolute name of the `example` module, **not** `example.example`.

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


### Imports
A module can import other modules to access their definitions. Imports must be specified
at the top of a file before definitions. Modules can only be imported by their absolute names,
even inside the same package. There are two ways to import modules.

The first one is use an absolute module name.
```
import package.module.name;

message NeedMessageFromAnotherModule {
    field   package.module.name.MyMessage;
}
```

The second is to import submodules from a package or a module.
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


### Circular imports and references
Circular imports are allowed. However, there are some constraints to support scripting languages.
Interpreted languages require a serial execution order for inheritance.

Two modules can import each other as long as the messages from the first one *do not inherit* the
messages from the second one and vice versa
(see [polymorphic inheritance](#polymorphic-inheritance)). Usually, this case is very rare,
and it almost never affects development. However, if you encounter it,
break one module into multiple ones, or merge two modules into one.

Circular references are allowed, even self-references are allowed. They can be implemented
in almost all languages via forward/lazy referencing.
See [circular references](./generated-lang-specific-code.md#circular-references)
in the [generated code guide](./generated-lang-specific-code.md).

Circular import and reference example:

`users.pdef`
```
from example import photos;

message User {
    bestFriend  User;           // References itself.
    photo       photos.Photo;   // References a photo from another module.
}

```

`photos.pdef`
```
from example import users;

message Photo {
    user    users.User;         // References a user from another module.
}
```


Types
-----
Pdef has a simple, yet powerful type system. It is built on a clear separation between
data types and interfaces. In Pdef data types represent serializable values,
and interfaces provide behaviour.

### Void
`void` is a special type which indicates that a method returns no result.

### Data types
All data types are *nullable*. It is up to a format how to represent null values,
for example, in JSON a field can be `null` or absent.

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

- `list` is an ordered list of elements. An element must be a **data type**.
- `set` is an unordered set of unique elements. An element must be a **data type**.
- `map` is an unordered key-value container (a dict in some languages). A key must be a
**non-null primitive**, a value must be a **data type**.

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

**Unknown enum values are deserialized as nulls**. In some languages enums are represented as
numbers (not as nullabe objects). In this case, a code generator must create a default
`UNDEFINED` enum value (and set it to `0` if possible).


#### Messages and exceptions
Message (an equivalent to a struct) is collection of strongly typed fields. Each field has a
unique name and a type. Messages support [simple](#inheritance)
and [polymorphic inheritance](#polymorphic-inheritance).
Messages marked as `exceptions` can be used in interfaces to declare thrown exceptions.

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


#### Inheritance
Inheritances allow one message/exception to inherit the fields from another message/exception.
However, in simple inheritance there is no polymorphic serialization/deserialization:
subtypes cannot be deserialized from a base type,
see [polymorphic inheritance](#polymorphic-inheritance).

*Constraints:*

- Circular inheritance is forbidden.
- A message can have only one base.
- A message cannot override its base fields.
- Both a message and its base must be either exceptions or messages.
  One cannot inherit an exception from a message, or a message from an exception.
- A base must be declared before a subtype and cannot be imported from a dependent module
  (see [circular imports and references](#circular-imports-and-references)).

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
Polymorphic inheritance allow to polymorphic serialization/deserialization of subtypes
from a base type. A base with all its subtypes form an *inheritance tree*. A subtype can also
inherit another subtype to form *multi-level inheritance*. To create a polymorphic message:

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
  (see [circular imports and references](#circular-imports-and-references)).
- A non-polymorphic message cannot inherit a polymorphic message.
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

A method is called a **terminal method** when it returns a data type or is `void`.
A method is called an **interface method** when it returns another interface.
Multiple chained methods form an **invocation chain**, i.e. `example.users().register("John Doe")`.

Terminal methods can be marked as `@post` to distinguish between mutators and
accessors. HTTP RPC sends these methods as POST requests. Non-post terminal methods can have
`@query` arguments sent as an HTTP URL query. `@post` methods can have `@post` arguments
sent as an HTTP POST form.

*Constraints:*

- Interface methods must have unique names.
- Method arguments must have unique names.
- An argument must be of a data type.
- Only terminal methods can be `@post`.
- Only `@post` methods can have `@post` arguments.
- Only non-post terminal methods can have `@query` arguments
  (i.e. `@post` and `@query` arguments can't be mixed in one method).
- Interfaces do not support inheritance (at least now).
- The last method of an invocation chain must be a terminal one.

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


### Interface exceptions
There can be only one exception per application specified at its root interface
via `@throws(Exception)`. It is impossible to specify different exceptions for different methods
or child interfaces. All child interface exceptions are ignored.

One exception per application is a simple and an unambiguous way to deal with exception.
However, applications usually use a lot of different exceptions. There are two possible
ways to implement them.

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
