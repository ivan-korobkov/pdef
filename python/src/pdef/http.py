# encoding: utf-8
import urllib
import urlparse

import requests
from requests.status_codes import codes

from pdef import Invocation
from pdef.rpc_pd import RpcResponse, RpcStatus, ServerError, NetworkError, \
    ClientError, MethodNotFoundError, WrongMethodArgsError


REQUEST_CONTENT_TYPE = 'application/x-www-form-urlencoded'
RESPONSE_CONTENT_TYPE = 'application/json; charset=utf-8'


class HttpClient(object):
    def __init__(self, url, session=None):
        '''Create an http client.
        @param url Base url.
        @param session A session to be use or None, see requests.session.
        '''
        self.url = url
        self.session = session

    def __call__(self, invocation):
        return self.handle(invocation)

    def handle(self, invocation):
        '''Serialize an invocation, send a request, parse a response and return the result.'''
        request = self._create_request(invocation)
        response = self._send_request(request)
        result = self._parse_response(response, invocation)
        return result

    def _create_request(self, invocation):
        '''Convert an invocation into a requests.Request.'''
        path, query, post = '', {}, {}
        for inv in invocation.to_chain():
            path = self._serialize_invocation(inv, path, query, post)

        method = 'POST' if invocation.method.is_post else 'GET'
        url = self.url + path
        return requests.Request(method, url=url, data=post, params=query)

    def _serialize_invocation(self, invocation, path, query, post):
        '''Add an invocation to a path, query dict, and post dict.'''
        method = invocation.method
        path += '/' if method.is_index else '/' + urllib.quote(method.name)

        args = invocation.args
        if method.is_remote and method.is_post:
            # Add arguments as post params, serialize messages and collections into json.
            for arg in method.args:
                value = args.get(arg.name)
                if value is None:
                    continue
                post[arg.name] = self._serialize_arg(arg, value)

        elif method.is_remote:
            # Add arguments as query params.
            for arg in method.args:
                value = args.get(arg.name)
                if value is None:
                    continue
                query[arg.name] = self._serialize_arg(arg, value)
        else:
            # Positionally prepend all arguments to the path.
            assert not method.is_post, 'Post methods must be remote, %s' % method
            for arg in method.args:
                value = args.get(arg.name)
                if value is None:
                    serialized = ''
                else:
                    serialized = self._serialize_arg(arg, value)
                path += '/' + urllib.quote(serialized)

        return path

    def _serialize_arg(self, arg, value):
        if arg.type.is_primitive or arg.type.is_enum:
            return arg.type.to_string(value)
        return arg.type.to_json(value)

    def _send_request(self, request):
        '''Send a requests.Request.'''
        session = self.session if self.session else requests.session()
        prepared = request.prepare()
        return session.send(prepared)

    def _parse_response(self, http_response, invocation):
        '''Parse a requests.Response into a result or raise an exception.'''
        response = None
        if http_response.headers.get('Content-Type', '').lower().startswith(RESPONSE_CONTENT_TYPE):
            # If the content type is json, then there should be a valid RpcResponse.
            try:
                response = RpcResponse.parse_json(http_response.text)
            except Exception, e:
                raise ServerError('Failed to parse a server response: %s' % e)

        if not response:
            # The server has not replied with a valid rpc response.
            http_status = http_response.status_code
            if http_status == 400:
                raise ClientError(http_response.text)
            elif http_status == 404:
                raise MethodNotFoundError(http_response.text)
            elif http_status in (502, 503):
                raise NetworkError(http_response.text)
            elif http_status == 500:
                raise ServerError(http_response.text)

            raise ServerError('Status code: %s, text=%s' % (http_status, http_response.text))

        status = response.status
        result = response.result

        # Successful and expected exception responses.
        if status == RpcStatus.OK:
            return invocation.result.parse_object(result)
        elif status == RpcStatus.EXCEPTION:
            raise invocation.exc.parse_object(result)

        # Rpc errors.
        if status == RpcStatus.SERVER_ERROR:
            raise ServerError(str(result))
        elif status == RpcStatus.NETWORK_ERROR:
            raise NetworkError(str(result))
        elif status == RpcStatus.CLIENT_ERROR:
            raise ClientError(str(result))
        elif status == RpcStatus.METHOD_NOT_FOUND:
            raise MethodNotFoundError(str(result))
        elif status == RpcStatus.WRONG_METHOD_ARGS:
            raise WrongMethodArgsError(str(result))

        raise ServerError(text='Unknown rpc response status: %s, result=%s' % (status, result))


