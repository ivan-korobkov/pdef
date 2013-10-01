# encoding: utf-8
from . import collections, definitions


def reference(name_ref_def):
    '''Create a reference from a string name, another reference, a definition or None'''
    if name_ref_def is None:
        return EmptyReference()

    elif isinstance(name_ref_def, basestring):
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

    def dereference(self):
        if not self._definition:
            raise ValueError('Reference is not linked: %s' % self)
        return self._definition

    def link(self, linker):
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

    def link(self, linker):
        self._definition, errors = linker.find(self.name)
        return errors


class ListReference(Reference):
    '''List reference, the element can be a name, another reference or a definition.'''
    def __init__(self, element):
        super(ListReference, self).__init__()
        self.element = reference(element)

    def link(self, linker):
        element, errors = self.element.link(linker)

        if element:
            self._definition = collections.List(element)

        return errors


class SetReference(Reference):
    '''Set reference, the element can be a name, another reference or a definition.'''
    def __init__(self, element):
        super(SetReference, self).__init__()
        self.element = reference(element)

    def link(self, linker):
        element, errors = self.element.link(linker)

        if element:
            self._definition = collections.Set(element)

        return errors


class MapReference(Reference):
    '''Map reference, the key/value can be names, another references or definitions.'''
    def __init__(self, key, value):
        super(MapReference, self).__init__()
        self.key = reference(key)
        self.value = reference(value)

    def link(self, linker):
        key, errors0 = self.key.link(linker)
        value, errors1 = self.value.link(linker)

        if key and value:
            self._definition = collections.Map(key, value)

        return errors0 + errors1
