# encoding: utf-8
from pdef_lang import definitions, exc


class List(definitions.Type):
    '''List collection.'''
    def __init__(self, element):
        super(List, self).__init__(definitions.TypeEnum.LIST)
        self.element = element

        if not isinstance(element, definitions.Type):
            raise TypeError('Element must be a definitions.Type instance, %r' % element)

    def validate(self):
        errors = []

        if not self.element.is_data_type:
            errors.append(exc.error(self, 'list element must be a data type'))

        return errors


class Set(definitions.Type):
    '''Set collection.'''
    def __init__(self, element):
        super(Set, self).__init__(definitions.TypeEnum.SET)
        self.element = element

        if not isinstance(element, definitions.Type):
            raise TypeError('Element must be a definitions.Type instance, %r' % element)

    def validate(self):
        errors = []

        if not self.element.is_data_type:
            errors.append(exc.error(self, 'set element must be a data type'))

        return errors


class Map(definitions.Type):
    '''Map collection.'''
    def __init__(self, key, value):
        super(Map, self).__init__(definitions.TypeEnum.MAP)
        self.key = key
        self.value = value

        if not isinstance(key, definitions.Type):
            raise TypeError('Key must be a definitions.Type instance, %r' % key)

        if not isinstance(value, definitions.Type):
            raise TypeError('Value must be a definitions.Type instance, %r' % value)

    def validate(self):
        errors = []

        if not self.key.is_primitive:
            errors.append(exc.error(self, 'map key must be a primitive'))

        if not self.value.is_data_type:
            errors.append(exc.error(self, 'map value must be a data type'))

        return errors
