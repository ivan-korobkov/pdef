# encoding: utf-8
from threading import Thread
import unittest
import urllib
from mock import Mock
import requests
from StringIO import StringIO

from pdef import descriptors
from pdef.rest import RestClient, RESPONSE_CONTENT_TYPE, RestServerRequest, \
    ERROR_RESPONSE_CONTENT_TYPE, WsgiRestServer, RestServerResponse
from pdef.test.messages_pd import SimpleMessage
from pdef.test.interfaces_pd import TestInterface, TestException, NextTestInterface

from pdef.rpc_pd import RpcResponse, RpcStatus, ServerError, NetworkError, \
    ClientError, MethodNotFoundError, WrongMethodArgsError, MethodNotAllowedError


class TestRestClient(unittest.TestCase):
    # Fixture methods.

    def proxy(self):
        return TestInterface.create_proxy(lambda invocation: invocation)

    def client(self):
        return RestClient('http://example.com/')

    def response(self, status_code, text=None, content_type=RESPONSE_CONTENT_TYPE):
        r = requests.Response()
        r.status_code = status_code
        if text:
            r._content = text
        if content_type:
            r.headers['content-type'] = content_type

        return r

    # Tests.

    def test_create_request(self):
        invocation = self.proxy().indexMethod(a=1, b=2)
        request = self.client()._create_request(invocation)

        assert request.method == 'GET'
        assert request.url == 'http://example.com/'
        assert request.params == {'a': '1', 'b': '2'}
        assert request.data == {}

    def test_create_request__post(self):
        invocation = self.proxy().postMethod(aList=[1, 2, 3], aMap={1: 2})
        request = self.client()._create_request(invocation)

        assert request.method == 'POST'
        assert request.url == 'http://example.com/postMethod'
        assert request.params == {}
        assert request.data == {'aList': '[1, 2, 3]', 'aMap': '{"1": 2}'}

    def test_create_request__chained_methods(self):
        invocation = self.proxy().interfaceMethod(1, 2).stringMethod('hello')
        request = self.client()._create_request(invocation)

        assert request.method == 'GET'
        assert request.url == 'http://example.com/interfaceMethod/1/2/stringMethod'
        assert request.params == {'text': 'hello'}
        assert request.data == {}

    # serialize_invocation.

    def test_serialize_invocation__index_method(self):
        invocation = self.proxy().indexMethod(a=1, b=2)
        path, query, post = self.client()._serialize_invocation(invocation)

        assert path == '/'
        assert query == {'a': '1', 'b': '2'}
        assert post == {}

    def test_serialize_invocation__post_method(self):
        invocation = self.proxy().postMethod(aList=[1, 2, 3], aMap={1: 2})
        path, query, post = self.client()._serialize_invocation(invocation)

        assert path == '/postMethod'
        assert query == {}
        assert post == {'aList': '[1, 2, 3]', 'aMap': '{"1": 2}'}

    def test_serialize_invocation__remote_method(self):
        invocation = self.proxy().remoteMethod(a=10, b=100)
        path, query, post = self.client()._serialize_invocation(invocation)

        assert path == '/remoteMethod'
        assert query == {'a': '10', 'b': '100'}
        assert post == {}

    def test_serialize_invocation__interface_method(self):
        invocation = self.proxy().interfaceMethod(a=1, b=2)._invocation
        path, query, post = self.client()._serialize_invocation(invocation)

        assert path == '/interfaceMethod/1/2'
        assert query == {}
        assert post == {}

    # Serializing arguments.

    def test_serialize_positional_arg(self):
        arg = descriptors.arg('arg', lambda: descriptors.string)

        value = self.client()._serialize_positional_arg(arg, u'Привет')
        assert value == '%D0%9F%D1%80%D0%B8%D0%B2%D0%B5%D1%82'

    def test_serialize_query_arg(self):
        arg = descriptors.arg('arg', lambda: descriptors.int32)

        dst = {}
        self.client()._serialize_query_arg(arg, 123, dst)
        assert dst == {'arg': '123'}

    def test_serialize_query_arg__form(self):
        arg = descriptors.arg('arg', lambda: SimpleMessage.__descriptor__)

        dst = {}
        msg = SimpleMessage(aString=u'Привет', aBool=False)
        self.client()._serialize_query_arg(arg, msg, dst)

        assert dst == {'aString': u'Привет', 'aBool': 'False'}

    def test_serialize_arg_to_string__primitive(self):
        descriptor = descriptors.int32
        result = self.client()._serialize_arg_to_string(descriptor, 123)

        assert result == '123'

    def test_serialize_arg_to_string__primitive_none_to_empty_string(self):
        descriptor = descriptors.int32
        result = self.client()._serialize_arg_to_string(descriptor, None)

        assert result == ''

    def test_serialize_arg_to_string__string(self):
        descriptor = descriptors.string
        result = self.client()._serialize_arg_to_string(descriptor, u'привет+ромашки')

        assert result == u'привет+ромашки'

    def test_serialize_arg_to_string__message(self):
        descriptor = SimpleMessage.__descriptor__
        msg = SimpleMessage(aString='hello', aBool=False, anInt16=256)
        result = self.client()._serialize_arg_to_string(descriptor, msg)

        assert result == '{"aBool": false, "anInt16": 256, "aString": "hello"}'

    # parse_response

    def test_parse_response__empty_client_error(self):
        response = self.response(400)
        self.assertRaises(ClientError, self.client()._parse_response, response, None)

    def test_parse_response__empty_method_not_found(self):
        response = self.response(404)
        self.assertRaises(MethodNotFoundError, self.client()._parse_response, response, None)

    def test_parse_response__empty_network_error(self):
        response = self.response(502)
        self.assertRaises(NetworkError, self.client()._parse_response, response, None)

        response = self.response(503)
        self.assertRaises(NetworkError, self.client()._parse_response, response, None)

    def test_parse_response__empty_server_error(self):
        response = self.response(500)
        self.assertRaises(ServerError, self.client()._parse_response, response, None)

    def test_parse_response__ok(self):
        msg = SimpleMessage(aString='hello', aBool=False, anInt16=127)
        text = RpcResponse(status=RpcStatus.OK, result=msg).to_json()
        response = self.response(200, text)

        invocation = self.proxy().formMethod(msg)
        status, result = self.client()._parse_response(response, invocation)

        assert status == RpcStatus.OK
        assert result == msg

    def test_parse_response__exception(self):
        exc = TestException(text='Application exception!')
        text = RpcResponse(status=RpcStatus.EXCEPTION, result=exc).to_json()
        response = self.response(200, text)

        invocation = self.proxy().excMethod()
        status, result = self.client()._parse_response(response, invocation)

        assert status == RpcStatus.EXCEPTION
        assert result == exc


