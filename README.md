Pdef
====
Pdef is an interface definition language with clear separation between data structures and
interfaces, support for object-oriented APIs and inheritance with JSON and REST(ish) as a default
format and an RPC implementation. Pdef has been influenced by Google Protobuf and Apache Thrift.
The language is statically typed. JSON format and REST RPC are weakly typed.

Pdef is suitable for public APIs, internal service-oriented APIs, config files, a format for cache,
message queues, logs, etc. Pdef is in general about interfaces and data structures.


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

Structure:
  - compiler:   Compiler and java/python code generators.
  - java:       Java bindings and a REST RPC implementation.
  - python:     Python bindings and a REST RPC implementation.
  - test:       Test pdef files.
