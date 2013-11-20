Pdef - Headers for the web
==========================
Pdef is an interface definition language with clear separation between data structures and
interfaces, support for object-oriented APIs and message inheritance with JSON as the default
format and a simple HTTP RPC. Pdef has been influenced by Google Protobuf and Apache Thrift.

Pdef (pi:def) stands for "protocol definition [language]". It is suitable for public
client/server APIs, internal service-oriented APIs, configuration files,
as a format for persistence, cache, message queues, logs, etc.

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

See the LICENSE.txt for a copy of the License.
