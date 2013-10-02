# encoding: utf-8
import pdef_lang.collects
import pdef_lang.enums
from pdef_lang import definitions


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
        self._definition, errors = linker(self.name)
        return errors


class ListReference(Reference):
    '''List reference, the element can be a name, another reference or a definition.'''
    def __init__(self, element):
        super(ListReference, self).__init__()
        self.element = reference(element)

    def link(self, linker):
        errors = self.element.link(linker)

        if not errors:
            self._definition = pdef_lang.collects.List(self.element)

        return errors


class SetReference(Reference):
    '''Set reference, the element can be a name, another reference or a definition.'''
    def __init__(self, element):
        super(SetReference, self).__init__()
        self.element = reference(element)

    def link(self, linker):
        errors = self.element.link(linker)

        if not errors:
            self._definition = pdef_lang.collects.Set(self.element)

        return errors


class MapReference(Reference):
    '''Map reference, the key/value can be names, another references or definitions.'''
    def __init__(self, key, value):
        super(MapReference, self).__init__()
        self.key = reference(key)
        self.value = reference(value)

    def link(self, linker):
        errors0 = self.key.link(linker)
        errors1 = self.value.link(linker)
        errors = errors0 + errors1

        if not errors:
            self._definition = pdef_lang.collects.Map(self.key, self.value)

        return errors
