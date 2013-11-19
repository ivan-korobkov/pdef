# encoding: utf-8
import unittest
from pdefc.lang import collects, types, references, NativeType


class TestReference(unittest.TestCase):
    def test_empty(self):
        ref = references.reference(None)
        assert isinstance(ref, references.EmptyReference)

    def test_name(self):
        ref = references.reference('module.Message')
        assert isinstance(ref, references.NameReference)

    def test_definition(self):
        def0 = types.Definition(types.TypeEnum.MESSAGE, 'Message')
        ref = references.reference(def0)

        assert isinstance(ref, references.Reference)
        assert ref.dereference() is def0

    def test_reference(self):
        ref0 = references.reference(None)
        ref1 = references.reference(ref0)

        assert ref0 is ref1

    def test_dereference(self):
        def0 = types.Definition(types.TypeEnum.MESSAGE, 'Message')
        ref = references.Reference(def0)

        assert ref.dereference() is def0


class TestNameReference(unittest.TestCase):
    def test_link(self):
        lookup = lambda name: name
        ref = references.reference('module.Message')
        errors = ref.link(lookup)

        assert not errors
        assert ref.dereference() == 'module.Message'

    def test_link__error(self):
        lookup = lambda name: None
        ref = references.reference('module.Message')
        errors = ref.link(lookup)

        assert "Type not found 'module.Message'" in errors[0]


class TestListReference(unittest.TestCase):
    def test_link(self):
        lookup = lambda name: NativeType.INT32
        ref = references.ListReference('int32')
        errors = ref.link(lookup)
        list0 = ref.dereference()

        assert not errors
        assert isinstance(list0, collects.List)
        assert list0.element is NativeType.INT32

    def test_link_errors(self):
        lookup = lambda name: None
        ref = references.ListReference('element')
        errors = ref.link(lookup)

        assert not ref
        assert len(errors) == 1

    def test_validate(self):
        ref = references.ListReference(NativeType.VOID)
        errors = ref.validate()

        assert len(errors) == 1


class TestSetReference(unittest.TestCase):
    def test_link(self):
        lookup = lambda name: NativeType.INT32
        ref = references.SetReference('int32')
        errors = ref.link(lookup)
        set0 = ref.dereference()

        assert not errors
        assert isinstance(set0, collects.Set)
        assert set0.element is NativeType.INT32

    def test_link_errors(self):
        lookup = lambda name: None
        ref = references.SetReference('element')
        errors = ref.link(lookup)

        assert not ref
        assert len(errors) == 1

    def test_validate(self):
        ref = references.SetReference(NativeType.VOID)
        errors = ref.validate()

        assert len(errors) == 1


class TestMapReference(unittest.TestCase):
    def test_link(self):
        lookup = lambda name: NativeType.STRING
        ref = references.MapReference('string', 'string')
        errors = ref.link(lookup)
        map0 = ref.dereference()

        assert not errors
        assert isinstance(map0, collects.Map)
        assert map0.key is NativeType.STRING
        assert map0.value is NativeType.STRING

    def test_link_errors(self):
        lookup = lambda name: None
        ref = references.MapReference('key', 'value')
        errors = ref.link(lookup)

        assert not ref
        assert "Type not found 'key'" in errors[0]
        assert "Type not found 'value'" in errors[1]

    def test_validate(self):
        ref = references.MapReference(NativeType.VOID, NativeType.VOID)
        errors = ref.validate()

        assert len(errors) == 2
