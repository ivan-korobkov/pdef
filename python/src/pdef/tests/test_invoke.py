# encoding: utf-8
import unittest

from pdef.invoke import *
from pdef import descriptors
from pdef_test.messages import SimpleMessage
from pdef_test.interfaces import TestInterface, TestException, NextTestInterface


class TestInvocation(unittest.TestCase):
    def test_init(self):
        method = descriptors.method('method', descriptors.void, exc=TestException.DESCRIPTOR,
                                    args=(descriptors.arg('a', descriptors.int32),
                                          descriptors.arg('b', descriptors.int32)))
        invocation = Invocation(method, args=[1, 2])

        assert invocation.method is method
        assert invocation.args == {'a': 1, 'b': 2}
        assert invocation.exc is TestException.DESCRIPTOR
        assert invocation.result is method.result

    def test_next(self):
        method0 = descriptors.method('method0', descriptors.interface(object))
        method1 = descriptors.method('method1', descriptors.void,
                                     args=(descriptors.arg('a', descriptors.int32),
                                           descriptors.arg('b', descriptors.int32)))

        invocation0 = Invocation(method0)
        invocation1 = invocation0.next(method1, 1, 2)

        assert invocation1.parent is invocation0
        assert invocation1.method is method1
        assert invocation1.args == {'a': 1, 'b': 2}

    def test_to_chain(self):
        method0 = descriptors.method('method0', descriptors.interface(object))
        method1 = descriptors.method('method1', descriptors.interface(object))
        method2 = descriptors.method('method2', descriptors.void)

        invocation0 = Invocation(method0)
        invocation1 = invocation0.next(method1)
        invocation2 = invocation1.next(method2)

        chain = invocation2.to_chain()
        assert chain == [invocation0, invocation1, invocation2]

    def test_invoke(self):
        class Service(object):
            def method(self, a=None, b=None):
                return a + b

        method = descriptors.method('method', descriptors.int32,
                                    args=(descriptors.arg('a', descriptors.int32),
                                          descriptors.arg('b', descriptors.int32)))
        invocation = Invocation(method, args=(1, 2))
        service = Service()
        result = invocation.invoke(service)

        assert result.success
        assert result.data == 3
        assert result.exc is None

    def test_invoke_exc(self):
        class Service(object):
            def method(self):
                raise TestException('hello')

        method = descriptors.method('method', descriptors.void, exc=TestException.DESCRIPTOR)
        invocation = Invocation(method)
        service = Service()
        result = invocation.invoke(service)

        assert result.success is False
        assert result.data is None
        assert result.exc == TestException('hello')

    def test_build_args(self):
        method = descriptors.method('method', descriptors.void,
                                    args=(descriptors.arg('a', descriptors.int32),
                                          descriptors.arg('b', descriptors.int32)))
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

    def test_deep_copy_args(self):
        method = descriptors.method('method', descriptors.void,
            args=(descriptors.arg('arg0', descriptors.list0(SimpleMessage.DESCRIPTOR)),
                  descriptors.arg('arg1', descriptors.set0(descriptors.int32))
            ))

        list0 = [SimpleMessage('hello'), SimpleMessage('world')]
        set0 = {1, 2, 3}

        invocation = Invocation(method, args=(list0, set0))
        arg0 = invocation.args['arg0']
        arg1 = invocation.args['arg1']

        assert arg0 == list0
        assert arg1 == set0

        assert arg0 is not list0
        assert arg1 is not set0

        assert arg0[0] is not list0[0]
        assert arg0[1] is not list0[1]


class TestInvocationProxy(unittest.TestCase):
    def proxy(self):
        return proxy(TestInterface, lambda invocation: InvocationResult.ok(invocation))

    def test_ok(self):
        proxy = InvocationProxy(TestInterface.DESCRIPTOR, lambda inv: InvocationResult.ok(3))
        result = proxy.indexMethod(1, 2)

        assert result == 3

    def test_exc(self):
        exc = TestException('hello')
        client = proxy(TestInterface, lambda inv: InvocationResult.exception(exc))

        try:
            client.indexMethod(1, 2)
            self.fail()
        except TestException, e:
            assert e == exc

    def test_proxy_method(self):
        interface = TestInterface.DESCRIPTOR
        method = interface.find_method('indexMethod')
        handler = lambda inv: InvocationResult.ok(None)

        proxy = InvocationProxy(interface, handler)
        proxy_method = proxy.indexMethod

        assert method
        assert proxy_method.method is method
        assert proxy_method.handler is handler

    def test_proxy_method_chain(self):
        interface0 = TestInterface.DESCRIPTOR
        interface1 = NextTestInterface.DESCRIPTOR
        method0 = interface0.find_method('interfaceMethod')
        method1 = interface1.find_method('indexMethod')
        handler = lambda inv: InvocationResult.ok(None)

        proxy = InvocationProxy(interface0, handler)
        proxy_method = proxy.interfaceMethod(1, 2).indexMethod

        assert proxy_method.method is method1
        assert proxy_method.handler is handler
        assert proxy_method.invocation
        assert proxy_method.invocation.method is method0

    def test_invocation_chain(self):
        handler = lambda inv: InvocationResult.ok(inv)
        proxy = InvocationProxy(TestInterface.DESCRIPTOR, handler)

        invocation = proxy.interfaceMethod(1, 2).remoteMethod()
        chain = invocation.to_chain()
        invocation0 = chain[0]
        invocation1 = chain[1]

        assert invocation0.method.name == 'interfaceMethod'
        assert invocation0.args == {'a': 1, 'b': 2}

        assert invocation1.method.name == 'remoteMethod'
        assert invocation1.args == {}
