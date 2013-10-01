# encoding: utf-8
from pdef_lang import definitions, references, validation


class List(definitions.Definition):
    '''List definition.'''
    def __init__(self, element):
        super(List, self).__init__(definitions.Type.LIST, 'list')
        self.element = element

    @property
    def element(self):
        return self._element.dereference()

    @element.setter
    def element(self, value):
        self._element = references.reference(value)

    def link(self, linker):
        return self._element.link(linker)

    def validate(self):
        errors = []

        if not self.element.is_datatype:
            errors.append(validation.ValidatorError(self, 'List element must be a data type'))

        return errors


class Set(definitions.Definition):
    '''Set definition.'''
    def __init__(self, element):
        super(Set, self).__init__(definitions.Type.SET, 'set')
        self.element = element

    @property
    def element(self):
        return self._element.dereference()

    @element.setter
    def element(self, value):
        self._element = references.reference(value)

    def link(self, linker):
        return self._element.link(linker)

    def validate(self):
        errors = []

        if not self.element.is_datatype:
            errors.append(validation.error(self, 'Set element must be a data type'))

        return errors


class Map(definitions.Definition):
    '''Map definition.'''
    def __init__(self, key, value):
        super(Map, self).__init__(definitions.Type.MAP, 'map')
        self.key = key
        self.value = value

    @property
    def key(self):
        return self._key.dereference()

    @key.setter
    def key(self, value):
        self._key = references.reference(value)

    @property
    def value(self):
        return self._value.dereference()

    @value.setter
    def value(self, value):
        self._value = references.reference(value)

    def link(self, linker):
        errors0 = self._key.link(linker)
        errors1 = self._value.link(linker)
        return errors0 + errors1

    def validate(self):
        errors = []

        if not self.key.is_primitive:
            errors.append(validation.error(self, 'Map key must be a primitive'))

        if not self.value.is_datatype:
            errors.append(validation.error(self, 'Map value must be a data type'))

        return errors
