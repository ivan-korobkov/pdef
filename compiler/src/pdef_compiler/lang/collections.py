# encoding: utf-8
from .definitions import Definition, Type
from pdef_compiler.lang.validator import ValidatorError


class List(Definition):
    '''List definition.'''
    def __init__(self, element):
        super(List, self).__init__(Type.LIST, 'list')
        self.element = element

    def link(self, linker):
        self.element, errors = linker.link(self.element)
        return errors

    def validate(self):
        errors = []

        if not self.element.is_datatype:
            errors.append(ValidatorError(self, 'List element must be a data type'))

        return errors


class Set(Definition):
    '''Set definition.'''
    def __init__(self, element):
        super(Set, self).__init__(Type.SET, 'set')
        self.element = element

    def link(self, linker):
        self.element, errors = linker.link(self.element)
        return errors

    def validate(self):
        errors = []

        if not self.element.is_datatype:
            errors.append(ValidatorError(self, 'Set element must be a data type'))

        return errors


class Map(Definition):
    '''Map definition.'''
    def __init__(self, key, value):
        super(Map, self).__init__(Type.MAP, 'map')
        self.key = key
        self.value = value

    def link(self, linker):
        errors = []

        self.key, errors0 = linker.link(self.key)
        errors += errors0

        self.value, errors0 = linker.link(self.value)
        errors += errors0

        return errors

    def validate(self):
        errors = []

        if not self.key.is_primitive:
            errors.append(ValidatorError(self, 'Map key must be a primitive'))

        if not self.value.is_datatype:
            errors.append(ValidatorError(self, 'Map value must be a data type'))

        return errors
