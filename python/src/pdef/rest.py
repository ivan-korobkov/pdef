# encoding: utf-8
import httplib
import requests
import urllib
import urlparse

import pdef
import pdef.descriptors


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
    return pdef.invoke.proxy(interface, handler)


def client_handler(sender):
    '''Create a REST client handler.'''
    return RestClient(sender)


def client_sender(url, session=None):
    '''Create a REST client sender.'''
    return RestSession(url, session=session)


def server(interface, service_or_provider):
    '''Create a default REST server.

    @param interface:           An interface class with a DESCRIPTOR field.
    @param service_or_provider: A service or a callable service provider.
    '''
    invoker = pdef.invoke.invoker(service_or_provider)
    return server_handler(interface, invoker)


def server_handler(interface, invoker):
    '''Create a REST server handler.'''
    descriptor = interface.DESCRIPTOR
    return RestServer(descriptor, invoker)


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


class RestException(Exception):
    def __init__(self, message, status=None):
        super(RestException, self).__init__(message)
        self.status = status


class RestProtocol(object):
    # Invocation serialization.

    def serialize_invocation(self, invocation):
        '''Convert an invocation into a RestRequest.'''
        request = RestRequest()
        request.method = POST if invocation.method.is_post else GET

        # Append invocations from a chain.
        for inv in invocation.to_chain():
            self._serialize_single_invocation(inv, request)

        return request

    def _serialize_single_invocation(self, invocation, request):
        '''Add an invocation to a path, query dict, and post dict.'''
        method = invocation.method

        if method.is_index:
            request.path += '/'
        else:
            # Append the url-encoded method name to the path.
            request.path += '/' + urllib.quote(method.name)

        is_post = method.is_post
        is_remote = method.is_remote

        # Add the method arguments to the request.
        args = invocation.args
        for argd in method.args:
            arg = args.get(argd.name)

            if method.is_post:
                # Serialize an argument as a post param.
                self._serialize_param(argd, arg, request.post)

            elif method.is_remote:
                # Serialize an argument as a query param.
                self._serialize_param(argd, arg, request.query)

            else:
                # Serialize an argument as a path part.
                request.path += '/' + self._serialize_path_argument(argd, arg)

    def _serialize_path_argument(self, argd, arg):
        '''Serialize a positional argument and percent-encode it.'''
        s = self._serialize_to_json(argd.type, arg)
        return self._urlencode(s)

    def _serialize_param(self, argd, arg, dst):
        '''Serialize a query/post argument and put into a dst dict.'''
        if arg is None:
            # Skip none arguments.
            return

        descriptor = argd.type
        is_form = descriptor.is_message and descriptor.is_form
        if not is_form:
            # Serialize as a single json param.
            dst[argd.name] = self._serialize_to_json(descriptor, arg)
            return

        # It's a form, serialize each its field into a json param.
        # Mind polymorphic messages.

        message = arg
        descriptor = message.DESCRIPTOR  # Polymorphic.

        for field in descriptor.fields:
            value = field.get(message)
            if value is None:
                # Skip null fields.
                continue

            dst[field.name] = self._serialize_to_json(field.type, value)

    def _serialize_to_json(self, descriptor, value):
        s = pdef.json.serialize(value, descriptor, indent=None)
        if descriptor.type == pdef.Type.STRING:
            s = s.strip('"')
        return s

    # InvocationResult parsing.

    def parse_invocation_result(self, response, datad, excd=None):
        '''Parse an invocation result from a RestResponse.'''
        result_class = self._result_class(datad, excd)
        result = result_class.parse_json(response.content)

        if result.success:
            # It's a successful result.
            # Return the data.
            return pdef.invoke.InvocationResult.ok(result.data)

        else:
            # It's an expected exception.
            if not excd:
                # The server returned an application exception,
                # but the client does not support them.
                raise RestException('Unsupported application exception')

            return pdef.invoke.InvocationResult.exception(result.exc)

    # Invocation parsing.

    def parse_invocation(self, request, descriptor):
        '''Parse an invocation chain from a RestRequest.'''

        path = request.path
        if path.startswith('/'):
            path = path[1:]

        parts = path.split('/')
        query = request.query
        post = request.post

        invocation = pdef.invoke.Invocation.root()
        while parts:
            part = parts.pop(0)

            # Find a method by a name or get an index method.
            method = descriptor.find_method(part) or descriptor.index_method
            if not method:
                raise RestException('Method not found', httplib.NOT_FOUND)

            if method.is_index and part != '':
                # It's an index method, and the part does not equal
                # the method name. Prepend the part back, it's an argument.
                parts.insert(0, part)

            if method.is_post and not request.is_post:
                # The method requires a POST HTTP request.
                raise RestException('Method not allowed, POST required. The method is %r'
                                    % method.name, httplib.METHOD_NOT_ALLOWED)

            # Parse method arguments.
            args = {}
            for argd in method.args:
                if method.is_post:
                    # Parse a post param.
                    arg = self._parse_param(argd, post)

                elif method.is_remote:
                    # Parse a query param.
                    arg = self._parse_param(argd, query)

                else:
                    # Remote the first part from the path,
                    # and parse an argument from it.
                    if not parts:
                        raise RestException('Wrong number of arguments. The method is %r'
                                            % method.name, httplib.NOT_FOUND)

                    arg = self._parse_path_argument(argd, parts.pop(0))

                args[argd.name] = arg

            # Create a next invocation in a chain with the parsed arguments.
            invocation = invocation.next(method, **args)

            if method.is_remote:
                # It's the last method which returns a data type.
                # Stop parsing.

                if parts:
                    # Cannot have any more parts here, bad url.
                    raise RestException('Reached a remote method which returns a data type '
                                        'or is void. Cannot have any more path parts. '
                                        'The method is %r' % method.name, httplib.NOT_FOUND)

                return invocation

            # It's an interface method.
            # Get the next interface and proceed parsing the parts.
            descriptor = method.result

        # The parts are empty but we failed to get a remote invocation
        # (the last invocation in a chain, which returns a data type).
        raise RestException('The last method must be a remote one. '
                            'It must return a data type or be void.', httplib.NOT_FOUND)

    def _parse_path_argument(self, argd, s):
        s = self._urldecode(s)
        return self._parse_from_json(argd.type, s)

    def _parse_param(self, argd, src):
        descriptor = argd.type
        is_form = descriptor.is_message and descriptor.is_form

        if not is_form:
            # Parse a single json string param.
            serialized = src.get(argd.name)
            return self._parse_from_json(argd.type, serialized)

        # It's a form. Parse each field as a param.
        # Mind polymorphic messages.

        descriptor = argd.type
        if descriptor.is_polymorphic:
            # Parse the discriminator field and get the subtype descriptor.
            field = descriptor.discriminator
            serialized = src.get(field.name)
            parsed = self._parse_from_json(field.type, serialized)
            descriptor = descriptor.find_subtype(parsed)

        message = descriptor.pyclass()
        for field in descriptor.fields:
            serialized = src.get(field.name)
            if serialized is None:
                continue

            value = self._parse_from_json(field.type, serialized)
            field.set(message, value)

        return message

    def _parse_from_json(self, descriptor, s):
        if s is None:
            return None
        if descriptor.type == pdef.Type.STRING:
            s = '"' + s + '"'
        return pdef.json.parse(s, descriptor)

    # InvocationResult serialization.

    def serialize_invocation_result(self, invocation_result, datad, excd=None):
        '''Serialize an InvocationResult into a RestResponse.'''
        result_class = self._result_class(datad, excd)
        result = result_class(success=invocation_result.success,
                              data=invocation_result.data,
                              exc=invocation_result.exc)

        content = result.to_json(indent=True)
        return RestResponse(status=httplib.OK, content=content, content_type=JSON_CONTENT_TYPE)

    def _result_class(self, datad, excd=None):
        '''Create a runtime rest result class with the given data and exception fields.'''
        fields = [pdef.descriptors.field('success', pdef.descriptors.bool0),
                  pdef.descriptors.field('data', datad)]
        if excd:
            fields.append(pdef.descriptors.field('exc', excd))

        class RestResult(pdef.Message):
            def __init__(self, success=False, data=None, exc=None):
                self.success = success
                self.data = data
                self.exc = exc
            DESCRIPTOR = pdef.descriptors.message(lambda: RestResult, fields=fields)

        return RestResult

    def _urlencode(self, s):
        return urllib.quote(s.encode('utf-8'), safe='[],{}-')

    def _urldecode(self, s):
        return urllib.unquote(s).decode('utf-8')