class HttpServer(object):
    def __init__(self, interface, service_or_callable, on_invocation=None, on_exception=None,
                 on_error=None):
        '''Create a WSGI server.

        @param interface            Generated interface (class).
        @param service_or_callable  Service instance or a callable which returns a service
                                    for invocations.
        @param on_invocation        Callback for incoming invocations.
        @param on_exception         Callback for application (expected) exceptions
                                    defined in the interface throws clause.
        @param on_error             Callback for unhandled exceptions.
        '''
        self.interface = interface.__descriptor__
        self.supplier = service_or_callable if callable(service_or_callable) \
            else lambda: service_or_callable

        self.on_invocation = on_invocation
        self.on_exception = on_exception
        self.on_error = on_error

    def __call__(self, request):
        return self.handle(request)

    def handle(self, request):
        try:
            invocation = self._parse_request(request)
            try:
                result = self._invoke(invocation)
                serialized = self._serialize_result(result, invocation)
                response = RpcResponse(status=RpcStatus.OK, result=serialized)
            except Exception, e:
                response = self._handle_app_exception(e, invocation)
                if not response:
                    # It's not an application exception, reraise it.
                    raise
            return self._successful_response(response)
        except Exception, e:
            return self._error_response(e)

    def _parse_request(self, request):
        '''Parse a request and return a chained invocation.'''
        path = request.path
        query = request.query
        post = request.post

        if path.startswith('/'):
            path = path[1:]
        parts = path.split('/')

        interface = self.interface
        invocation = Invocation.root()
        while parts:
            part = parts.pop(0)
            # Find a method by a name or an index method.
            if not interface:
                raise MethodNotFoundError('Method not found')

            method = None
            for m in interface.methods:
                if m.name == part:
                    method = m
                    break
                if m.is_index:
                    method = m

            if not method:
                raise MethodNotFoundError('Method not found')

            if method.is_index:
                parts.insert(0, part)

            # Parse method arguments.
            args = {}
            if method.is_remote and method.is_post:
                if request.method.upper() != 'POST':
                    raise ClientError('Method not allowed, POST required')

                # Parse arguments as post params.
                for arg in method.args:
                    value = post.get(arg.name)
                    args[arg.name] = self._parse_arg(arg, value)

            elif method.is_remote:
                # Parse arguments as query params.
                for arg in method.args:
                    value = query.get(arg.name)
                    args[arg.name] = self._parse_arg(arg, value)

            else:
                # Parse arguments as positional params.
                for arg in method.args:
                    if not parts:
                        raise WrongMethodArgsError('Wrong number of method args')

                    value = urllib.unquote(parts.pop(0))
                    args[arg.name] = self._parse_arg(arg, value)

            invocation = invocation.next(method, **args)
            interface = None if method.is_remote else method.result

        if not invocation.method.is_remote:
            raise MethodNotFoundError('Method not found')

        return invocation

    def _parse_arg(self, arg, value):
        if arg.type.is_primitive or arg.type.is_enum:
            return arg.type.parse_string(value)
        return arg.type.parse_json(value)

    def _invoke(self, invocation):
        '''Invoke an invocation on a service.'''
        service = self.supplier()
        return invocation.invoke(service)

    def _serialize_result(self, result, invocation):
        '''Serialize an invocation result and return an instance of RpcResponse.'''
        method = invocation.method
        return method.result.to_object(result)

    def _handle_app_exception(self, e, invocation):
        '''Handle an application exception and return an RpcResponse or return None.'''
        if invocation.exc and isinstance(e, invocation.exc.pyclass):
            result = invocation.exc.to_object(e)
            return RpcResponse(status=RpcStatus.EXCEPTION, result=result)
        return None

    def _successful_response(self, response):
        '''Write an rpc response and return an http server response.'''
        status = 200
        json = response.to_json()
        return HttpServerResponse(status, body=json)

    def _error_response(self, e):
        '''Handle an unhandled error and return an RpcResponse.'''
        if isinstance(e, WrongMethodArgsError):
            status = RpcStatus.WRONG_METHOD_ARGS
            http_status = 400
            result = e.text
        elif isinstance(e, MethodNotFoundError):
            status = RpcStatus.METHOD_NOT_FOUND
            http_status = 404
            result = e.text
        elif isinstance(e, ClientError):
            status = RpcStatus.CLIENT_ERROR
            http_status = 400
            result = e.text
        elif isinstance(e, NetworkError):
            status = RpcStatus.NETWORK_ERROR
            http_status = 503
            result = e.text
        else:
            status = RpcStatus.SERVER_ERROR
            http_status = 500
            result = 'Internal server error'

        response = RpcResponse(status=status, result=result)
        http_body = response.to_json()
        return HttpServerResponse(http_status, body=http_body)


