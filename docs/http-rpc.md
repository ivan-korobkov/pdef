Pdef HTTP RPC specification
===========================
Pdef HTTP RPC specifies how to map method invocations and invocation chains to HTTP requests.
The specification is not pdef-specific. It describes how to map any interface invocation
chains to HTTP requests. It is not RESTfult, because REST is more complex and ambiguous and
implementing will require more language features such as annotations/attributes in Pdef.

Please, read the [Language Specification] and [JSON Format] before proceeding.
See [Implementations] for the real implementations.

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
    login(username string @query, password string @post) Person;

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
Append the `username` to the query string and the `password` to the post data
because they are marked as `@query` and `@post` respectively.
```
POST /people/login?username=john.doe HTTP/1.0
Host: example.com
Content-Type: application/x-www-form-urlencoded
password=secret
```

Successful response:
```
HTTP/1.0 200 OK
{
    "data: {
        "id": 10,
        "name": "John Doe"
    }
}
```

Application exception response:
```
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
```
GET /people/find?query=John+Doe&limit=10&offset=100 HTTP1.0
```

Successful response:
```
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
```
HTTP/1.0 422 Unprocessable entity
{
    "error": {
        "type": "invalid_data",
        "text": "The world does not like your query"
    }
}
```


Specification
=============

HTTP Request
------------
Method invocation must be sent as an HTTP request. The content type must be set to
`application/x-www-form-urlencoded`. All arguments must be serialized into JSON UTF-8 strings,
with quotes stripped from the strings, and then url-encoded.

HTTP Response
-------------
Responses must be returned as `application/json;charset=utf-8` strings. Successful invocation
results must be returned as `HTTP 200 OK` responses. Application exceptions (specified in
the application interface via `@throws`) must be returned as `HTTP 422 Unprocessable entity`
responses. The response body must be a json object with `data` and `error` fields.
`data` is filled with successful method results, `error` is filled with application exceptions.
```json
{
    "data": "Successful method result",
    "error": "Application exception"
}
```

Sending an invocation
---------------------
```
Given an invocation chain and an application interface (the root interface).
Assert that the last method in an invocation chain is terminal
(its result is a data type or it is void).

If the last method in an invocation chain is @post:
    Set the request HTTP method to POST

For each method in an invocation chain:
    Append '/' to the request path;
    Append a method name to the request path;

    For each argument in a method:
        Convert it into a JSON string, strip the quotes.
        If the argument is @post:
            Url-encode it and add it ot the request post data with the argument name as a key.
        Else if the argument is @query:
            Url-encode it and add it to the request query string with the argument name as a key.
        Else:
            Append '/' to the request path;
            Url-encode the argument and append it to the request path;


Send the HTTP request.
Receive an HTTP response.
Get the last method result type and the application exception type.


If the response status is 200 OK:
    Parse the expected result from the `data` field of a JSON object.
    Return the result.
Else if the response status is 422 Unprocessable entity:
    If there is no expected application type:
        Raise an exception 'Unknown application exception'
    Else:
        Parse the expected application exception from the `error` field of a JSON object.
        Raise the application exception (or return it to the user other ways).
Else:
    Raise an HTTP error.
```

Handling an invocation
----------------------
```
Given an HTTP request and an application interface (the root interface).

# Parse the invocation.
Strip '/' from the HTTP request path (here the request path is not a full URL path,
but an application specific path, such as CGI PATH_INFO, etc).
Split the request path on '/' into a list of parts.

Create an empty invocation chain.
For each segment in path delimited by '/':
    Find a method in an interface by the segment.
    If the method is not found:
        Return HTTP 404, 'Method is not found'.

    If the method is @post:
        Assert that HTTP request method is POST.
        Otherwise return HTTP 405, 'HTTP method not allowed, POST required'.

    Create an empty list for method arguments.
    For each method argument:
        If the argument is @post:
            Get it from the request post data.
        Else if the argument is @query:
            Get it from the request query string.
        Else:
            If the remaining path segments are empty:
                Return HTTP 404, 'Method not found, wrong number of arguments'.
            Get the next segment from the remaining segments as an argument.

        Url-decode the argument. If the expected argument type is a string,
        enclose it in quotes to get a valid JSON string.
        Parse the argument from a JSON string and add it to the arguments lists.

    Create a new invocation of the method with the parsed arguments.
    Add it to the invocation chain.

    If the method is terminal (returns a data type or is void):
        Assert that the remaining path segments are empty.
        Otherwise return HTTP 404, 'Wrong invocation chain'.
    Else:
        The method is not terminal, so it must return an interface.
        Set the interface to the method result and continue.


All path segments are consumed.
If the invocation chain is empty:
    return HTTP 404, 'Methods required'

If the last method in an invocation chain is not terminal:
    return HTTP 404, 'The last method must be terminal. It must return a data type or be void.'


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
