# encoding: utf-8
import httplib
import logging
import requests
import urllib
import urlparse

import pdef.invocation
from pdef.rpc import *


GET = 'GET'
POST = 'POST'
CHARSET = 'utf-8'

JSON_MIME_TYPE = 'application/json'
TEXT_MIME_TYPE = 'text/plain'
FORM_MIME_TYPE = 'application/x-www-form-urlencoded'

JSON_CONTENT_TYPE = 'application/json; charset=utf-8'
TEXT_CONTENT_TYPE = 'text/plain; charset=utf-8'


def client(interface, url, session=None):
    '''Create a default REST client.'''
    sender = client_sender(url, session=session)
    handler = client_handler(sender)
    return pdef.invocation.proxy(interface, handler)


def client_handler(sender):
    '''Create a REST client handler.'''
    return RestClientHandler(sender)


def client_sender(url, session=None):
    '''Create a REST client sender.'''
    return RestClientSender(url, session=session)


def server(interface, service_or_provider):
    '''Create a default REST server.

    @param interface:           An interface class with a __descriptor__ field.
    @param service_or_provider: A service or a callable service provider.
    '''
    invoker = pdef.invocation.invoker(service_or_provider)
    return server_handler(interface, invoker)


def server_handler(interface, invoker):
    '''Create a REST server handler.'''
    descriptor = interface.__descriptor__
    return RestServerHandler(descriptor, invoker)


def wsgi_server(rest_server):
    '''Create a WSGI REST server.'''
    return WsgiRestServer(rest_server)


class RestRequest(object):
    '''Simple REST request which decouples the REST client/server from the transport libraries.

    The result contains an HTTP method, a url-encoded path, and two dicts with query and post
    params. The params must be unicode not url-encoded strings.
    '''
    def __init__(self, method=GET, path='', query=None, post=None):
        self.method = method
        self.path = path
        self.query = dict(query) if query else {}
        self.post = dict(post) if post else {}

    def __repr__(self):
        return '<RestRequest %s %s%s>' % (self.method, self.path,
                                          '?%s' % self.query_string if self.query else '')

    @property
    def is_post(self):
        return self.method.upper() == POST

    @property
    def query_string(self):
        if not self.query:
            return ''

        q = {unicode(k).encode('utf-8'): unicode(v).encode('utf-8') for k, v in self.query.items()}
        return urllib.urlencode(q)


class RestResponse(object):
    '''Simple REST response which decouples the REST client/server from the transport libraries.

    The response contains an int HTTP status code, a decoded unicode string, and a content type.
    The content type can be "application/json" or "text/plain".
    '''
    def __init__(self, status=httplib.OK, content=None, content_type=None):
        self.status = status
        self.content = content
        self.content_type = content_type

    @property
    def is_ok(self):
        return self.status == httplib.OK

    @property
    def is_application_json(self):
        if not self.content_type:
            return False

        return self.content_type.lower().startswith(JSON_MIME_TYPE)

    @property
    def is_text_plain(self):
        if not self.content_type:
            return False
        return self.content_type.lower().startswith(TEXT_MIME_TYPE)


