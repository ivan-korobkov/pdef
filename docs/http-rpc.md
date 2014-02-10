Pdef HTTP RPC specification
===========================
Pdef HTTP RPC specifies how to map method invocations and invocation chains to HTTP requests.
The specification is not pdef-specific. It describes how to map any interface invocation
chains to HTTP requests. It is not RESTfult, because REST is more complex and ambiguous and
implementing will require more language features such as annotations/attributes in Pdef.

Request
-------
Method invocation chain must be sent as an HTTP request with the `application/x-www-form-urlencoded`
content type.

Method names and interface method arguments must be appended to the request path,
null interface method arguments are forbidden.
```http
GET /interfaceMethod/arg0/arg1/terminalMethod HTTP/1.1
```

Terminal method arguments must be sent as HTTP query params, null arguments should be skipped.
```http
GET /interfaceMethod/arg0/terminalMethod?arg0=hello&arg1=world HTTP/1.1
```

Terminal `@post` method arguments must be sent as HTTP POST `application/x-www-form-urlencoded` 
params, null arguments should be skipped.
```http
POST /terminalMethod HTTP/1.1
Content-Type: application/x-www-form-urlencoded

arg0=Hello&arg1=World
```

Arguments must be serialized into JSON UTF-8 strings, with quotes stripped, and then url-encoded.
```http
GET /terminalMethod?user={"firstName"%3A+"John",+"lastName%3A"+"Doe"} HTTP/1.1
```

Response
--------
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

Client pseudo-code
------------------
```
Given an invocation chain and an application interface (the root interface).
Assert that the last method in an invocation chain is terminal
(its result is a data type or it is void).

If the last method in an invocation chain is @post:
    Set the request HTTP method to POST

For each method in an invocation chain:
    Append '/' to the request path;
    Append a method name to the request path;
    Serialize method arguments into JSON strings, strip quotes and url-encode them.
    
    If the method is an interface method:
        Append the JSON strings separated by '/' to the request path.
    
    Else if the method is @post:
        Add the JSON strings to POST params with the arg names as the keys.
    
    Else:
        Add the JSON strings to the request query string with the arg names as the keys.


Remember the last method result type and the application exception type.
Send the HTTP request.
Receive an HTTP response.

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

Server pseudo-code
------------------
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
    
    If the method is an interface method:
        If the remaining path segments count is less than the arguments count:
            Return HTTP 400, 'Wrong number of method arguments'.
        Pop the arguments from the request path segments.
    Else if the method is @post:
        Get the arguments from the request POST body, set absent arguments to null.
    Else:
        Get the arguments from the request query string, set absent arguments to null.
    
    Url-decode the arguments and parse them from JSON strings.
    If the argument type is a string, an enum or a date type,
    and a JSON string does not start and end with quotes ("), enclose it
    in quotes to get a valid JSON string.
    
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