class TestRestServer(unittest.TestCase):
    def server(self):
        service = TestInterface()
        return service.to_rest_server()

    def proxy(self):
        return TestInterface.create_proxy(lambda invocation: invocation)

    def get_request(self, path, query=None, post=None):
        return RestServerRequest('GET', path, query=query, post=post)

    def post_request(self, path, query=None, post=None):
        return RestServerRequest('POST', path, query=query, post=post)

    def test_handle(self):
        pass

    def test_parse_request__index_method(self):
        request = self.get_request('/', query={'a': '123', 'b': '456'})

        invocation = self.server()._parse_request(request)
        assert invocation.method.name == 'indexMethod'
        assert invocation.args == {'a': 123, 'b': 456}

    def test_parse_request__post_method(self):
        request = self.post_request('/postMethod', post={'aList': '[1, 2, 3]', 'aMap': '{"1":2}'},)

        invocation = self.server()._parse_request(request)
        assert invocation.method.name == 'postMethod'
        assert invocation.args == {'aList': [1, 2, 3], 'aMap': {1: 2}}

    def test_parse_request__post_method_not_allowed(self):
        request = self.get_request('/postMethod', post={})

        self.assertRaises(MethodNotAllowedError, self.server()._parse_request, request)

    def test_parse_request__remote_method(self):
        request = self.get_request('/remoteMethod', query={'a': '1', 'b': '2'})

        invocation = self.server()._parse_request(request)
        assert invocation.method.name == 'remoteMethod'
        assert invocation.args == {'a': 1, 'b': 2}

    def test_parse_request__chained_method_index(self):
        request = self.get_request('/interfaceMethod/1/2/')

        chain = self.server()._parse_request(request).to_chain()
        invocation0 = chain[0]
        invocation1 = chain[1]

        assert len(chain) == 2
        assert invocation0.method.name == 'interfaceMethod'
        assert invocation0.args == {'a': 1, 'b': 2}
        assert invocation1.method.name == 'indexMethod'
        assert invocation1.args == {}

    def test_parse_request__chained_method_remote(self):
        request = self.get_request('/interfaceMethod/1/2/stringMethod', query={'text': u'привет'})

        chain = self.server()._parse_request(request).to_chain()
        invocation0 = chain[0]
        invocation1 = chain[1]

        assert len(chain) == 2
        assert invocation0.method.name == 'interfaceMethod'
        assert invocation0.args == {'a': 1, 'b': 2}
        assert invocation1.method.name == 'stringMethod'
        assert invocation1.args == {'text': u'привет'}

    def test_parse_request__interface_method_not_remote(self):
        request = self.get_request('/interfaceMethod/1/2')

        self.assertRaises(MethodNotFoundError, self.server()._parse_request, request)

    # Parsing args.

    def test_parse_positional_arg(self):
        arg = descriptors.arg('arg', lambda: descriptors.string)
        part = '%D0%9F%D1%80%D0%B8%D0%B2%D0%B5%D1%82'

        value = self.server()._parse_positional_arg(arg, part)
        assert value == u'Привет'

    def test_parse_query_arg__form(self):
        arg = descriptors.arg('arg', lambda: SimpleMessage.__descriptor__)

        msg = SimpleMessage(aString=u'Привет', aBool=True, anInt16=7)
        src = {'aString': u'Привет', 'aBool': 'true', 'anInt16': '7'}

        msg0 = self.server()._parse_query_arg(arg, src)
        assert msg0 == msg

    def test_parse_query_arg__primitive(self):
        arg = descriptors.arg('arg', lambda: descriptors.int32)
        src = {'arg': '123'}

        value = self.server()._parse_query_arg(arg, src)
        assert value == 123

    def test_parse_arg_from_string__primitive(self):
        descriptor = descriptors.int32

        value = self.server()._parse_arg_from_string(descriptor, '123')
        assert value == 123

    def test_parse_arg_from_string__primitive_empty_to_none(self):
        descriptor = descriptors.int32

        value = self.server()._parse_arg_from_string(descriptor, '')
        assert value is None

    def test_parse_arg_from_string__string(self):
        descriptor = descriptors.string

        value = self.server()._parse_arg_from_string(descriptor, u'привет, мир?')
        assert value == u'привет, мир?'

    def test_parse_arg_from_string__message(self):
        descriptor = SimpleMessage.__descriptor__

        msg = SimpleMessage(aString=u'привет', aBool=True, anInt16=123)
        value = msg.to_json()

        msg0 = self.server()._parse_arg_from_string(descriptor, value)
        assert msg0 == msg

    # Results and responses.

    def test_result_to_response(self):
        invocation = self.proxy().formMethod()

        msg = SimpleMessage(aString=u'привет', aBool=False, anInt16=0)
        response = self.server()._result_to_response(msg, invocation)

        assert response == RpcResponse(status=RpcStatus.OK, result=msg.to_dict())

    def test_app_exc_to_response__expected(self):
        invocation = self.proxy().excMethod()

        exc = TestException(u'Привет, мир')
        response = self.server()._app_exc_to_response(exc, invocation)

        assert response == RpcResponse(status=RpcStatus.EXCEPTION, result=exc.to_dict())

    def test_app_exc_to_response__unexpected(self):
        invocation = self.proxy().excMethod()

        exc = TestException()
        response = self.server()._app_exc_to_response(ValueError(), invocation)

        assert response is None

    def test_rest_response__ok(self):
        response = RpcResponse(status=RpcStatus.OK, result='Hello, world!')
        resp = self.server()._rest_response(response)

        assert resp.status == 200
        assert resp.content_type == RESPONSE_CONTENT_TYPE
        assert resp.content == response.to_json()
        assert resp.content_length == len(resp.content)

    def test_rest_response_exception(self):
        response = RpcResponse(status=RpcStatus.OK, result=TestException().to_dict())
        resp = self.server()._rest_response(response)

        assert resp.status == 200
        assert resp.content_type == RESPONSE_CONTENT_TYPE
        assert resp.content == response.to_json()
        assert resp.content_length == len(resp.content)

    def test_error_response__wrong_method_args(self):
        e = WrongMethodArgsError(u'Неправильные аргументы')
        resp = self.server()._error_rest_response(e)

        assert resp.status == 400
        assert resp.content == e.text.encode('utf-8')
        assert resp.content_type == ERROR_RESPONSE_CONTENT_TYPE

    def test_error_response__method_not_found(self):
        e = MethodNotFoundError(u'Метод не найден')
        resp = self.server()._error_rest_response(e)

        assert resp.status == 404
        assert resp.content == e.text.encode('utf-8')

    def test_error_response__method_not_allowed(self):
        e = MethodNotAllowedError(u'HTTP метод запрещен')
        resp = self.server()._error_rest_response(e)

        assert resp.status == 405
        assert resp.content == e.text.encode('utf-8')

    def test_error_response__client_error(self):
        e = ClientError(u'Ошибка клиента')
        resp = self.server()._error_rest_response(e)

        assert resp.status == 400
        assert resp.content == e.text.encode('utf-8')

    def test_error_response__network_error(self):
        e = NetworkError(u'Сетевая ошибка')
        resp = self.server()._error_rest_response(e)

        assert resp.status == 503
        assert resp.content == e.text.encode('utf-8')

    def test_error_response__server_error(self):
        e = ServerError(u'Ошибка сервера')
        resp = self.server()._error_rest_response(e)

        assert resp.status == 500
        assert resp.content == e.text.encode('utf-8')

    def test_error_response__internal_server_error(self):
        e = ValueError('Unhandled exception')
        resp = self.server()._error_rest_response(e)

        assert resp.status == 500
        assert resp.content == 'Internal server error'

