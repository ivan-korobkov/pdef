# encoding: utf-8
import urllib
import urlparse

import requests
from pdef import Invocation
from rpc_pd import RpcResponse, RpcResponseStatus, RpcError, ServerError, ClientError, \
    NetworkError, RpcErrorCode


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
        request = self._build_request(invocation)
        response = self._send_request(request)
        result = self._parse_response(response, invocation)
        return result

    def _build_request(self, invocation):
        '''Convert an invocation into a requests.Request.'''
        path, query, post = '', {}, {}
        for inv in invocation.to_chain():
            path = self._serialize_invocation(inv, path, query, post)

        method = 'POST' if invocation.method.is_post else 'GET'
        url = self.url + path
        return requests.Request(method, url=url, data=post, params=query)

    def _send_request(self, request):
        '''Send a requests.Request.'''
        session = self.session if self.session else requests.session()
        return session.send(request)

    def _parse_response(self, response, invocation):
        '''Parse a requests.Response into a result or raise an exception.'''
        resp = None
        if response.headers['Content-Type'] == RESPONSE_CONTENT_TYPE:
            # If the content type is json, then there should be a valid RpcResponse.
            try:
                resp = RpcResponse.parse_json(response.text)
            except Exception, e:
                raise ServerError(text='Failed to parse a server response: %s' % e)

        if not resp:
            # The server has not replied with a valid response.
            # Try to raise
            status_code = response.status_code
            if status_code == 400:
                raise ClientError(text=response.text)
            elif status_code == 502 or status_code == 503:
                raise NetworkError(text=response.text)
            elif status_code == 500:
                raise ServerError(text=response.text)

            raise ServerError(text='Status code: %s, text=%s' % (status_code, response.text))

        status = response.status
        result = response.result
        if status == RpcResponseStatus.OK:
            # Successful invocation result.
            return invocation.result.parse_object(result)

        elif status == RpcResponseStatus.EXCEPTION:
            # Application exception.
            raise invocation.exc.parse_object(result)

        elif status == RpcResponseStatus.ERROR:
            # Rpc error.
            raise RpcError.parse_dict(result)

        else:
            # Unknown rpc response status.
            raise ServerError(text='Unknown rpc response status: %s, result=%s' % (status, result))

    def _serialize_invocation(self, invocation, path, query, post):
        '''Add an invocation to a path, query dict, and post dict.'''
        method = invocation.method
        path += '/' if method.is_index else '/' + urllib.quote(method.name)

        args = invocation.args
        if method.is_remote and method.is_post:
            # Add arguments as post params, serialize messages and collections into json.
            for arg in method.args:
                value = args.get(arg.name)
                if arg.type.is_primitive:
                    serialized = arg.type.to_string(value)
                else:
                    serialized = arg.type.to_json(value)
                post[arg.name] = serialized

        elif method.is_remote:
            # Add arguments as query params.
            for arg in method.args:
                value = args.get(arg.name)
                if arg.type.is_primitive:
                    serialized = arg.type.to_string(value)
                else:
                    serialized = arg.type.to_json(value)
                query[arg.name] = serialized
        else:
            # Positionally prepend all arguments to the path.
            for arg in method.args:
                value = args.get(arg.name)
                if arg.type.is_primitive:
                    serialized = arg.type.to_string(value)
                else:
                    serialized = arg.type.to_json(value)
                path += '/' + urllib.quote(serialized)

        return path


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
        self.interface = interface
        self.supplier = service_or_callable if callable(service_or_callable) \
            else lambda: service_or_callable

        self.on_invocation = on_invocation
        self.on_exception = on_exception
        self.on_error = on_error

    def __call__(self, request):
        return self.handle(request)

    def handle(self, request):
        response = None
        try:
            invocation = self._parse_request(request)
            try:
                result = self._invoke(invocation)
                response = self._serialize_result(result, invocation)
            except Exception, e:
                response = self._handle_exception(e, invocation)
                if not response:
                    raise
        except Exception, e:
            response = self._handle_error(e)

        return self._write_response(response)

    def _parse_request(self, request):
        '''Parse a request and return an invocation.'''
        path = request.path
        query = request.query
        post = request.post
        parts = path.split('/').lstrip('/')

        interface = self.interface
        invocation = Invocation.root()
        while parts:
            part = parts.pop(0)
            # Find a method by a name or an index method.
            if not interface:
                raise ClientError(text='Method not found: %s' % path)

            method = None
            for m in interface.methods:
                if m.name == part:
                    method = m
                    break
                if m.is_index:
                    method = m

            if not method:
                raise ClientError(text='Method not found: %s' % path)

            if method.is_index:
                parts.insert(0, part)

            # Parse method arguments.
            args = {}
            if method.is_remote and method.is_post:
                if request.method.upper() != 'POST':
                    raise ClientError(text='Method not allowed, POST required: %s' % path)

                # Parse arguments as post params.
                for arg in method.args:
                    value = post.get(arg.name)
                    if arg.type.is_primitive:
                        parsed = arg.type.parse_string(value)
                    else:
                        parsed = arg.type.parse_json(value)
                    args[arg.name] = parsed

            elif method.is_remote:
                # Parse arguments as query params.
                for arg in method.args:
                    value = query.get(arg.name)
                    if arg.type.is_primitive:
                        parsed = arg.type.parse_string(value)
                    else:
                        parsed = arg.type.parse_json(value)
                    args[arg.name] = parsed

            else:
                # Parse arguments as positional params.
                for arg in method.args:
                    value = urllib.unquote(parts.pop(0)) if parts else None
                    if arg.type.is_primitive:
                        parsed = arg.type.parse_string(value)
                    else:
                        parsed = arg.type.parse_json(value)
                    args[arg.name] = parsed

            invocation = invocation.next(method, **args)
            interface = None if method.is_remote else method.result

        if not invocation.method.is_remote:
            raise ClientError(text='Method not found: %s' % path)

        return invocation

    def _invoke(self, invocation):
        '''Invoke an invocation on a service.'''
        service = self.supplier()
        return invocation.invoke(service)

    def _serialize_result(self, result, invocation):
        '''Serialize an invocation result and return an instance of RpcResponse.'''
        method = invocation.method
        return method.result.to_object(result)

    def _handle_exception(self, e, invocation):
        '''Handle an application exception and return an RpcResponse or return None.'''
        if invocation.exc and isinstance(e, invocation.exc.pyclass):
            result = invocation.exc.to_object(e)
            return RpcResponse(status=RpcResponseStatus.EXCEPTION, result=result)
        return None

    def _handle_error(self, e):
        '''Handle an unhandled error and return an RpcResponse.'''
        result = e if isinstance(e, RpcError) else ServerError(text='Internal server error')
        return RpcResponse(status=RpcResponseStatus.ERROR, result=result)

    def _write_response(self, response):
        '''Write an rpc response and return an http server response.'''
        if response.status == RpcResponseStatus.OK \
                or response.status == RpcResponseStatus.EXCEPTION:
            status = 200
        else:
            status = 400 if response.result['code'] == RpcErrorCode.CLIENT_ERROR else 500

        json = response.to_json()
        return HttpServerResponse(status, body=json)


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

        status = '%s %s' % (response.status,  requests.codes[response.status])
        headers = [('Content-Type', response.content_type),
                   ('Content-Length', response.content_length)]
        start_response(status, headers)
        content = response.body.encode('utf-8')
        yield content


def wsgi_server(interface, service_or_supplier):
    http_server = HttpServer(interface, service_or_supplier)
    return WsgiServer(http_server)
