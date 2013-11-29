Pdef - Headers for the web
==========================
Pdef is a statically typed interface definition language with clear separation between data
structures and interfaces, support for object-oriented APIs and message inheritance with JSON as
the default format and a simple HTTP RPC.

Pdef (pi:def) stands for "protocol definition [language]". It allows to write interfaces and
data structures once and then to generate code and RPC clients/servers for different languages.
It is suitable for public APIs, internal service-oriented APIs, configuration files,
as a format for persistence, cache, message queues, logs, etc.

Links
-----
- [Language guide](docs/language-guide.md)
- [Style guide](docs/style-guide.md)
- [JSON format](docs/json-format.md)
- [HTTP RPC](docs/http-rpc.md)
- [Grammar in BNF](docs/grammar.bnf)
- [Generated and language specific code](docs/generated-lang-specific-code.md)
- [How to write a code generator](https://github.com/pdef/pdef-generator-template)

Features
--------
- Clear separation between data structures and interfaces.
- Interfaces, not services, to allows Object-Oriented APIs.
- Simple data type system.
- Message inheritance.
- Packages and modules with imports.
- Circular module imports (with some limitations).
- Circular references in message and interface definitions
  (with some limitations to support interpreted languages).
- Loosely-coupled format and RPC implementations, with JSON and a simple HTTP RPC as the defaults.
- Pluggable code generators.

Code generators
---------------
- [Java](https://github.com/pdef/pdef-java)
- [Python](https://github.com/pdef/pdef-python)
- [Objective-C](https://github.com/pdef/pdef-objc)

Installation
------------
Pdef consists of a compiler, pluggable code generators, and language-specific bindings.
Install the compiler as a python package:
```bash
pip install pdef-compiler
# or
easy_install pdef-compiler
```

Or [download](https://github.com/pdef/pdef/releases) the archive, unzip it and run:
```bash
python setup.py install
```

Install the code generators:
```bash
pip install pdef-java
pip install pdef-python
pip install pdef-objc
```

Check the test package (no source code is generated):
```bash
pdefc check https://raw.github.com/pdef/pdef/master/example/world.yaml
```

List the installed generators:
```bash
pdefc generate -h
```

Generate Python code:
```bash
pdefc generate https://raw.github.com/pdef/pdef/master/example/world.yaml \
    --generator python
    --out generated
```

Generate Java code:
```bash
pdefc generate https://raw.github.com/pdef/pdef/master/example/world.yaml \
    --generator java
    --ns pdef_test:io.pdef
    --out target/generated-sources
```

Generate Objective-C code:
```bash
pdefc -v generate https://raw.github.com/pdef/pdef/master/example/world.yaml \
    --generator objc \
    --out GeneratedClasses
```

Example
-------
See the full [example package](https://github.com/pdef/pdef/tree/master/example).
```pdef
/**
 * Example world.
 */
from world import continents, space;    // Import two modules from a package.


/**
 * The world interface.
 * A god-like person can use it to rule the world.
 */
interface World {
    /** Returns the humans interface. */
    humans() Humans;                    // Returns another interface.

    /** Returns the continents interface. */
    continents() continents.Continents; // Returns an interface from another module.

    /** Switches the light. */
    switchDayNight() void;

    /** Returns the last world events, the events are polymorphic. */
    events(limit int32 @query, offset int64 @query) list<Event>;
}


interface Humans {
    /** Finds a human by id. */
    find(id int64) Human;

    /** Lists all people. */
    all(  // A method with query arguments.
        limit int32 @query,
        offset int32 @query) list<Human>;

    /** Creates a human. */
    @post  // A post method (a mutator).
    create(
        name string @post,
        sex Sex @post) Human;
}


message Thing {                     // A simple message definition.
    id          int64;              // an id field of the int64 type.
    location    space.Location;
}


/** Human is a primate of the family Hominidae, and the only extant species of the genus Homo. */
message Human : Thing {             // A message with a base message and a docstring.
    name        string;
    birthday    datetime;
    sex         Sex;
    continent   continents.Continent;
}

enum Sex {
    MALE, FEMALE, UNCLEAR;
}

// An enumeration.
enum EventType {
    HUMAN_EVENT,
    HUMAN_CREATED,
    HUMAN_DIED;
}


// A polymorphic message with EventType as its discriminator.
message Event {
    type    EventType @discriminator;
    id      int32;
    time    datetime;
}


// A polymorphic subtype.
message HumanEvent : Event(EventType.HUMAN_EVENT) {
    human   Human;
}


// Multi-level polymorphic messages.
message HumanCreated : HumanEvent(EventType.HUMAN_CREATED) {}
message HumanDied : HumanEvent(EventType.HUMAN_DIED) {}
```

Java
----
JSON:
```java
// Read a human from a JSON string or stream.
Human human = Human.fromJson(jsonString);
human.setContinent(ContinentName.NORTH_AMERICA);

// Serialize a human to a JSON string.
String json = human.toJson();
```

Client:
```java
// Create an HTTP RPC client.
RpcClient<World> client = new RpcClient<World>(World.DESCRIPTOR, "http://example.com/world/");
World world = client.proxy();

// Create a man.
Human man = world.humans().create(new Human()
        .setId(1)
        .setName("Man")
        .setSex(Sex.MALE)
        .setContinent(ContinentName.ASIA));

// Switch day/night.
world.switchDayNight();
```

Server:
```java
World world = getMyWorldImplementation();
RpcHandler<World> handler = new RpcHandler<World>(World.DESCRIPTOR, world);
RpcServlet<World> servlet = new RpcServlet<World>(handler);

// Pass it to your servlet container,
// or wrap in another servlet as a delegate.
```

License and Copyright
---------------------
Copyright: 2013 Ivan Korobkov <ivan.korobkov@gmail.com>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at:

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