class TestWsgiRestServer(unittest.TestCase):
    def env(self):
        return {
            'REQUEST_METHOD': 'GET',
            'CONTENT_TYPE': 'application/x-www-form-urlencoded',
            'CONTENT_LENGTH': 0,
            'SCRIPT_NAME': '/myapp',
            'PATH_INFO': '/method0/method1'
        }

    def test_handle(self):
        hello = u'Привет, мир'
        response = RestServerResponse(status=200, content=hello, content_type='text/plain')
        start_response = Mock()

        server = WsgiRestServer(lambda x: response)
        content = ''.join(server.handle(self.env(), start_response))

        assert content.decode('utf-8') == hello
        start_response.assert_called_with('200 OK',
            [('Content-Type', 'text/plain'), ('Content-Length', '%s' % len(content))])

    def test_parse_request(self):
        query = urllib.quote(u'привет=мир'.encode('utf-8'), '=')
        body = urllib.quote(u'пока=мир'.encode('utf-8'), '=')
        env = {
            'REQUEST_METHOD': 'POST',
            'CONTENT_TYPE': 'application/x-www-form-urlencoded',
            'CONTENT_LENGTH': len(body),
            'SCRIPT_NAME': '/myapp',
            'PATH_INFO': '/method0/method1',
            'QUERY_STRING': query,
            'wsgi.input': StringIO(body),
            }

        server = WsgiRestServer(None)
        request = server._parse_request(env)

        assert request.method == 'POST'
        assert request.path == '/myapp/method0/method1'
        assert request.query == {u'привет': u'мир'}
        assert request.post == {u'пока': u'мир'}


