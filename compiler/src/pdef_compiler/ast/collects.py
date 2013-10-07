# encoding: utf-8
from pdef_compiler.ast.definitions import Type, TypeEnum


class List(Type):
    '''List collection.'''
    def __init__(self, element, location=None):
        super(List, self).__init__(TypeEnum.LIST, location=location)
        self.element = element

        if not isinstance(element, Type):
            raise TypeError('Element must be a definitions.Type instance, %r' % element)

    def validate(self):
        errors = []

        if not self.element.is_data_type:
            errors.append(self._error('List element must be a data type'))

        return errors


class Set(Type):
    '''Set collection.'''
    def __init__(self, element, location=None):
        super(Set, self).__init__(TypeEnum.SET, location=location)
        self.element = element

        if not isinstance(element, Type):
            raise TypeError('Element must be a definitions.Type instance, %r' % element)

    def validate(self):
        errors = []

        if not self.element.is_data_type:
            errors.append(self._error('Set element must be a data type'))

        return errors


class Map(Type):
    '''Map collection.'''
    def __init__(self, key, value, location=None):
        super(Map, self).__init__(TypeEnum.MAP, location=location)
        self.key = key
        self.value = value

        if not isinstance(key, Type):
            raise TypeError('Key must be a definitions.Type instance, %r' % key)

        if not isinstance(value, Type):
            raise TypeError('Value must be a definitions.Type instance, %r' % value)

    def validate(self):
        errors = []

        if not self.key.is_primitive:
            errors.append(self._error('Map key must be a primitive'))

        if not self.value.is_data_type:
            errors.append(self._error('Map value must be a data type'))

        return errors
