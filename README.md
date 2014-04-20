Pdef - API language with code-generation
========================================
Pdef (pi:def, stands for "protocol definition [language]") is an interface definition language
with optional code-generation for HTTP clients/servers and JSON data structures.
It is suitable for public APIs, internal service-oriented APIs, configuration files, 
as a format for persistence, cache, message queues, logs, etc.


Contents
--------
- [Syntax](#syntax)
- [JSON encoding](#json-encoding)
- [HTTP RPC](#http-rpc)
- [License and Copyright](#license)


Syntax
------
Pdef syntax is similar to C/C++/Java with the inverted type/identifier order in fields and 
arguments. All identifiers must start with a latin letter and contain only latin letters,
digits and underscores. See the [grammar specification](grammar.bnf).

There are two types of comments: single-line comments and multi-line docstrings.
Docstrings can be placed at the beginning of a file, before a definition (enum, struct,
interface) or before a method.

```pdef
/**
 * This is a multi-line file docstring.
 *
 * Start each line with a star because 
 * it is used as an indentation margin.
 */
package example;

// This is a one line comment, it is stripped from the source code.

/** Example interface. */
interface Blog {
    /** Returns an article by its id. */
    GET getArticle(id int64) Article;
    
    /** Adds a new comment to the blog. */
    POST comment(text string) string;
}


/** Example struct. */
struct Article {
    id          int64;
    title       string;
    createdAt   datetime;
}
```


### Packages
Pdef uses packages to organize files. Each package must be put into a distinct directory
named after the package. Each file must declare its package. Example package structure:
```
company.blog/
    blog.pdef
    articles.pdef
    comments.pdef
    users.pdef
    users/profile.pdef
```


### Primitive types
- `bool`: a boolean value (true/false),
- `int16`: a signed 16-bit integer,
- `int32`: a signed 32-bit integer,
- `int64`: a signed 64-bit integer,
- `float`: a 32-bit floating point number,
- `double`: a 64-bit floating point number,
- `string`: a unicode string,
- `datetime`: a date and time object without a time zone,
- `void` is a special type which indicates that a method returns no result.


### Containers
- `list` is an ordered list of elements. An element must be a data type.
- `set` is an unordered set of unique elements. An element must be a data type.
- `map` is an unordered key-value container (a dict in some languages). A key must be a
non-null primitive, a value must be a data type.

```pdef
struct User {
    id          int64;
    name        string;
    
    // Example containers
    emails      set<string>;
    friends     list<User>;
    aliases     map<string, string>;
}
```


### Enums
Enum is a collection of unique predefined string values. Code generators can add
`UNDEFINED` enum values if not-present and required.

```pdef
enum Sex {
    MALE, FEMALE;
}
```


### Structs and exceptions
Struct is collection of strongly typed fields. Each field has a unique name and a type.
Inheritance is not supported. Structs can be declared as `exceptions`
so that code-generators can use native language exceptions.

```pdef
/** Example struct. */
struct User {
    id          int64;
    name        string;
    age         int32;
    profile     Profile;
    friends     set<User>;
}

/** Example exception. */
exception ApplicationException {
    code        int32;
    message     string;
}
```


### Interfaces
Interface is a collection of strongly typed methods. Each method has a unique name,
a number of or zero data type arguments, and a result. The result can be of any type.
Methods are declared as getters or mutators, using `GET` and `POST` keywords respectively.

If a method returns an interface then it must be declared as a `GET` method.
Interface methods are used in *invocation chains*, i.e. `blog(17).articles().comment("Hello")`. 
The last method in an invocation chain must return a data type.

Arguments can be declared either 
as name-type pairs (`query(limit int32, offset int32) list<string>`),
or as requests (`create(CreateArticleRequest) CreateArticleResponse`).

```pdef
interface Blog {
    /** Returns a blog title. */
    GET title() string;
    
    /** Returns a blog articles interface. */
    GET articles() Articles;
}

interface Articles {
    /** Queries blog articles. */
    GET query(limit int32, offset int32) list<Article>;

    /** Creates a new article and returns it. */
    POST create(Article) Article;
    
    /** Updates an article and returns it. */
    POST update(Article) Article;
    
    /** Deletes an article by its id. */
    POST delete(id int64) void;
}
```


JSON encoding
-------------
Pdef data types transparently map to JSON data types. Dates are encoded as
as ISO8601 UTC `yyyy-MM-ddTHH:mm:ssZ` strings, enums are encoded as lowercase strings.

Pdef:
```
struct User {
    id          int64;
    sex         Sex;
    name        string;
    signedUpAt  datetime;
    friends     list<User>;
}
```

JSON:
```json
{
    "id": 1234,
    "sex": "male",
    "name": "John Doe",
    "signedUpAt": "2014-04-20T23:59:59Z"
    "friends": [
        {"id": 1235, "name": "Jane"},
        {"id": 10, "name": "Albert"}
    ]
}
```


HTTP RPC
--------
### Request
Pdef invocation chains are sent as HTTP `application/x-www-form-urlencoded` requests.
Method names are appended to request paths. Arguments are appended to paths when a method returns
an interface, otherwise, they and are sent as HTTP query or post data. Primitive arguments are 
converted to strings, containers and structs are converted to JSON. Unnamed request arguments as 
in `POST create(CreateArticleRequest) CreateArticleResponse` are expanded into fields, 
i.e. their fields are sent as normal arguments.

GET `blog(10).articles().query(limit=10, offset=20) // pseudo-code`
```http
GET /blog/10/articles/query?limit=10&offset=20 HTTP/1.1
```

POST `blog(10).articles().create(CreateArticleRequest(title="Hello world", date=now)) //pseudo-code`
```http
POST /blog/10/articles/create HTTP/1.1
Content-Type: application/x-www-form-urlencoded

title=Hello+world&date=2014-04-14T23:59:59Z
```

### Response
Successful result are sent as `{"data": "method result"}` JSON responses:
```http
HTTP/1.0 200 OK
Content-Type: application/json;charset=utf-8

{
  "data": {
    "id": 1234,
    "title": "Hello, world",
    "createdAt": "2014-04-20T23:59:59Z"
  }
}
```

Application exceptions are sent as `{"error": "exception struct"}` JSON responses
with HTTP error codes:
```http
HTTP/1.0 422 Unprocessable entity
Content-Type: application/json;charset=utf-8

{
  "error": {
      "text": "Expected application exception"
  }
}
```

RPC errors are sent as plain text `400 Bad request` responses:
```http
HTTP/1.0 400 Bad request
Content-Type: text/plain;charset=utf-8

Wrong method arguments.
```


License and Copyright
---------------------
Copyright: 2013-2014 Ivan Korobkov <ivan.korobkov@gmail.com>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at:

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
