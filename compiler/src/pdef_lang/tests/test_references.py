# encoding: utf-8
import unittest
from pdef_lang import collects, definitions, references


class TestReference(unittest.TestCase):
    def test_empty(self):
        ref = references.reference(None)
        assert isinstance(ref, references.EmptyReference)

    def test_name(self):
        ref = references.reference('module.Message')
        assert isinstance(ref, references.NameReference)

    def test_definition(self):
        def0 = definitions.Definition(definitions.Type.MESSAGE, 'Message')
        ref = references.reference(def0)
        assert isinstance(ref, references.Reference)
        assert ref.dereference() is def0

    def test_reference(self):
        ref0 = references.reference(None)
        ref1 = references.reference(ref0)
        assert ref0 is ref1

    def test_dereference(self):
        def0 = definitions.Definition(definitions.Type.MESSAGE, 'Message')
        ref = references.Reference(def0)
        assert ref.dereference() is def0


class TestNameReference(unittest.TestCase):
    def test_link(self):
        linker = lambda name: (name, ['error'])
        ref = references.reference('module.Message')
        errors = ref.link(linker)

        assert errors == ['error']
        assert ref.dereference() == 'module.Message'


class TestListReference(unittest.TestCase):
    def test_link(self):
        linker = lambda name: (name, [])
        ref = references.ListReference('element')
        errors = ref.link(linker)
        list0 = ref.dereference()

        assert not errors
        assert isinstance(list0, collects.List)
        assert list0.element == 'element'

    def test_link_errors(self):
        linker = lambda name: (name, ['list_error'])
        ref = references.ListReference('element')
        errors = ref.link(linker)

        assert errors == ['list_error']
        assert ref._definition is None


class TestSetReference(unittest.TestCase):
    def test_link(self):
        linker = lambda name: (name, [])
        ref = references.SetReference('set_element')
        errors = ref.link(linker)
        set0 = ref.dereference()

        assert not errors
        assert isinstance(set0, collects.Set)
        assert set0.element == 'set_element'

    def test_link_errors(self):
        linker = lambda name: (name, ['set_error'])
        ref = references.SetReference('set_element')
        errors = ref.link(linker)

        assert errors == ['set_error']
        assert ref._definition is None


class TestMapReference(unittest.TestCase):
    def test_link(self):
        linker = lambda name: (name, [])
        ref = references.MapReference('key', 'value')
        errors = ref.link(linker)
        map0 = ref.dereference()

        assert not errors
        assert isinstance(map0, collects.Map)
        assert map0.key == 'key'
        assert map0.value == 'value'

    def test_link_errors(self):
        linker = lambda name: (name, ['map_error_' + name])
        ref = references.MapReference('key', 'value')
        errors = ref.link(linker)

        assert errors == ['map_error_key', 'map_error_value']
        assert ref._definition is None
