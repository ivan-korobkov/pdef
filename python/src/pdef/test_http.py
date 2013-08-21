# encoding: utf-8
import unittest


class TestHttpClient(unittest.TestCase):
    def test_create_request(self):
        pass

    def test_create_request__post(self):
        pass

    # serialize_invocation.

    def test_serialize_invocation__index_method(self):
        pass

    def test_serialize_invocation__remote_post_method(self):
        pass

    def test_serialize_invocation__remote_method(self):
        pass

    def test_serialize_invocation__interface_method(self):
        pass

    # serialize_arg.

    def test_serialize_arg__primitive(self):
        pass

    def test_serialize_arg__enum(self):
        pass

    def test_serialize_arg__message(self):
        pass

    # send_request

    def test_send_request(self):
        pass

    # parse_response

    def test_parse_response__empty_client_error(self):
        pass

    def test_parse_response__empty_method_not_found(self):
        pass

    def test_parse_response__empty_network_error(self):
        pass

    def test_parse_response__empty_server_error(self):
        pass

    def test_parse_response__ok(self):
        pass

    def test_parse_response__exception(self):
        pass

    def test_parse_response__network_error(self):
        pass

    def test_parse_response__client_error(self):
        pass

    def test_parse_response__method_not_found(self):
        pass

    def test_parse_response__wrong_method_args(self):
        pass
