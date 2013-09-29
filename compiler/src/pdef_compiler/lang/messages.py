# encoding: utf-8
import logging
from .definitions import Type, Definition
from .validator import ValidatorError


class Message(Definition):
    '''User-defined message.'''
    def __init__(self, name, base=None, discriminator_value=None, declared_fields=None,
                 is_exception=False, is_form=False, doc=None, location=None):
        super(Message, self).__init__(Type.MESSAGE, name, doc=doc, location=location)

        self.base = base
        self.discriminator_value = discriminator_value  # Enum value.
        self._discriminator = None  # Discriminator field, self.discriminator is a property.

        self.subtypes = []
        self.declared_fields = []

        self.is_form = is_form
        self.is_exception = is_exception

        if declared_fields:
            map(self.add_field, declared_fields)

    @property
    def is_polymorphic(self):
        return bool(self.discriminator)

    @property
    def discriminator(self):
        '''Return this message discriminator field, base discriminator field, or None.'''
        if self._discriminator:
            return self._discriminator

        return self.base.discriminator if self.base else None

    @property
    def fields(self):
        if not self.base:
            return self.declared_fields

        return self.base.fields + self.declared_fields

    @property
    def inherited_fields(self):
        if not self.base:
            return []

        return self.base.fields

    def add_field(self, field):
        '''Add a new field to this message and return the field.
        :param field:
        '''
        if field.message:
            raise ValueError('Field is already in a message, %s' % file)

        self.declared_fields.append(field)
        field.message = self

        if field.is_discriminator:
            self._discriminator = field

        logging.debug('%s: added a field, field=%s', self, field.name)
        return field

    def create_field(self, name, definition, is_discriminator=False):
        '''Create a new field, add it to this message and return the field.'''
        field = Field(name, definition, is_discriminator=is_discriminator)
        return self.add_field(field)

    def _add_subtype(self, subtype):
        '''Add a new subtype to this message.'''
        if not isinstance(subtype, Message):
            raise ValueError('Must be a message instance, %r'  % subtype)

        if subtype is self:
            return

        self.subtypes.append(subtype)
        if self.base:
            self.base._add_subtype(subtype)

    def link(self, linker):
        errors = []

        self.base, errors0 = linker.link(self.base)
        errors += errors0

        self.discriminator_value, errors0 = linker.link(self.discriminator_value)
        errors += errors0

        if self.base:
            errors += self.base.link(linker)

        if self.discriminator_value:
            self.base._add_subtype(self)

        for field in self.declared_fields:
            errors += field.link()

        return errors

    def validate(self):
        errors = []
        errors += self._validate_base()
        errors += self._validate_discriminator_value()
        errors += self._validate_subtypes()
        errors += self._validate_fields()
        return errors

    def _validate_base(self):
        errors = []
        base = self.base
        if not base:
            return errors

        # The base must be a message itself.
        if not base.is_message:
            errors.append(ValidatorError(self, 'base must be a message, base=%s' % base))

        # The base exception/message flag must match this message flag.
        if self.is_exception != base.is_exception:
            errors.append(ValidatorError(self, 'wrong base type (message/exc), base=%s', base))

        # The base must be defined before this message.
        errors += base._validate_is_defined_before(base)

        # Prevent circular inheritance.
        while base:
            if base is self:
                errors.append(ValidatorError(self, 'circular inheritance'))
                break

            base = base.base

    def _validate_discriminator_value(self):
        errors = []
        base = self.base
        dvalue = self.discriminator_value

        if not dvalue:
            if base and base.is_polymorphic:
                # The base is present and it is polymorphic,
                # so it requires a discriminator value.
                return [ValidatorError(self, 'discriminator value required')]

            return []

        # The discriminator value is present.
        if not base:
            # But no base, it's an error.
            return [ValidatorError(self, 'cannot set a discriminator value, no base')]

        if not dvalue.is_enum_value:
            errors.append(ValidatorError(self, 'discriminator value must be an enum value'))

        if not base.is_polymorphic:
            errors.append(ValidatorError(self, 'cannot set a discriminator value, the base '
                                               'does not have a discriminator'))

        if dvalue not in base.discriminator.type:
            errors.append(ValidatorError(self, 'discriminator value does not match base '
                                               'discriminator type'))

        # The discriminator type must be defined before the message.
        errors += dvalue.enum._validate_is_defined_before(self)

        return errors

    def _validate_subtypes(self):
        errors = []
        if not self.subtypes:
            return []

        dvalues = set()
        for subtype in self.subtypes:
            dvalue = subtype.discriminator_value

            if dvalue in dvalues:
                errors.append(ValidatorError(self, 'Duplicate discriminator value, %s', dvalue))

            dvalues.add(dvalue)

        return errors

    def _validate_fields(self):
        errors = []

        # Prevent duplicate fields.
        names = set()
        for field in self.fields:
            if field.name in names:
                errors.append(ValidatorError(self, 'Duplicate field %r', field.name))

            names.add(field.name)

        # Prevent multiple discriminator fields.
        discriminator = None
        for field in self.fields:
            if not field.is_discriminator:
                continue

            if discriminator:
                errors.append(ValidatorError(self, 'Multiple discriminator fields'))
                break  # One multiple discriminator error is enough.

            discriminator = field

        for field in self.fields:
            errors += field.validate()

        return errors


class Field(object):
    '''Single message field.'''
    def __init__(self, name, type0, is_discriminator=False):
        self.name = name
        self.type = type0
        self.is_discriminator = is_discriminator
        self.message = None

    @property
    def fullname(self):
        if not self.message:
            return self.name

        return '%s.%s' % (self.message.fullname, self.name)

    def link(self, linker):
        self.type, errors = linker.link(self.type)
        return errors

    def validate(self):
        errors = []

        if not self.type.is_datatype:
            errors.append(ValidatorError(self, 'Field must be a data type'))

        if self.is_discriminator and not self.type.is_enum:
            errors.append(ValidatorError(self, 'Discriminator field must be an enum'))

        return errors