class HttpServerRequest(object):
    '''Simple gateway-agnostic server request.'''
    @classmethod
    def from_wsgi(cls, environ):
        '''Create an http server request from a wsgi request.'''
        ctype = environ.get('CONTENT_TYPE')
        clength = environ.get('CONTENT_LENGTH', 0)
        if ctype == REQUEST_CONTENT_TYPE:
            body = environ['wsgi.input'].read(clength)
        else:
            body = None

        method = environ['REQUEST_METHOD']
        path = environ['SCRIPT_NAME'] + environ['PATH_INFO']
        query = urlparse.parse_qs(environ['QUERY_STRING']) if environ['QUERY_STRING'] else {}
        post = urlparse.parse_qs(body) if body else {}
        return HttpServerRequest(method, path, query=query, post=post)


    def __init__(self, method, path, query=None, post=None):
        '''Create an http server request.

        @param method   Http method string.
        @param path     Request path.
        @param query    Dict, unquoted query params.
        @param body     Dict, unquoted post params.
        '''
        self.method = method
        self.path = path
        self.query = query
        self.post = post


class HttpServerResponse(object):
    '''Simple gateway-agnostic server response.

    @field status           Http status code as an int.
    @field body             Response body as a string.
    @field content_type     Content type of the body.
    @field content_length   Length of the body.
    '''
    def __init__(self, status=200, body=None, content_type=RESPONSE_CONTENT_TYPE):
        self.status = status
        self.body = body or u''
        self.content_type = content_type

    @property
    def content_length(self):
        return len(self.body)


class WsgiServer(object):
    def __init__(self, http_server):
        self.http_server = http_server

    def __call__(self, environ, start_response):
        return self.handle(environ, start_response)

    def handle(self, environ, start_response):
        request = HttpServerRequest.from_wsgi(environ)
        response = self.http_server.handle(request)

        status = '%s %s' % (response.status,  codes[response.status])
        headers = [('Content-Type', response.content_type),
                   ('Content-Length', str(response.content_length))]
        start_response(status, headers)
        content = response.body.encode('utf-8')
        yield content


def wsgi_server(interface, service_or_supplier):
    http_server = HttpServer(interface, service_or_supplier)
    return WsgiServer(http_server)


if __name__ == '__main__':
    from test_pd import TestInterface
    from wsgiref.simple_server import make_server

    app = wsgi_server(TestInterface, TestInterface())
    httpd = make_server('', 8000, app)
    print "Serving on port 8000..."

    # Serve until process is killed
    httpd.serve_forever()
