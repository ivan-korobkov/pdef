# encoding: utf-8
import unittest
from pdefc.lang import interfaces, List, Set, Message, Map, NativeType


class TestList(unittest.TestCase):
    def test_validate__element_is_data_type(self):
        iface = interfaces.Interface('Interface')
        list0 = List(iface)
        errors = list0.validate()

        assert 'List element must be a data type' in errors[0]


class TestSet(unittest.TestCase):
    def test_validate__element_is_data_type(self):
        iface = interfaces.Interface('Interface')
        set0 = Set(iface)
        errors = set0.validate()

        assert 'Set element must be a data type' in errors[0]


class TestMap(unittest.TestCase):
    def test_validate__key_is_primitive(self):
        msg = Message('Message')
        map0 = Map(msg, msg)
        errors = map0.validate()

        assert 'Map key must be a primitive' in errors[0]

    def test_validate__value_is_data_type(self):
        iface = interfaces.Interface('Interface')
        map0 = Map(NativeType.STRING, iface)
        errors = map0.validate()

        assert 'Map value must be a data type' in errors[0]