class TestIntegration(unittest.TestCase):
    def setUp(self):
        from wsgiref.simple_server import make_server
        service = IntegrationService()
        app = service.to_wsgi_server()

        self.server = make_server('localhost', 0, app)
        self.server_thread = Thread(target=self.server.serve_forever)
        self.server_thread.start()

        import logging
        FORMAT = '%(name)s %(levelname)s - %(message)s'
        logging.basicConfig(level=logging.DEBUG, format=FORMAT)

    def tearDown(self):
        self.server.shutdown()

    def client(self):
        url = 'http://localhost:%s/' % self.server.server_port
        return TestInterface.create_rest_client(url)

    def test(self):
        client = self.client()
        form = SimpleMessage(u'Привет', True, 0)

        assert client.indexMethod(1, 2) == 3
        assert client.remoteMethod(10, 2) == '5'
        assert client.postMethod([1, 2, 3], {4: 5}) == [1, 2, 3, 4, 5]
        assert client.formMethod(form) == form
        assert client.voidMethod() is None
        assert client.stringMethod(u'Как дела?') == u'Как дела?'
        assert client.interfaceMethod(1, 2).indexMethod() == 'chained call 1 2'
        self.assertRaises(TestException, client.excMethod)


class IntegrationService(TestInterface):
    def indexMethod(self, a=None, b=None):
        return a + b

    def remoteMethod(self, a=None, b=None):
        return a / b

    def postMethod(self, aList=None, aMap=None):
        return list(aList) + aMap.keys() + aMap.values()

    def formMethod(self, msg=None):
        return msg

    def voidMethod(self):
        return 'void?' # But should send None.

    def excMethod(self):
        raise TestException('Application Exception!')

    def stringMethod(self, text=None):
        return text

    def interfaceMethod(self, a=None, b=None):
        class Next(NextTestInterface):
            def indexMethod(self):
                return 'chained call %s %s' % (a, b)

        return Next()
