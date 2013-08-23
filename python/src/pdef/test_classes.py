# encoding: utf-8
import unittest
from pdef import test_pd
from pdef import Proxy, Invocation
from pdef.test.interfaces_pd import TestInterface, TestException


class TestMessage(unittest.TestCase):
    JSON = '''{"a": "one", "b": "two"}'''

    def _fixture(self):
        return test_pd.TestMessage(a="one", b="two")

    def _fixture_dict(self):
        return {'a': 'one', 'b': 'two'}

    def test_parse_json(self):
        msg = test_pd.TestMessage.parse_json(self.JSON)
        assert msg == self._fixture()

    def test_parse_dict(self):
        msg = self._fixture()
        d = msg.to_dict()

        msg1 = test_pd.TestMessage.parse_dict(d)
        assert msg == msg1

    def test_to_json(self):
        msg = self._fixture()
        s = msg.to_json()

        msg1 = test_pd.TestMessage.parse_json(s)
        assert msg == msg1

    def test_to_dict(self):
        d = self._fixture().to_dict()

        assert d == self._fixture_dict()

    def test_eq(self):
        msg0 = self._fixture()
        msg1 = self._fixture()
        assert msg0 == msg1

        msg1.a = 'qwer'
        assert msg0 != msg1


class TestProxy(unittest.TestCase):
    def proxy(self):
        return Proxy(TestInterface.__descriptor__, lambda invocation: invocation)

    def test_method_invocation(self):
        subproxy = self.proxy().interfaceMethod(1, 2)

        invocation = subproxy._invocation
        assert invocation.method.name == 'interfaceMethod'
        assert invocation.args == {'a': 1, 'b': 2}

    def test_call_client(self):
        chain = self.proxy().interfaceMethod(1, 2).remoteMethod().to_chain()
        invocation0 = chain[0]
        invocation1 = chain[1]

        assert invocation0.method.name == 'interfaceMethod'
        assert invocation0.args == {'a': 1, 'b': 2}

        assert invocation1.method.name == 'remoteMethod'
        assert invocation1.args == {}


class TestInvocation(unittest.TestCase):
    def method(self):
        return TestInterface.__descriptor__.find_method('indexMethod')

    def interface_method(self):
        return TestInterface.__descriptor__.find_method('interfaceMethod')

    def test_init(self):
        method = self.method()
        invocation = Invocation(method, None, args=[1, 2])

        assert invocation.method is method
        assert invocation.args == {'a': 1, 'b': 2}
        assert invocation.exc is TestException.__descriptor__
        assert invocation.result is method.result

    def test_next(self):
        method = self.method()

        root = Invocation.root()
        invocation = root.next(method, 1, 2)

        assert invocation.parent is root
        assert invocation.method is method
        assert invocation.args == {'a': 1, 'b': 2}

    def test_to_chain(self):
        method0 = self.interface_method()
        method1 = method0.result.methods[0]

        root = Invocation.root()
        invocation0 = root.next(method0)
        invocation1 = invocation0.next(method1)

        chain = invocation1.to_chain()
        assert chain == [invocation0, invocation1]

    def test_build_args(self):
        method = self.method()
        build = lambda args, kwargs: Invocation._build_args(method, args, kwargs)
        expected = {'a': 1, 'b': 2}

        assert build([1, 2], None) == expected
        assert build(None, {'a': 1, 'b': 2}) == expected
        assert build([1], {'b': 2}) == expected
        assert build(None, None) == {'a': None, 'b': None}

        self.assertRaises(TypeError, build, [1, 2, 3], None)
        self.assertRaises(TypeError, build, [1, 2], {'a': 1, 'b': 2})
        self.assertRaises(TypeError, build, None, {'a': 1, 'b': 2, 'c': 3})
        self.assertRaises(TypeError, build, None, {'c': 3})
