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
- [Code generators](docs/code-generators.md)
- [JSON format](docs/json-format.md)
- [HTTP RPC](docs/http-rpc.md)
- [Grammar BNF](docs/grammar.bnf)
- [Generated and language specific code](docs/generated-lang-specific-code.md)
- [How to write a code generator](docs/how-to-write-code-generator.md)

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
pdefc check https://raw.github.com/pdef/pdef/master/test/test.yaml
```

List the installed generators
```bash
pdefc generate -h
```

Generate Python code:
```bash
pdefc generate https://raw.github.com/pdef/pdef/master/test/test.yaml \
    --generator python
    --out generated
```

Generate Java code:
```bash
pdefc generate https://raw.github.com/pdef/pdef/master/test/test.yaml \
    --generator java
    --ns pdef_test:io.pdef
    --out target/generated-sources
```

Generate Objective-C code:
```bash
pdefc -v generate https://raw.github.com/pdef/pdef/master/test/test.yaml \
    --generator objc \
    --out GeneratedClasses
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
