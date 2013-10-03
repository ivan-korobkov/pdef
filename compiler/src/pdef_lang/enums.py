# encoding: utf-8
from pdef_lang import definitions, exc


class Enum(definitions.Definition):
    '''Enum definition.'''
    def __init__(self, name, value_names=None):
        super(Enum, self).__init__(definitions.TypeEnum.ENUM, name)
        self.values = []

        if value_names:
            map(self.add_value, value_names)

    def add_value(self, name):
        '''Create a new enum value by its name, add it to this enum, and return it.'''
        value = EnumValue(self, name)
        self.values.append(value)
        return value

    def get_value(self, name):
        '''Get a value by its name or raise an exception.'''
        for value in self.values:
            if value.name == name:
                return value

    def __contains__(self, enum_value):
        return enum_value in self.values

    def validate(self):
        errors = []

        names = set()
        for value in self.values:
            if value.name in names:
                errors.append(exc.error(self, 'duplicate enum value, %r', value.name))

            names.add(value.name)

        return errors


class EnumValue(definitions.Type):
    '''Single enum value which has a name and a pointer to the declaring enum.'''
    def __init__(self, enum, name):
        super(EnumValue, self).__init__(definitions.TypeEnum.ENUM_VALUE)
        self.enum = enum
        self.name = name
