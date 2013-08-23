# encoding: utf-8
import urllib
import urlparse
import httplib

import requests
from requests.status_codes import codes

from pdef import Invocation
from pdef.rpc_pd import RpcResponse, RpcStatus, ServerError, NetworkError, \
    ClientError, MethodNotFoundError, WrongMethodArgsError, MethodNotAllowedError


FORM_URLENCODED_CONTENT_TYPE = 'application/x-www-form-urlencoded'
REQUEST_CONTENT_TYPE = FORM_URLENCODED_CONTENT_TYPE
RESPONSE_CONTENT_TYPE = 'application/json; charset=utf-8'
ERROR_RESPONSE_CONTENT_TYPE = 'text/plain; charset=utf-8'


class RestClient(object):
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

        # Convert invocation chain into path, query and post params.
        path, query, post = '', {}, {}
        for inv in invocation.to_chain():
            path, query, post = self._serialize_invocation(inv, path, query, post)

        # Calc the method and the complete url.
        method = 'POST' if invocation.method.is_post else 'GET'
        url = self._join_url_and_path(self.url, path)

        # Create a request.
        return requests.Request(method, url=url, data=post, params=query)

    def _serialize_invocation(self, invocation, path='', query=None, post=None):
        '''Add an invocation to a path, query dict, and post dict.'''
        method = invocation.method
        path += '/' if method.is_index else '/' + urllib.quote(method.name)
        query = dict(query) if query else {}
        post = dict(post) if post else {}

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
                serialized = self._serialize_positional_arg(arg, value)
                path += '/' + urllib.quote(serialized)

        return path, query, post

    def _serialize_positional_arg(self, arg, value):
        '''Serialize a positional argument and percent-encode it.'''
        type0 = arg.type
        if value is None:
            return ''

        if type0.is_primitive or type0.is_enum:
            s = type0.to_string(value)
        else:
            s = type0.to_json(value)

        return urllib.quote(s.encode('utf-8'), safe='[],{}')

    def _serialize_arg(self, arg, value):
        '''Serialize a query/post argument, but do not percent-encode it.'''
        type0 = arg.type

        if type0.is_primitive or type0.is_enum:
            return type0.to_string(value)

        return type0.to_json(value)

    def _join_url_and_path(self, url, path):
        '''Join url and path, correctly handle slashes /.'''
        if not url.endswith('/'):
            url += '/'

        if path.startswith('/'):
            path = path[1:]

        return url + path

    def _send_request(self, request):
        '''Send a requests.Request.'''
        session = self.session if self.session else requests.session()
        prepared = request.prepare()
        return session.send(prepared)

    def _parse_response(self, http_response, invocation):
        '''Parse a requests.Response into a result or raise an exception.'''

        # If it is not a json response, raise an rpc exception
        # based on the status code.
        http_status = http_response.status_code

        if http_status != 200:
            # The server has not replied with a valid rpc response.
            if http_status == 400:
                raise ClientError(http_response.text)

            elif http_status == 404:
                raise MethodNotFoundError(http_response.text)

            elif http_status == 405:
                raise MethodNotAllowedError(http_response.text)

            elif http_status in (502, 503):
                raise NetworkError(http_response.text)

            elif http_status == 500:
                raise ServerError(http_response.text)

            raise ServerError('Status code: %s, text=%s' % (http_status, http_response.text))


        # Try to parse a json rpc response.
        try:
            response = RpcResponse.parse_json(http_response.text)
        except Exception, e:
            raise ClientError('Failed to parse a server response: %s' % e)


        status = response.status
        result = response.result
        if status == RpcStatus.OK:
            # It's a successful result.
            # Parse it using the invocation method result descriptor.
            return invocation.result.parse_object(result)

        elif status == RpcStatus.EXCEPTION:
            # It's an excepcted exception.
            # Parse it using the invocation exception descriptor.

            exc = invocation.exc
            if not exc:
                raise ClientError('Unsupported application exception: %s' % result)
            raise exc.parse_object(result)

        raise ClientError('Unsupported rpc response status: response=%s' % response)


