# encoding: utf-8
import unittest

from pdef.invocation import *
from pdef_test.interfaces import TestInterface, TestException


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

    def test_init__check_arg_types(self):
        method = self.method()
        self.assertRaises(TypeError, Invocation, method, None, args=[1, 'string'])

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

    def test_invoke(self):
        class Service(TestInterface):
            def indexMethod(self, a=None, b=None):
                return a + b

        method = self.method()
        invocation = Invocation.root().next(method, 1, 2)
        result = invocation.invoke(Service())

        assert result.ok
        assert result.data == 3

    def test_invoke_exc(self):
        class Service(TestInterface):
            def indexMethod(self, a=None, b=None):
                raise TestException('hello')

        method = self.method()
        invocation = Invocation.root().next(method, 1, 2)
        result = invocation.invoke(Service())

        assert result.ok is False
        assert result.data == TestException('hello')


class TestInvocationProxy(unittest.TestCase):
    def proxy(self):
        return proxy(TestInterface, lambda invocation: InvocationResult(invocation))

    def test_invoke_capture(self):
        subproxy = self.proxy().interfaceMethod(1, 2)

        invocation = subproxy._invocation
        assert invocation.method.name == 'interfaceMethod'
        assert invocation.args == {'a': 1, 'b': 2}

    def test_invoke_capture_chain(self):
        chain = self.proxy().interfaceMethod(1, 2).remoteMethod().to_chain()
        invocation0 = chain[0]
        invocation1 = chain[1]

        assert invocation0.method.name == 'interfaceMethod'
        assert invocation0.args == {'a': 1, 'b': 2}

        assert invocation1.method.name == 'remoteMethod'
        assert invocation1.args == {}

    def test_invoke_handle_ok(self):
        client = proxy(TestInterface, lambda inv: InvocationResult(3))
        result = client.indexMethod(1, 2)

        assert result == 3

    def test_invoke_handle_exc(self):
        exc = TestException('hello')
        client = proxy(TestInterface, lambda inv: InvocationResult(exc, ok=False))

        try:
            client.indexMethod(1, 2)
            assert False
        except TestException, e:
            assert e == exc
