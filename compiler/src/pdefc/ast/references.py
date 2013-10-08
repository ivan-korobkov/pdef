# encoding: utf-8
import pdefc.ast.collects
from pdefc.ast.definitions import Located, Type


def reference(name_ref_def):
    '''Create a reference from a string name, another reference, a definition or None'''
    if name_ref_def is None:
        return EmptyReference()

    elif isinstance(name_ref_def, basestring):
        return NameReference(name_ref_def)

    elif isinstance(name_ref_def, Type):
        return Reference(name_ref_def)

    elif isinstance(name_ref_def, Reference):
        return name_ref_def

    raise ValueError('Unsupported type: %r' % name_ref_def)


class Reference(Located):
    '''Reference directly references a type.'''
    def __init__(self, type0=None, location=None):
        self._type = type0
        self.location = location

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
    def __init__(self, location=None):
        super(EmptyReference, self).__init__(None, location=location)

    def dereference(self):
        return None


class NameReference(Reference):
    '''NameReference references a type by its name.'''
    def __init__(self, name, location=None):
        super(NameReference, self).__init__(None, location=location)
        self.name = name

    def link(self, scope):
        self._type = scope(self.name)
        if self._type:
            return []

        return [self._error('Type not found %r', self.name)]


class ListReference(Reference):
    '''ListReference has a child reference for an element, creates a list on linking.'''
    def __init__(self, element, location=None):
        super(ListReference, self).__init__(None, location=location)
        self.element = reference(element)
        self._init_type()

    def _init_type(self):
        if not self.element:
            return
        self._type = pdefc.ast.collects.List(self.element.dereference(), location=self.location)

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
    def __init__(self, element, location=None):
        super(SetReference, self).__init__(None, location=location)
        self.element = reference(element)
        self._init_type()

    def _init_type(self):
        if not self.element:
            return
        self._type = pdefc.ast.collects.Set(self.element.dereference(), location=self.location)

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
    def __init__(self, key, value, location=None):
        super(MapReference, self).__init__(None, location=location)
        self.key = reference(key)
        self.value = reference(value)
        self._init_type()

    def _init_type(self):
        if not self.key or not self.value:
            return
        self._type = pdefc.ast.collects.Map(self.key.dereference(), self.value.dereference(),
                                            location=self.location)

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
