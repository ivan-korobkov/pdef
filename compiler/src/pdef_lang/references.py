# encoding: utf-8
import pdef_lang.collects
import pdef_lang.enums
from pdef_lang import definitions, validation


def reference(name_ref_def):
    '''Create a reference from a string name, another reference, a definition or None'''
    if name_ref_def is None:
        return EmptyReference()

    elif isinstance(name_ref_def, basestring):
        return NameReference(name_ref_def)

    elif isinstance(name_ref_def, pdef_lang.enums.EnumValue):
        # TODO: replace with Type
        return Reference(name_ref_def)

    elif isinstance(name_ref_def, definitions.Definition):
        return Reference(name_ref_def)

    elif isinstance(name_ref_def, Reference):
        return name_ref_def

    raise ValueError('Unsupported type: %r' % name_ref_def)


class Reference(object):
    '''Simple reference which directly points to a definition.'''
    def __init__(self, definition=None):
        self._definition = definition

    def __nonzero__(self):
        return bool(self._definition)

    def dereference(self):
        if not self._definition:
            raise ValueError('Reference is not linked: %s' % self)
        return self._definition

    def link(self, scope):
        return []


class EmptyReference(Reference):
    '''Always returns None when dereferenced.'''
    def __init__(self):
        super(EmptyReference, self).__init__(None)

    def dereference(self):
        return None


class NameReference(Reference):
    '''Reference which uses a definition name to reference it.'''
    def __init__(self, name):
        super(NameReference, self).__init__(None)
        self.name = name

    def link(self, scope):
        self._definition = scope(self.name)
        if self._definition:
            return []

        return [validation.error(self, 'symbol not found %r', self.name)]


class ListReference(Reference):
    '''List reference, the element can be a name, another reference or a definition.'''
    def __init__(self, element):
        super(ListReference, self).__init__()
        self.element = reference(element)

    def link(self, scope):
        errors = self.element.link(scope)
        if errors:
            return errors

        self._definition = pdef_lang.collects.List(self.element)
        return []


class SetReference(Reference):
    '''Set reference, the element can be a name, another reference or a definition.'''
    def __init__(self, element):
        super(SetReference, self).__init__()
        self.element = reference(element)

    def link(self, scope):
        errors = self.element.link(scope)
        if errors:
            return errors

        self._definition = pdef_lang.collects.Set(self.element)
        return []


class MapReference(Reference):
    '''Map reference, the key/value can be names, another references or definitions.'''
    def __init__(self, key, value):
        super(MapReference, self).__init__()
        self.key = reference(key)
        self.value = reference(value)

    def link(self, scope):
        errors0 = self.key.link(scope)
        errors1 = self.value.link(scope)
        if errors0 or errors1:
            return errors0 + errors1

        self._definition = pdef_lang.collects.Map(self.key, self.value)
        return []
