# encoding: utf-8
import unittest
import requests
from StringIO import StringIO

from pdef import descriptors
from pdef.rest import RestClient, RESPONSE_CONTENT_TYPE
from pdef.test.messages_pd import SimpleMessage
from pdef.test.interfaces_pd import TestInterface, TestException

from pdef.rpc_pd import RpcResponse, RpcStatus, ServerError, NetworkError, \
    ClientError, MethodNotFoundError, WrongMethodArgsError


class TestRestClient(unittest.TestCase):
    # Fixture methods.

    def proxy(self):
        return TestInterface.create_proxy_client(lambda invocation: invocation)

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

        print request.url
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

    # serialize_arg.

    def test_serialize_arg__primitive(self):
        arg = descriptors.arg('arg', lambda: descriptors.int32)

        result = self.client()._serialize_arg(arg, 123)
        assert result == '123'

    def test_serialize_arg__string(self):
        arg = descriptors.arg('arg', lambda: descriptors.string)

        result = self.client()._serialize_arg(arg, u'привет+ромашки')
        assert result == u'привет+ромашки'

    def test_serialize_arg__message(self):
        arg = descriptors.arg('arg', lambda: SimpleMessage.__descriptor__)
        msg = SimpleMessage(aString='hello', aBool=False, anInt16=256)

        result = self.client()._serialize_arg(arg, msg)
        assert result == '{"aBool": false, "anInt16": 256, "aString": "hello"}'

    def test_serialize_positional_arg__quote(self):
        arg = descriptors.arg('arg', lambda: descriptors.string)

        result = self.client()._serialize_positional_arg(arg, u'привет')
        assert result == '%D0%BF%D1%80%D0%B8%D0%B2%D0%B5%D1%82'

    def test_serialize_positional_arg__quote_json(self):
        arg = descriptors.arg('arg', lambda: descriptors.map0(descriptors.int32, descriptors.int32))

        result = self.client()._serialize_positional_arg(arg, {1: 2})
        assert result == '{%221%22%3A%202}'

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
        invocation = self.proxy().queryMethod(msg)

        text = RpcResponse(status=RpcStatus.OK, result=msg).to_json()
        response = self.response(200, text)

        result = self.client()._parse_response(response, invocation)
        assert result == msg

    def test_parse_response__exception(self):
        exc = TestException(text='Application exception!')
        invocation = self.proxy().excMethod()

        text = RpcResponse(status=RpcStatus.EXCEPTION, result=exc).to_json()
        response = self.response(200, text)

        try:
            self.client()._parse_response(response, invocation)
            self.fail('TestException is not raised')
        except TestException, e:
            assert e == exc