class RestClientHandler(object):
    logger = logging.getLogger('pdef.rest.RestClientHandler')

    def __init__(self, sender):
        '''Create a rest client.'''
        self.sender = sender

    def __call__(self, invocation):
        return self.invoke(invocation)

    def invoke(self, invocation):
        '''Serialize an invocation, send a request, parse a response and return the result.'''
        self.logger.debug('Invoking %s', invocation)
        request = self._create_request(invocation)
        response = self._send_request(request)

        if self._is_successful(response):
            return self._parse_result(response, invocation)

        # The method raise an error itself for better stack traces.
        self._parse_raise_error(response)

    def _create_request(self, invocation):
        '''Convert an invocation into a RestRequest.'''
        request = RestRequest()
        request.method = POST if invocation.method.is_post else GET

        # Append invocations from a chain.
        for inv in invocation.to_chain():
            self._serialize_invocation(inv, request)

        return request

    def _serialize_invocation(self, invocation, request):
        '''Add an invocation to a path, query dict, and post dict.'''
        method = invocation.method

        # Append the url-encoded method name to the path.
        request.path += '/'
        if not method.is_index:
            request.path += urllib.quote(method.name)


        # Add the method arguments to the request.
        args = invocation.args
        if method.is_post:
            # Serialize and put args to request.post.

            for arg in method.args:
                value = args.get(arg.name)
                self._serialize_query_arg(arg, value, request.post)

        elif method.is_remote:
            # Serialize and put args to request.query.

            for arg in method.args:
                value = args.get(arg.name)
                self._serialize_query_arg(arg, value, request.query)

        else:
            # Prepend args to request.path.

            for arg in method.args:
                value = args.get(arg.name)
                request.path += '/' + self._serialize_positional_arg(arg, value)

    def _serialize_positional_arg(self, arg, value):
        '''Serialize a positional argument and percent-encode it.'''
        serialized = self._serialize_arg_to_string(arg.type, value)
        return urllib.quote(serialized.encode('utf-8'), safe='[],{}-')

    def _serialize_query_arg(self, arg, value, dst):
        '''Serialize a query/post argument and put into a dst dict.'''
        if value is None:
            # Skip none arguments.
            return

        descriptor = arg.type
        is_form = descriptor.is_message and descriptor.is_form
        if not is_form:
            dst[arg.name] = self._serialize_arg_to_string(descriptor, value)
            return

        # It's a form, expand its fields into distinct arguments.
        for field in descriptor.fields:
            fvalue = field.get(value)
            if fvalue is None:
                continue

            dst[field.name] = self._serialize_arg_to_string(field.type, fvalue)

    def _serialize_arg_to_string(self, descriptor, value):
        if value is None:
            return ''

        if descriptor.is_primitive or descriptor.is_enum:
            return descriptor.to_string(value)

        return descriptor.to_json(value)

    def _send_request(self, request):
        '''Send a request and return a response.'''
        return self.sender(request)

    def _is_successful(self, response):
        return response.is_ok and response.is_application_json

    def _parse_result(self, response, invocation):
        '''Parse a RestResponse into an invocation result'''

        rpc = RpcResult.parse_json(response.content)
        status = rpc.status
        data = rpc.data

        if status == RpcStatus.OK:
            # It's a successful result.
            # Parse it using the invocation method result descriptor.

            r = invocation.result.parse_object(data)
            return pdef.invocation.InvocationResult(r)

        elif status == RpcStatus.EXCEPTION:
            # It's an expected exception.
            # Parse it using the invocation exception descriptor.

            exc = invocation.exc
            if not exc:
                raise ClientError('Unsupported application exception')

            r = exc.parse_object(data)
            return pdef.invocation.InvocationResult(r, ok=False)

        raise ClientError('Unsupported rpc response status=%s' % status)

    def _parse_raise_error(self, response):
        '''Parse an error from a RestResponse.'''
        status = response.status
        text = response.content or 'No text'
        text = text if len(text) < 255 else text[:255]  # Limit the length for the exception.

        if status == httplib.BAD_REQUEST:               # 400
            raise ClientError(text)

        elif status == httplib.NOT_FOUND:               # 404
            raise MethodNotFoundError(text)

        elif status == httplib.METHOD_NOT_ALLOWED:      # 405
            raise MethodNotAllowedError(text)

        elif status in (httplib.BAD_GATEWAY, httplib.SERVICE_UNAVAILABLE):  # 502, 503
            raise ServiceUnavailableError(text)

        elif status == httplib.INTERNAL_SERVER_ERROR:   # 500
            raise ServerError(text)

        raise ServerError('Server error, status=%s, text=%s' % (status, text))


class RestClientSender(object):
    '''The requests-based sender for RestClient.'''
    def __init__(self, url, session=None):
        self.url = url
        self.session = session

    def __call__(self, request):
        return self.send(request)

    def send(self, request):
        '''Send a RestRequest and return a RestResponse.'''
        url = self._join_url_and_path(self.url, request.path)
        req = requests.Request(request.method, url=url, data=request.post, params=request.query)
        prepared = req.prepare()

        session = self.session if self.session else requests.session()
        resp = session.send(prepared)
        return self._parse_response(resp)

    def _parse_response(self, resp):
        '''Parse a requests.Response into a RestResponse.'''
        status = resp.status_code
        content = resp.text
        content_type = resp.headers.get('content-type') or TEXT_CONTENT_TYPE
        return RestResponse(status, content=content, content_type=content_type)

    def _join_url_and_path(self, url, path):
        '''Join url and path, correctly handle slashes /.'''
        if not url.endswith('/'):
            url += '/'

        if path.startswith('/'):
            path = path[1:]

        return url + path


