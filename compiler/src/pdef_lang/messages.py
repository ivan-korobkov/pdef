# encoding: utf-8
import logging
from pdef_lang import definitions, validation, references


class Message(definitions.Definition):
    '''User-defined message.'''
    def __init__(self, name, base=None, discriminator_value=None, declared_fields=None,
                 is_exception=False, is_form=False, doc=None, location=None):
        super(Message, self).__init__(definitions.Type.MESSAGE, name, doc=doc, location=location)

        self.base = base
        self.discriminator_value = discriminator_value
        self._discriminator = None  # Field.

        self.subtypes = []
        self.declared_fields = []

        self.is_form = is_form
        self.is_exception = is_exception

        if declared_fields:
            map(self.add_field, declared_fields)

    @property
    def base(self):
        return self._base.dereference()

    @base.setter
    def base(self, value):
        self._base = references.reference(value)

    @property
    def discriminator_value(self):
        return self._discriminator_value.dereference()

    @discriminator_value.setter
    def discriminator_value(self, value):
        self._discriminator_value = references.reference(value)

    @property
    def discriminator(self):
        '''Return this message discriminator field, base discriminator field, or None.'''
        if self._discriminator:
            return self._discriminator

        return self.base.discriminator if self.base else None

    @property
    def is_polymorphic(self):
        return bool(self.discriminator)

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
        '''Add a new field to this message and return the field.'''
        if field.message:
            raise ValueError('Field is already in a message, %s' % field)

        field.message = self
        self.declared_fields.append(field)

        # TODO: move into the build stage.
        if field.is_discriminator:
            self._discriminator = field

        logging.debug('%s: added a field %r', self, field.name)
        return field

    def create_field(self, name, definition, is_discriminator=False):
        '''Create a new field, add it to this message and return the field.'''
        field = Field(name, definition, is_discriminator=is_discriminator)
        return self.add_field(field)

    def _add_subtype(self, subtype):
        '''Add a new subtype to this message.'''
        if not isinstance(subtype, Message):
            raise ValueError('Must be a message instance, %r' % subtype)

        if subtype is self:
            return

        self.subtypes.append(subtype)
        if self.base:
            self.base._add_subtype(subtype)

    def link(self, linker):
        errors = []
        errors += self._base.link(linker)
        errors += self._discriminator_value.link(linker)

        for field in self.declared_fields:
            errors += field.link()

        if self._discriminator_value:
            # TODO: move to the build stage.
            self.base._add_subtype(self)

        return errors

    def validate(self):
        errors = []
        errors += self._validate_base()
        if errors:
            # Cannot continue validation when the base is wrong.
            return errors

        errors += self._validate_discriminator_value()
        errors += self._validate_subtypes()
        errors += self._validate_fields()
        return errors

    def _validate_base(self):
        base = self.base
        if not base:
            return []

        # The base must be a message.
        if not base.is_message:
            return [validation.error(self, 'base must be a message, base=%s' % base)]

        errors = []

        # The base exception/message flag must match this message flag.
        if self.is_exception != base.is_exception:
            errors.append(validation.error(self, 'wrong base type (message/exc), base=%s', base))

        # The base must be defined before this message.
        errors += base._validate_is_defined_before(self)

        # Prevent circular inheritance.
        while base:
            if base is self:
                errors.append(validation.error(self, 'circular inheritance'))
                break

            base = base.base

        return errors

    def _validate_discriminator_value(self):
        base = self.base
        dvalue = self.discriminator_value

        if not dvalue:
            if not base or not base.is_polymorphic:
                return []

            # The base is present and it is polymorphic,
            # so it requires a discriminator value.
            return [validation.error(self, 'discriminator value required')]

        # The discriminator value is present.

        errors = []
        if not dvalue.is_enum_value:
            errors.append(validation.error(self, 'discriminator value must be an enum value'))

        if not base:
            # No base but the discriminator value is present.
            errors.append(validation.error(self, 'cannot set a discriminator value, no base'))
            return errors

        if not base.is_polymorphic:
            # The base is not polymorphic, i.e. does not have a discriminator field.
            errors.append(validation.error(self, 'cannot set a discriminator value, the base '
                                                 'does not have a discriminator field'))
            return errors

        if dvalue not in base.discriminator.type:
            # The discriminator value is not a base discriminator enum value.
            errors.append(validation.error(self, 'discriminator value does not match base '
                                                 'discriminator type'))
            return errors

        # The discriminator type must be defined before the message.
        errors += dvalue.enum._validate_is_defined_before(self)
        return errors

    def _validate_subtypes(self):
        if not self.subtypes:
            return []

        # Prevent duplicate discriminator values in subtypes.
        errors = []
        values = set()
        for subtype in self.subtypes:
            value = subtype.discriminator_value
            if value in values:
                errors.append(validation.error(self, 'duplicate discriminator value %s', value))
            values.add(value)

        return errors

    def _validate_fields(self):
        errors = []

        # Prevent duplicate field names.
        names = set()
        for field in self.fields:
            if field.name in names:
                errors.append(validation.error(self, 'duplicate field %r', field.name))
            names.add(field.name)

        # Prevent multiple discriminator fields.
        discriminator = None
        for field in self.fields:
            if not field.is_discriminator:
                continue

            if discriminator:
                errors.append(validation.error(self, 'multiple discriminator fields'))
                break  # One multiple discriminator error is enough.

            discriminator = field

        for field in self.fields:
            errors += field.validate()

        return errors


class Field(object):
    '''Message field.'''
    def __init__(self, name, type0, is_discriminator=False):
        self.name = name
        self._type = references.reference(type0)

        self.is_discriminator = is_discriminator
        self.message = None

    def __str__(self):
        return self.name

    def __repr__(self):
        return '<%s %s at %s>' % (self.__class__.__name__, self.name, hex(id(self)))

    @property
    def type(self):
        return self._type.dereference()

    def link(self, linker):
        return self._type.link(linker)

    def validate(self):
        errors = []

        if not self.type.is_data_type:
            errors.append(validation.error(self, 'field must be a data type'))

        if self.is_discriminator and not self.type.is_enum:
            errors.append(validation.error(self, 'discriminator field must be an enum'))

        return errors