class RestClient(object):
    def __init__(self, sender):
        '''Create a rest client.'''
        self.sender = sender
        self.protocol = RestProtocol()

    def __call__(self, invocation):
        return self.invoke(invocation)

    def invoke(self, invocation):
        '''Serialize an invocation, send a request, parse a response and return the result.'''
        request = self.protocol.serialize_invocation(invocation)
        response = self.sender(request)

        if response.is_ok and response.is_application_json:
            datad = invocation.result
            excd = invocation.exc
            return self.protocol.parse_invocation_result(response, datad, excd)

        raise self._rest_error(response)

    def _rest_error(self, response):
        '''Create a RestError from a RestResponse.'''
        status = response.status
        text = response.content or 'No text'
        text = text if len(text) < 255 else text[:255]  # Limit the length for the exception.
        return RestException(text, status)


class RestSession(object):
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


class RestServer(object):
    def __init__(self, descriptor, invoker):
        '''Create a WSGI server.'''
        self.descriptor = descriptor
        self.invocation_handler = invoker
        self.protocol = RestProtocol()

    def __call__(self, request):
        return self.handle(request)

    def handle(self, request):
        try:
            invocation = self.protocol.parse_invocation(request, self.descriptor)
        except RestException as e:
            return self._error_response(e)

        datad = invocation.result
        excd = invocation.exc

        invocation_result = self.invocation_handler(invocation)
        return self.protocol.serialize_invocation_result(invocation_result, datad, excd)

    def _error_response(self, e):
        '''Serialize a RestException into an error RestResponse.'''
        status = e.status or httplib.INTERNAL_SERVER_ERROR
        return RestResponse(status, content=e.message, content_type=TEXT_CONTENT_TYPE)


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
