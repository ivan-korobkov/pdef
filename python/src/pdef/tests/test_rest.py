# encoding: utf-8
import httplib
import unittest
import urllib
from mock import Mock
from StringIO import StringIO
from threading import Thread

import pdef.invocation
import pdef.descriptors
import pdef.rest
from pdef.rest import *
from pdef_test.messages import SimpleMessage, SimpleForm
from pdef_test.interfaces import TestInterface, TestException, NextTestInterface


class TestRestClientHandler(unittest.TestCase):
    # Fixture methods.

    def proxy(self):
        handler = lambda inv: pdef.invocation.InvocationResult(inv)
        return pdef.invocation.proxy(TestInterface, handler)

    def handler(self):
        sender = lambda inv: inv.InvocationResult(inv)
        return pdef.rest.client_handler(sender)

    def response(self, status_code, content=None, content_type=JSON_CONTENT_TYPE):
        return RestResponse(status_code, content=content, content_type=content_type)

    # Tests.

    def test_create_request(self):
        invocation = self.proxy().indexMethod(a=1, b=2)
        request = self.handler()._create_request(invocation)

        assert request.method == 'GET'
        assert request.path == '/'
        assert request.query == {'a': '1', 'b': '2'}
        assert request.post == {}

    def test_create_request__post(self):
        invocation = self.proxy().postMethod(aList=[1, 2, 3], aMap={1: 2})
        request = self.handler()._create_request(invocation)

        assert request.method == 'POST'
        assert request.path == '/postMethod'
        assert request.query == {}
        assert request.post == {'aList': '[1, 2, 3]', 'aMap': '{"1": 2}'}

    def test_create_request__chained_methods(self):
        invocation = self.proxy().interfaceMethod(1, 2).stringMethod('hello')
        request = self.handler()._create_request(invocation)

        assert request.method == 'GET'
        assert request.path == '/interfaceMethod/1/2/stringMethod'
        assert request.query == {'text': 'hello'}
        assert request.post == {}

    # serialize_invocation.

    def test_serialize_invocation__index_method(self):
        request = RestRequest()
        invocation = self.proxy().indexMethod(a=1, b=2)
        self.handler()._serialize_invocation(invocation, request)

        assert request.path == '/'
        assert request.query == {'a': '1', 'b': '2'}
        assert request.post == {}

    def test_serialize_invocation__post_method(self):
        request = RestRequest()
        invocation = self.proxy().postMethod(aList=[1, 2, 3], aMap={1: 2})
        self.handler()._serialize_invocation(invocation, request)

        assert request.path == '/postMethod'
        assert request.query == {}
        assert request.post == {'aList': '[1, 2, 3]', 'aMap': '{"1": 2}'}

    def test_serialize_invocation__remote_method(self):
        request = RestRequest()
        invocation = self.proxy().remoteMethod(a=10, b=100)
        self.handler()._serialize_invocation(invocation, request)

        assert request.path == '/remoteMethod'
        assert request.query == {'a': '10', 'b': '100'}
        assert request.post == {}

    def test_serialize_invocation__interface_method(self):
        request = RestRequest()
        invocation = self.proxy().interfaceMethod(a=1, b=2)._invocation
        self.handler()._serialize_invocation(invocation, request)

        assert request.path == '/interfaceMethod/1/2'
        assert request.query == {}
        assert request.post == {}

    # Serializing arguments.

    def test_serialize_positional_arg(self):
        arg = descriptors.arg('arg', lambda: descriptors.string)

        value = self.handler()._serialize_positional_arg(arg, u'Привет')
        assert value == '%D0%9F%D1%80%D0%B8%D0%B2%D0%B5%D1%82'

    def test_serialize_query_arg(self):
        arg = descriptors.arg('arg', lambda: descriptors.int32)

        dst = {}
        self.handler()._serialize_query_arg(arg, 123, dst)
        assert dst == {'arg': '123'}

    def test_serialize_query_arg__form(self):
        arg = descriptors.arg('arg', lambda: SimpleForm.__descriptor__)

        dst = {}
        form = SimpleForm(text=u'Привет', numbers=[1, 2, 3], flag=False)
        self.handler()._serialize_query_arg(arg, form, dst)

        assert dst == {'text': u'Привет', 'numbers': '[1, 2, 3]', 'flag': 'false'}

    def test_serialize_arg_to_string__primitive(self):
        descriptor = descriptors.int32
        result = self.handler()._serialize_arg_to_string(descriptor, 123)

        assert result == '123'

    def test_serialize_arg_to_string__primitive_none_to_empty_string(self):
        descriptor = descriptors.int32
        result = self.handler()._serialize_arg_to_string(descriptor, None)

        assert result == ''

    def test_serialize_arg_to_string__string(self):
        descriptor = descriptors.string
        result = self.handler()._serialize_arg_to_string(descriptor, u'привет+ромашки')

        assert result == u'привет+ромашки'

    def test_serialize_arg_to_string__message(self):
        descriptor = SimpleMessage.__descriptor__
        msg = SimpleMessage(aString='hello', aBool=False, anInt16=256)
        result = self.handler()._serialize_arg_to_string(descriptor, msg)

        assert result == '{"aBool": false, "anInt16": 256, "aString": "hello"}'

    # parse_result

    def test_parse_result__ok(self):
        msg = SimpleMessage(aString='hello', aBool=False, anInt16=127)
        text = RpcResult(status=RpcStatus.OK, data=msg).to_json()
        response = self.response(200, text)

        invocation = self.proxy().messageMethod(msg)
        result = self.handler()._parse_result(response, invocation)

        assert result.ok
        assert result.data == msg

    def test_parse_result__exc(self):
        exc = TestException(text='Application exception!')
        text = RpcResult(status=RpcStatus.EXCEPTION, data=exc).to_json()
        response = self.response(200, text)

        invocation = self.proxy().excMethod()
        result = self.handler()._parse_result(response, invocation)

        assert result.ok is False
        assert result.data == exc

    # parse_raise_error

    def test_parse_response__empty_client_error(self):
        response = self.response(400)
        self.assertRaises(ClientError, self.handler()._parse_raise_error, response)

    def test_parse_response__empty_method_not_found(self):
        response = self.response(404)
        self.assertRaises(MethodNotFoundError, self.handler()._parse_raise_error, response)

    def test_parse_response__empty_network_error(self):
        response = self.response(502)
        self.assertRaises(ServiceUnavailableError, self.handler()._parse_raise_error, response)

        response = self.response(503)
        self.assertRaises(ServiceUnavailableError, self.handler()._parse_raise_error, response)

    def test_parse_response__empty_server_error(self):
        response = self.response(500)
        self.assertRaises(ServerError, self.handler()._parse_raise_error, response)


