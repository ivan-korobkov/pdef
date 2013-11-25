Pdef Language Guide
===================
Pdef is a statically typed interface definition language. It allows to write interfaces and
data structures once and then to code for different languages (Java, Python,
Objective-C). Pdef has a simple type system and a package/module organization.


Contents
--------
- Types overview
- Data types:
    - Primitives
    - Containers
    - Enums
    - Messages and exceptions
        - Simple inheritance
        - Polymorphic inheritance
- Interfaces
- Comments
- Imports


Example
-------


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

<h3>Containers</h3>
Containers are generic strongly typed containers.

- `list` is an ordered list of elements. An element must be a **data type**.
- `set` is an unordered set of unique elements. An element must be a **data type**.
- `map` is an unordered key-value container (a dict in some languages). A key must be a
**primitive**, a value must be a **data type**.

Example:
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


<h3>Messages and exceptions</h3>
Message (an equivalent to a struct) is collection of strongly typed fields. Each field has a
unique name and a type. Messages support [simple] and [polymorphic inheritance].
Exceptions are special messages which can be used in interfaces to declare raised exceptions.

*Constraints:*

- All message fields must have unique names.
- A field type must be a data type, message fields can references other messages.
- A field can have a message it is declared in as its type (see the `friends` field below).

Example:
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


<h4>Simple inheritance</h4>
Simple inheritances allow one message to inherit the fields of another message. However,
in simple inheritance there is no polymorphic serialization/deserialization. In other words,
subtypes cannot be deserialized from a base type.

*Constraints:*

- A message can have only one base.
- Circular inheritance is forbidden.
- A message cannot override its base fields.
- Both a message and its base must be either exceptions or messages.
  One cannot inherit an exception from a message, or a message from an exception.
- A message must be declared after its base and cannot be imported from a dependent module
  (see [dependent modules]).

Example:
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
 subtype to form multi-level inheritance. To create a polymorphic message:

- Define an `enum` which will serve as a discriminator.
- Add a field of this enum type to a base message and mark it as a `@discriminator`.
- Specify discriminator values in subtypes as `message Subtype : Base(DiscriminatorEnum.VALUE)`.

*Constraints:*

- A `@discriminator` field type must be an `enum`.
- A discriminator `enum` must be declared before the base `message` and cannot be imported from
 a dependent module (see [dependent modules]).
- There can be only one `@discriminator` field per inheritance tree.
- Each subtype must have a specified unique discriminator value.
- A base must be declared before subtypes and cannot be imported from a dependent module
  (see [depdendent modules]).
- A message cannot inherit another polymorphic message (a base or a subtype)
  without specifying a discriminator value. In other words, simple inheritance is forbidden
  with polymorphic messages.
- All simple inheritance constraints.

Example:
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

Circular References and Imports
-------------------------------
Can reference any type, or definition. Only inheritance impose constraints on declaration order
and imports. Circular references and imports are allowed.