class RestServer(object):
    def __init__(self, descriptor, service_or_callable, on_invocation=None, on_exception=None,
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
        self.descriptor = descriptor
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
                response = self._result_to_response(result, invocation)
            except Exception, e:
                response = self._app_exc_to_response(e, invocation)
                if not response:
                    # It's not an application exception, reraise it.
                    raise
            return self._rest_response(response)
        except Exception, e:
            return self._error_rest_response(e)

    def _parse_request(self, request):
        '''Parse a request and return a chained invocation.'''
        path = request.path
        query = request.query
        post = request.post

        if path.startswith('/'):
            path = path[1:]
        parts = path.split('/')

        descriptor = self.descriptor
        invocation = Invocation.root()
        while parts:
            part = parts.pop(0)
            # Find a method by a name or an index method.
            if not descriptor:
                raise MethodNotFoundError('Method not found')

            method = descriptor.find_method(part) or descriptor.index_method
            if not method:
                raise MethodNotFoundError('Method not found')

            # If an index method, prepend the part back,
            # because index methods do not have names.
            if method.is_index and part != '':
                parts.insert(0, part)

            # Parse method arguments.
            args = {}
            if method.is_remote and method.is_post:
                if request.method.upper() != 'POST':
                    raise MethodNotAllowedError('Method not allowed, POST required')

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
            descriptor = None if method.is_remote else method.result

        if not invocation.method.is_remote:
            raise MethodNotFoundError('Method not found')

        return invocation

    def _parse_positional_arg(self, arg, value):
        type0 = arg.type
        if value == '':
            return '' if type0.is_string else None

        s = urllib.unquote(value).decode('utf-8')
        if type0.is_primitive:
            return type0.parse_string(s)
        return type0.parse_json(s)

    def _parse_arg(self, arg, value):
        if arg.type.is_primitive or arg.type.is_enum:
            return arg.type.parse_string(value)
        return arg.type.parse_json(value)

    def _invoke(self, invocation):
        '''Invoke an invocation on a service.'''
        service = self.supplier()
        return invocation.invoke(service)

    def _result_to_response(self, result, invocation):
        '''Serialize an invocation result and return an instance of RpcResponse.'''
        method = invocation.method
        result = method.result.to_object(result)
        return RpcResponse(status=RpcStatus.OK, result=result)

    def _app_exc_to_response(self, e, invocation):
        '''Handle an application exception and return an RpcResponse or return None.'''
        if invocation.exc and isinstance(e, invocation.exc.pyclass):
            result = invocation.exc.to_object(e)
            return RpcResponse(status=RpcStatus.EXCEPTION, result=result)
        return None

    def _rest_response(self, rpc_response):
        '''Write an rpc response and return an http server response.'''
        status = 200
        json = rpc_response.to_json()
        return RestServerResponse(status, content=json)

    def _error_rest_response(self, e):
        '''Handle an unhandled error and return an RpcResponse.'''
        if isinstance(e, WrongMethodArgsError):
            http_status = 400
            result = e.text
        elif isinstance(e, MethodNotFoundError):
            http_status = 404
            result = e.text
        elif isinstance(e, MethodNotAllowedError):
            http_status = 405
            result = e.text
        elif isinstance(e, ClientError):
            http_status = 400
            result = e.text
        elif isinstance(e, NetworkError):
            http_status = 503
            result = e.text
        elif isinstance(e, ServerError):
            http_status = 500
            result = e.text
        else:
            http_status = 500
            result = 'Internal server error'

        return RestServerResponse(http_status, content=result,
                                  content_type=ERROR_RESPONSE_CONTENT_TYPE)


class RestServerRequest(object):
    '''Simple gateway-agnostic server request, all fields must be unicode.'''

    @classmethod
    def from_wsgi(cls, env):
        '''Create an http server request from a wsgi request.'''
        method = env['REQUEST_METHOD']
        path = env['SCRIPT_NAME'] + env['PATH_INFO']
        query = cls._read_wsgi_query(env)
        post = cls._read_wsgi_post(env)

        decode = lambda s: s.decode('utf-8')
        query = {decode(k): decode(vv[0]) for k, vv in query.items()}
        post = {decode(k): decode(vv[0]) for k, vv in post.items()}
        return RestServerRequest(method, path, query=query, post=post)

    @classmethod
    def _read_wsgi_clength(cls, env):
        clength = env.get('CONTENT_LENGTH') or 0
        try:
            return int(clength)
        except:
            return 0

    @classmethod
    def _read_wsgi_query(cls, env):
        return urlparse.parse_qs(env['QUERY_STRING']) if 'QUERY_STRING' in env else {}

    @classmethod
    def _read_wsgi_post(cls, env):
        ctype = env.get('CONTENT_TYPE', '')
        clength = cls._read_wsgi_clength(env)

        body = None
        if clength > 0 and ctype.lower() == FORM_URLENCODED_CONTENT_TYPE:
            body = env['wsgi.input'].read(clength)

        return urlparse.parse_qs(body) if body else {}

    def __init__(self, method, path, query=None, post=None):
        '''Create an http server request.

        @param method   Http method string.
        @param path     Request path.
        @param query    Dict, unquoted query params, only single values.
        @param body     Dict, unquoted post params, only single values.
        '''
        self.method = method
        self.path = path
        self.query = query
        self.post = post


class RestServerResponse(object):
    '''Simple gateway-agnostic server response. Should be created with a unicode content.
    Internally, converts it into a UTF-8 string.

    @field status           Http status code as an int.
    @field content          Unicode content.
    @field content_type     Content type of the body.
    @field content_length   Length of the body.
    '''
    def __init__(self, status=200, content=None, content_type=RESPONSE_CONTENT_TYPE):
        self.status = status
        self.content = content.encode('utf-8') if content else ''
        self.content_type = content_type
        self.unicode_content = content

    @property
    def content_length(self):
        return len(self.content)


class WsgiRestServer(object):
    def __init__(self, callable_rest_server):
        self.rest_server = callable_rest_server

    def __call__(self, environ, start_response):
        return self.handle(environ, start_response)

    def handle(self, environ, start_response):
        request = RestServerRequest.from_wsgi(environ)
        response = self.rest_server(request)

        reason = httplib.responses.get(response.status)
        status = '%s %s' % (response.status,  reason)
        headers = [('Content-Type', response.content_type),
                   ('Content-Length', str(response.content_length))]
        start_response(status, headers)
        yield response.content