class RestServerHandler(object):
    logger = logging.getLogger('pdef.rest.RestServerHandler')

    def __init__(self, descriptor, invoker):
        '''Create a WSGI server.'''
        self.descriptor = descriptor
        self.invoker = invoker

    def __call__(self, request):
        return self.handle(request)

    def handle(self, request):
        self.logger.debug('Incoming request %s', request)

        try:
            invocation = self._parse_request(request)
            result = self._invoke(invocation)
            return self._ok_response(result, invocation)
        except Exception, e:
            self.logger.exception('Error, e=%s, request=%s', e, request)

            # Create a rest response from an unhandled exception.
            return self._error_response(e)

    def _parse_request(self, request):
        '''Parse a request and return a chained invocation.'''
        path = request.path
        query = request.query
        post = request.post

        if path.startswith('/'):
            path = path[1:]
        parts = path.split('/')

        descriptor = self.descriptor
        invocation = pdef.invocation.Invocation.root()
        while parts:
            part = parts.pop(0)
            # Find a method by a name or get an index method.
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
            if method.is_post:
                if not request.is_post:
                    raise MethodNotAllowedError('Method not allowed, POST required')

                # Post methods are remote.
                # Parse arguments as post params.
                for arg in method.args:
                    args[arg.name] = self._parse_query_arg(arg, post)

            elif method.is_remote:
                # Parse arguments as query params.
                for arg in method.args:
                    args[arg.name] = self._parse_query_arg(arg, query)

            else:
                # Parse arguments as positional params.
                for arg in method.args:
                    if not parts:
                        raise WrongMethodArgsError('Wrong number of method args')

                    args[arg.name] = self._parse_positional_arg(arg, parts.pop(0))

            invocation = invocation.next(method, **args)
            descriptor = None if method.is_remote else method.result

        if not invocation.is_remote:
            raise MethodNotFoundError('Method not found')

        return invocation

    def _parse_positional_arg(self, arg, value):
        value = urllib.unquote(value).decode('utf-8')
        return self._parse_arg_from_string(arg.type, value)

    def _parse_query_arg(self, arg, src):
        descriptor = arg.type
        is_form = descriptor.is_message and descriptor.is_form

        if not is_form:
            # Parse as a string argument.
            value = src.get(arg.name)
            if value is None:
                return None
            return self._parse_arg_from_string(arg.type, value)

        # Parse as an expanded form fields.
        form = {}
        for field in descriptor.fields:
            fvalue = src.get(field.name)
            if fvalue is None:
                continue

            form[field.name] = self._parse_arg_from_string(field.type, fvalue)

        return descriptor.parse_object(form)

    def _parse_arg_from_string(self, descriptor, value):
        if value is None:
            return

        if value == '':
            return '' if descriptor.is_string else None

        if descriptor.is_primitive or descriptor.is_enum:
            return descriptor.parse_string(value)

        return descriptor.parse_json(value)

    def _invoke(self, invocation):
        '''Invoke an invocation and return InvocationResult.'''
        return self.invoker.invoke(invocation)

    def _ok_response(self, result, invocation):
        '''Create a successful REST response from an invocation result.'''
        data = result.data
        method = invocation.method

        rpc = RpcResult()
        if result.ok:
            # It's a successful method result.
            rpc.status = RpcStatus.OK
            rpc.data = method.result.to_object(data)

        else:
            # It's an expected application exception.
            rpc.status = RpcStatus.EXCEPTION
            rpc.data = method.exc.to_object(data)

        content = rpc.to_json(indent=True)
        return RestResponse(status=httplib.OK, content=content, content_type=JSON_CONTENT_TYPE)

    def _error_response(self, e):
        '''Create an error REST response from an unhandled exception.'''
        if isinstance(e, WrongMethodArgsError):
            http_status = httplib.BAD_REQUEST           # 400
            result = e.text
        elif isinstance(e, MethodNotFoundError):
            http_status = httplib.NOT_FOUND             # 404
            result = e.text
        elif isinstance(e, MethodNotAllowedError):
            http_status = httplib.METHOD_NOT_ALLOWED    # 405
            result = e.text
        elif isinstance(e, ClientError):
            http_status = httplib.BAD_REQUEST           # 400
            result = e.text
        elif isinstance(e, ServiceUnavailableError):
            http_status = httplib.SERVICE_UNAVAILABLE   # 503
            result = e.text
        elif isinstance(e, ServerError):
            http_status = httplib.INTERNAL_SERVER_ERROR # 500
            result = e.text
        else:
            http_status = httplib.INTERNAL_SERVER_ERROR # 500
            result = 'Internal server error'

        return RestResponse(http_status, content=result, content_type=TEXT_CONTENT_TYPE)


class WsgiRestServer(object):
    '''WSGI REST server.'''
    def __init__(self, callable_rest_server):
        self.rest_server = callable_rest_server

    def __call__(self, environ, start_response):
        return self.handle(environ, start_response)

    def handle(self, environ, start_response):
        request = self._parse_request(environ)
        response = self.rest_server(request)

        reason = httplib.responses.get(response.status)
        status = '%s %s' % (response.status,  reason)

        content = response.content or ''
        content = content.encode(CHARSET)
        headers = [('Content-Type', response.content_type),
                   ('Content-Length', str(len(content)))]
        start_response(status, headers)
        yield content

    def _parse_request(self, env):
        '''Create an http server request from a wsgi request.'''
        method = env['REQUEST_METHOD']
        path = env['PATH_INFO']
        query = self._read_wsgi_query(env)
        post = self._read_wsgi_post(env)

        return RestRequest(method, path=path, query=query, post=post)

    def _read_wsgi_query(self, env):
        q = urlparse.parse_qs(env['QUERY_STRING']) if 'QUERY_STRING' in env else {}

        decode = lambda s: s.decode(CHARSET)
        return {decode(k): decode(vv[0]) for k, vv in q.items()}

    def _read_wsgi_post(self, env):
        ctype = env.get('CONTENT_TYPE', '')
        clength = self._read_wsgi_clength(env)

        body = None
        if clength > 0 and ctype.lower().startswith(FORM_MIME_TYPE):
            body = env['wsgi.input'].read(clength)

        post = urlparse.parse_qs(body) if body else {}

        decode = lambda s: s.decode(CHARSET)
        return {decode(k): decode(vv[0]) for k, vv in post.items()}

    def _read_wsgi_clength(self, env):
        clength = env.get('CONTENT_LENGTH') or 0
        try:
            return int(clength)
        except (ValueError, TypeError):
            return 0
