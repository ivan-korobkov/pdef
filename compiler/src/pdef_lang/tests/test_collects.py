# encoding: utf-8
import unittest
from pdef_lang import *


class TestList(unittest.TestCase):
    def test_validate__element_is_data_type(self):
        iface = interfaces.Interface('Interface')
        list0 = List(iface)
        errors = list0.validate()

        assert 'list element must be a data type' in errors[0].message


class TestSet(unittest.TestCase):
    def test_validate__element_is_data_type(self):
        iface = interfaces.Interface('Interface')
        set0 = Set(iface)
        errors = set0.validate()

        assert 'set element must be a data type' in errors[0].message


class TestMap(unittest.TestCase):
    def test_validate__key_is_primitive(self):
        msg = messages.Message('Message')
        map0 = Map(msg, msg)
        errors = map0.validate()

        assert 'map key must be a primitive' in errors[0].message

    def test_validate__value_is_data_type(self):
        iface = interfaces.Interface('Interface')
        map0 = Map(definitions.NativeType.STRING, iface)
        errors = map0.validate()

        assert 'map value must be a data type' in errors[0].message
