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
        scope = lambda name: name
        ref = references.reference('module.Message')
        errors = ref.link(scope)

        assert not errors
        assert ref.dereference() == 'module.Message'

    def test_link__error(self):
        scope = lambda name: None
        ref = references.reference('module.Message')
        errors = ref.link(scope)

        assert "symbol not found 'module.Message'" in errors[0].message


class TestListReference(unittest.TestCase):
    def test_link(self):
        scope = lambda name: name
        ref = references.ListReference('element')
        errors = ref.link(scope)
        list0 = ref.dereference()

        assert not errors
        assert isinstance(list0, collects.List)
        assert list0.element == 'element'

    def test_link_errors(self):
        scope = lambda name: None
        ref = references.ListReference('element')
        errors = ref.link(scope)

        assert not ref
        assert len(errors) == 1


class TestSetReference(unittest.TestCase):
    def test_link(self):
        scope = lambda name: name
        ref = references.SetReference('set_element')
        errors = ref.link(scope)
        set0 = ref.dereference()

        assert not errors
        assert isinstance(set0, collects.Set)
        assert set0.element == 'set_element'

    def test_link_errors(self):
        scope = lambda name: None
        ref = references.SetReference('set_element')
        errors = ref.link(scope)

        assert not ref
        assert len(errors) == 1


class TestMapReference(unittest.TestCase):
    def test_link(self):
        scope = lambda name: name
        ref = references.MapReference('key', 'value')
        errors = ref.link(scope)
        map0 = ref.dereference()

        assert not errors
        assert isinstance(map0, collects.Map)
        assert map0.key == 'key'
        assert map0.value == 'value'

    def test_link_errors(self):
        scope = lambda name: None
        ref = references.MapReference('key', 'value')
        errors = ref.link(scope)

        assert not ref
        assert "symbol not found 'key'" in errors[0].message
        assert "symbol not found 'value'" in errors[1].message