class TestRestServerHandler(unittest.TestCase):
    def server(self):
        invoker = lambda inv: pdef.invocation.InvocationResult(inv)
        return server_handler(TestInterface, invoker)

    def proxy(self):
        handler = lambda inv: pdef.invocation.InvocationResult(inv)
        return pdef.invocation.proxy(TestInterface, handler)

    def get_request(self, path, query=None, post=None):
        return RestRequest(GET, path, query=query, post=post)

    def post_request(self, path, query=None, post=None):
        return RestRequest(POST, path, query=query, post=post)

    def test_handle(self):
        class Service(TestInterface):
            def indexMethod(self, a=None, b=None):
                return a + b

        request = self.get_request('/', query={'a': '1', 'b': '2'})
        server = pdef.rest.server(TestInterface, Service)
        response = server.handle(request)

        assert response.status == httplib.OK
        assert response.content_type == JSON_CONTENT_TYPE
        assert response.content == RpcResult(status=RpcStatus.OK, data=3).to_json(True)

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
        arg = descriptors.arg('arg', lambda: SimpleForm.__descriptor__)

        form = SimpleForm(text=u'Привет', numbers=[1, 2, 3], flag=True)
        src = {'text': u'Привет', 'numbers': '[1,2,3]', 'flag': 'true'}

        result = self.server()._parse_query_arg(arg, src)
        assert result == form

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

    # ok_response.

    def test_ok_response(self):
        invocation = self.proxy().messageMethod()

        msg = SimpleMessage(aString=u'привет', aBool=False, anInt16=0)
        result = pdef.invocation.InvocationResult(msg)
        response = self.server()._ok_response(result, invocation)

        assert response.status == httplib.OK
        assert response.content_type == JSON_CONTENT_TYPE
        assert response.content == RpcResult(status=RpcStatus.OK, data=msg).to_json(True)

    def test_ok_response_exc(self):
        invocation = self.proxy().excMethod()

        exc = TestException(u'Привет, мир')
        result = pdef.invocation.InvocationResult(exc, ok=False)
        response = self.server()._ok_response(result, invocation)

        assert response.status == httplib.OK
        assert response.content_type == JSON_CONTENT_TYPE
        assert response.content == RpcResult(status=RpcStatus.EXCEPTION, data=exc).to_json(True)

    # error_response.

    def test_error_response__wrong_method_args(self):
        e = WrongMethodArgsError(u'Неправильные аргументы')
        resp = self.server()._error_response(e)

        assert resp.status == 400
        assert resp.content == e.text
        assert resp.content_type == TEXT_CONTENT_TYPE

    def test_error_response__method_not_found(self):
        e = MethodNotFoundError(u'Метод не найден')
        resp = self.server()._error_response(e)

        assert resp.status == 404
        assert resp.content == e.text

    def test_error_response__method_not_allowed(self):
        e = MethodNotAllowedError(u'HTTP метод запрещен')
        resp = self.server()._error_response(e)

        assert resp.status == 405
        assert resp.content == e.text

    def test_error_response__client_error(self):
        e = ClientError(u'Ошибка клиента')
        resp = self.server()._error_response(e)

        assert resp.status == 400
        assert resp.content == e.text

    def test_error_response__service_unavailable_error(self):
        e = ServiceUnavailableError(u'Сетевая ошибка')
        resp = self.server()._error_response(e)

        assert resp.status == 503
        assert resp.content == e.text

    def test_error_response__server_error(self):
        e = ServerError(u'Ошибка сервера')
        resp = self.server()._error_response(e)

        assert resp.status == 500
        assert resp.content == e.text

    def test_error_response__internal_server_error(self):
        e = ValueError('Unhandled exception')
        resp = self.server()._error_response(e)

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
        response = RestResponse(status=200, content=hello, content_type='text/plain')
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
        assert request.path == '/method0/method1'
        assert request.query == {u'привет': u'мир'}
        assert request.post == {u'пока': u'мир'}


class TestIntegration(unittest.TestCase):
    def setUp(self):
        from wsgiref.simple_server import make_server
        service = IntegrationService()
        server = pdef.rest.server(TestInterface, service)
        app = pdef.wsgi_server(server)

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
        return client(TestInterface, url)

    def test(self):
        client = self.client()
        msg = SimpleMessage(u'Привет', True, 0)
        form = SimpleForm(u'Привет', [1, 2, 3], True)

        assert client.indexMethod(1, 2) == 3
        assert client.remoteMethod(10, 2) == 5
        assert client.postMethod([1, 2, 3], {4: 5}) == [1, 2, 3, 4, 5]
        assert client.messageMethod(msg) == msg
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

    def messageMethod(self, msg=None):
        return msg

    def formMethod(self, form=None):
        return form

    def voidMethod(self):
        return 'void?'  # But should send None.

    def excMethod(self):
        raise TestException('Application exception')

    def stringMethod(self, text=None):
        return text

    def interfaceMethod(self, a=None, b=None):
        class Next(NextTestInterface):
            def indexMethod(self):
                return 'chained call %s %s' % (a, b)

        return Next()
