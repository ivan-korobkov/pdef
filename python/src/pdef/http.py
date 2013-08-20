# encoding: utf-8
import requests
import urllib
from rpc_pd import RpcResponse, RpcResponseStatus, RpcError, ServerError, ClientError, NetworkError


class HttpClient(object):
    CONTENT_TYPE = 'application/json'

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
        result = self._parse_response(response, invocation.method)
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
        if response.headers['Content-Type'] == self.CONTENT_TYPE:
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
                serialized = arg.type.to_json(value)
                post[arg.name] = serialized

        elif method.is_remote:
            # Add arguments as query params.
            for arg in method.args:
                value = args.get(arg.name)
                serialized = arg.type.to_string(value)
                query[arg.name] = serialized
        else:
            # Positionally prepend all arguments to the path.
            for arg in method.args:
                assert arg.type.is_primitive, 'Arguments must be primitives, method=%s' % method
                value = args.get(arg.name)
                serialized = arg.type.to_string(value)
                path += '/' + urllib.quote(serialized)

        return path
