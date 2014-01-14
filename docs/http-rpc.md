Pdef HTTP RPC specification
===========================
Pdef HTTP RPC specifies how to map method invocations and invocation chains to HTTP requests.
The specification is not pdef-specific. It describes how to map any interface invocation
chains to HTTP requests. It is not RESTfult, because REST is more complex and ambiguous and
implementing will require more language features such as annotations/attributes in Pdef.

Please, read the [Language Specification] and [JSON Format] before proceeding.
See [Implementations] for the real implementations.


Contents
========
- [Specification](#specification)
- [Pseudo-code](#Pseudo-code)
- [Examples](#examples)


Specification
=============

HTTP Request
------------
Method invocation chain must be sent as an HTTP request with the `application/x-www-form-urlencoded`
content type.

Method names and path arguments (not `@query` or `@post`) must be appended to the request path,
null path arguments are forbidden.
```http
GET /method/argument0/nextMethod/1234 HTTP/1.1
```

`@post` and `@query` arguments must be added to request post params and query params
respectively with argument names as keys; null arguments must be skipped.
```http
GET /method/arg0/nextMethod?arg=hello+world HTTP/1.1
```

Arguments must be serialized into JSON UTF-8 strings, with quotes stripped, and then url-encoded.
Example `{"firstName": "John", "lastName:" "Doe"}` user argument.
```http
GET /methodWithQueryArg?user={"firstName"%3A+"John",+"lastName%3A"+"Doe"} HTTP/1.1
```

```http
POST /methodWithPostArgs HTTP/1.1
Content-Type: application/x-www-form-urlencoded

user={"firstName"%3A+"John",+"lastName%3A"+"Doe"}
```

HTTP Response
-------------
RPC errors must be returned as `HTTP 400 Bad request` responses
with the `text/plain; charset=utf-8` content type.
```http
HTTP/1.0 400 Bad request
Content-Type: text/plain;charset=utf-8

Wrong method arguments.
```

Successful results must be returned as `HTTP 200 OK` responses with
the `application/json; charset=utf-8` content type; the result must be wrapped into a JSON
object with the `data` field.
```http
HTTP/1.0 200 OK
Content-Type: application/json;charset=utf-8

{
  "data": "method result"
}
```

Application exceptions (specified in the application interface via `@throws`)
must be returned as an `HTTP 422 Uprocessable entity` responses with
the `application/json; charset=utf-8` content type; the exception must be wrapped into
a JSON object with the `error` field.
```http
HTTP/1.0 422 Unprocessable entity
Content-Type: application/json;charset=utf-8

{
  "error": {
      "text": "Expected application exception"
  }
}
```

Pseudo-code
===================
Client
------
```
Given an invocation chain and an application interface (the root interface).
Assert that the last method in an invocation chain is terminal
(its result is a data type or it is void).

If the last method in an invocation chain is @post:
    Set the request HTTP method to POST

For each method in an invocation chain:
    Append '/' to the request path;
    Append a method name to the request path;

    For each method argument:
        If the argument is null:
            If the argument is @query or @post:
                Continue.
            Throw an exception, path arguments cannot be null.

        Convert the argument into a JSON string, strip the quotes and url-encode it.
        If the argument is @post:
            Add the string to the POST params with the arg name as its key.
        Else if the argument is @query:
            Add the string to the query string with the arg name as its key.
        Else:
            Append '/' to the request path.
            Append the string to the request path.


Send the HTTP request.
Receive an HTTP response.
Get the last method result type and the application exception type.


If the response status is 200 OK:
    Parse the expected result from the `data` field of a JSON object.
    Return the result.
Else if the response status is 422 Unprocessable entity:
    If there is no expected application exception:
        Raise an exception 'Unknown application exception'
    Else:
        Parse the expected application exception from the `error` field of a JSON object.
        Raise the application exception.
Else:
    Raise an HTTP error.
```

Server
------
```
Given an HTTP request and an application interface (the root interface).

# Parse the invocation.
Strip '/' from the HTTP request path (here the request path is not a full URL path,
but an application specific path, such as CGI PATH_INFO, etc).
Split the request path on '/' into a list of parts.

Create an empty invocation chain.
For each segment in path delimited by '/':
    Pop (remove and return) the first segment as a method name.
    Find a method in the interface by its name.
    If the method is not found:
        Return HTTP 400, 'Method is not found'.

    If the method is @post:
        Assert that HTTP request method is POST.
        Otherwise return HTTP 405, 'HTTP method not allowed, POST required'.

    Create an empty list for method arguments.
    For each method argument:
        If the argument is @post:
            Get its value from the request post data.
        Else if the argument is @query:
            Get its value from the request query string.
        Else:
            If the remaining path segments are empty:
                Return HTTP 400, 'Wrong number of method arguments'.
            Pop (remove and return) the next value from the remaining segments.

        If the value is not present:
            Add null to the arguments list.

        Else:
            Url-decode the value.

            If the expected argument type is a string, an enum or a date type,
            and the value does not start and end with quotes ("), enclose it
            in quotes to get a valid JSON string.

            Parse the value as a JSON string and add it to the arguments list.

    Create a new invocation of the method with the parsed argument values.
    Add it to the invocation chain.

    If the method is terminal (returns a data type or is void):
        Assert that the remaining path segments are empty.
        Otherwise return HTTP 400, 'Wrong invocation chain'.
    Else:
        The method is not terminal, so it must return an interface.
        Set the interface to the method result and continue.


All path segments are consumed.
If the invocation chain is empty:
    return HTTP 400, 'Methods required'

If the last method in an invocation chain is not terminal:
    return HTTP 400, 'The last method must be terminal. It must return a data type or be void.'


# Invoke the invocation and return the result.
Invoke the invocation chain on your objects and get the result.

If the result is successful:
    Create a JSON object with the `data` field set to the result.
    Serialize the JSON object into a UTF-8 string.
    Send it as HTTP 200 OK response with 'application/json;charset:utf-8' content type.
Else if the result is an application exception specified in the application interface:
    Create a JSON object with the `error` field set to the exception.
    Serialize the JSON object into a UTF-8 string.
    Send it as HTTP 422 Unprocessable entity response with 'application/json;charset-utf-8'
    content type.
Else:
    Return HTTP specific error responses or HTTP 500 Internal server error
```


Examples
========
Interfaces and data structures used in the examples.
```pdef
/** Root world interface. */
@throws(WorldException)
interface World {
    people() People;
}

interface People {
    /** Login people by username/password, and return them.*/
    @post
    login(username string @post, password string @post) Person;

    /** Find people, return a list of people. */
    find(query string @query, limit int32 @query, offset int32 @query) list<Person>;
}

message Person {
    id      int64;
    name    string;
}

enum WorldExceptionCode {
    AUTH_EXCEPTION, INVALID_DATA;
}

exception WorldException {
    type    WorldExceptionCode @discriminator;
    text    string;
}

exception AuthException : WorldException(WorldExceptionCode.AUTH_EXCEPTION) {}
exception InvalidDataException : WorldException(WorldExceptionCode.INVALID_DATA) {}
```

<h3>Login a person</h3>
`worldClient.people().login("john.doe", "secret");`

Send a POST HTTP request, because the method is marked as `@post`.
Append `username` and `password` to the post data because they are marked as `@post` arguments.
```http
POST /people/login HTTP/1.0
Host: example.com
Content-Type: application/x-www-form-urlencoded

username=john.doe&password=secret
```

Successful response:
```http
HTTP/1.0 200 OK

{
    "data: {
        "id": 10,
        "name": "John Doe"
    }
}
```

Application exception response:
```http
HTTP/1.0 422 Unprocessable entity

{
    "error": {
        "type": "auth_exception",
        "text": "Wrong username or password"
    }
}
```


<h3>Find people</h3>
`worldClient.people().find("John Doe", limit=10, offset=100)`

Send a GET HTTP request, add the last method arguments to the query string because they are
marked as `@query`.
```http
GET /people/find?query=John+Doe&limit=10&offset=100 HTTP/1.0
```

Successful response:
```http
HTTP/1.0 200 OK

{
    "data": [
        {
            "id": 10,
            "name": "John Doe"
        },
        {
            "id": 22,
            "name": "Another John Doe"
        }
    ]
}
```

Application exception response:
```http
HTTP/1.0 422 Unprocessable entity

{
    "error": {
        "type": "invalid_data",
        "text": "The world does not like your query"
    }
}
```
