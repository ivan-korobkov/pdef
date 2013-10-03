# encoding: utf-8
import pdef_lang.collects
from pdef_lang import definitions, exc


def reference(name_ref_def):
    '''Create a reference from a string name, another reference, a definition or None'''
    if name_ref_def is None:
        return EmptyReference()

    elif isinstance(name_ref_def, basestring):
        return NameReference(name_ref_def)

    elif isinstance(name_ref_def, definitions.Type):
        return Reference(name_ref_def)

    elif isinstance(name_ref_def, Reference):
        return name_ref_def

    raise ValueError('Unsupported type: %r' % name_ref_def)


class Reference(object):
    '''Reference directly references a type.'''
    def __init__(self, type0=None):
        self._type = type0

    def __nonzero__(self):
        return bool(self._type)

    def dereference(self):
        '''Return a type this references points to or raise ValueError when not linked.'''
        if not self._type:
            raise ValueError('Reference is not linked: %s' % self)
        return self._type

    def link(self, scope):
        '''Link this reference in a provided callable scope.'''
        return []

    def validate(self):
        '''Validate this reference and return a list of errors.'''
        return []


class EmptyReference(Reference):
    '''EmptyReference is a sentinel for an absent type. It returns None when dereferenced'''
    def __init__(self):
        super(EmptyReference, self).__init__()

    def dereference(self):
        return None


class NameReference(Reference):
    '''NameReference references a type by its name.'''
    def __init__(self, name):
        super(NameReference, self).__init__(None)
        self.name = name

    def link(self, scope):
        self._type = scope(self.name)
        if self._type:
            return []

        return [exc.error(self, 'type not found %r', self.name)]


class ListReference(Reference):
    '''ListReference has a child reference for an element, creates a list on linking.'''
    def __init__(self, element):
        super(ListReference, self).__init__()
        self.element = reference(element)
        self._init_type()

    def _init_type(self):
        if not self.element:
            return
        self._type = pdef_lang.collects.List(self.element.dereference())

    def link(self, scope):
        errors = self.element.link(scope)
        if errors:
            return errors

        self._init_type()
        return []

    def validate(self):
        if not self._type:
            return []
        return self._type.validate()


class SetReference(Reference):
    '''SetReference has a child for an element, creates a set on linking.'''
    def __init__(self, element):
        super(SetReference, self).__init__()
        self.element = reference(element)
        self._init_type()

    def _init_type(self):
        if not self.element:
            return
        self._type = pdef_lang.collects.Set(self.element.dereference())

    def link(self, scope):
        errors = self.element.link(scope)
        if errors:
            return errors

        self._init_type()
        return []

    def validate(self):
        if not self._type:
            return []
        return self._type.validate()


class MapReference(Reference):
    '''MapReference has children references for a key and a value, creates a map on linking.'''
    def __init__(self, key, value):
        super(MapReference, self).__init__()
        self.key = reference(key)
        self.value = reference(value)
        self._init_type()

    def _init_type(self):
        if not self.key or not self.value:
            return
        self._type = pdef_lang.collects.Map(self.key.dereference(), self.value.dereference())

    def link(self, scope):
        errors0 = self.key.link(scope)
        errors1 = self.value.link(scope)
        if errors0 or errors1:
            return errors0 + errors1

        self._init_type()
        return []

    def validate(self):
        if not self._type:
            return []
        return self._type.validate()
