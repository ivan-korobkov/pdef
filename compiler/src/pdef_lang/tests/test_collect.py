# encoding: utf-8
import unittest
from pdef_lang import definitions, interfaces, messages
from pdef_lang.collect import *


class TestList(unittest.TestCase):
    def test_link(self):
        list0 = List('module.Message')
        errors = list0.link(lambda name: (name, []))

        assert not errors
        assert list0.element == 'module.Message'

    def test_validate__element_is_data_type(self):
        iface = interfaces.Interface('Interface')
        list0 = List(iface)
        errors = list0.validate()

        assert 'List element must be a data type' in errors[0].message


class TestSet(unittest.TestCase):
    def test_link(self):
        set0 = Set('module.Message')
        errors = set0.link(lambda name: (name, []))

        assert not errors
        assert set0.element == 'module.Message'

    def test_validate__element_is_data_type(self):
        iface = interfaces.Interface('Interface')
        set0 = Set(iface)
        errors = set0.validate()

        assert 'Set element must be a data type' in errors[0].message


class TestMap(unittest.TestCase):
    def test_link(self):
        map0 = Map('key', 'value')
        errors = map0.link(lambda name: (name, []))

        assert not errors
        assert map0.key == 'key'
        assert map0.value == 'value'

    def test_validate__key_is_primitive(self):
        msg = messages.Message('Message')
        map0 = Map(msg, msg)
        errors = map0.validate()

        assert 'Map key must be a primitive' in errors[0].message

    def test_validate__value_is_data_type(self):
        iface = interfaces.Interface('Interface')
        map0 = Map(definitions.NativeTypes.STRING, iface)
        errors = map0.validate()

        assert 'Map value must be a data type' in errors[0].message
