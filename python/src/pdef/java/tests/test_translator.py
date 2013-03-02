# encoding: utf-8
import unittest
from pdef import lang
from pdef.java import *


class TestJavaRef(unittest.TestCase):

    def setUp(self):
        module = lang.Module('module')
        self.msg = lang.Message('Message', variables=[lang.Variable('T')])
        self.msg.parent = module

    def test(self):
        ref = JavaRef.from_ref(self.msg)
        assert str(ref) == 'module.Message<T>'

    def test_local(self):
        ref = JavaRef.from_ref(self.msg).local
        assert str(ref) == 'Message<T>'

    def test_raw(self):
        ref = JavaRef.from_ref(self.msg).raw
        assert str(ref) == 'module.Message'

    def test_wildcards(self):
        ref = JavaRef.from_ref(self.msg).wildcard
        assert str(ref) == 'module.Message<?>'

    def test_local_wildcars(self):
        ref = JavaRef.from_ref(self.msg).local.wildcard
        assert str(ref) == 'Message<?>'
