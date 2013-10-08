# encoding: utf-8
import logging
from pdefc.ast import references
from pdefc.ast.definitions import Definition, TypeEnum, Located


class Message(Definition):
    '''User-defined message.'''
    def __init__(self, name, base=None, discriminator_value=None, declared_fields=None,
                 is_exception=False, is_form=False, doc=None, location=None):
        super(Message, self).__init__(TypeEnum.EXCEPTION if is_exception else TypeEnum.MESSAGE,
                                      name, doc=doc, location=location)

        self.base = base
        self.discriminator_value = discriminator_value
        self._discriminator = None  # Field.

        self.subtypes = []
        self.declared_fields = []
        self.is_form = is_form

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

        if field.is_discriminator:
            self._discriminator = field

        logging.debug('%s: added a field %r', self, field.name)
        return field

    def create_field(self, name, type0, is_discriminator=False):
        '''Create a new field, add it to this message and return the field.'''
        field = Field(name, type0, is_discriminator=is_discriminator)
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

    def link(self, scope):
        errors = []
        errors += self._base.link(scope)
        errors += self._discriminator_value.link(scope)

        for field in self.declared_fields:
            errors += field.link(scope)

        return errors

    def build(self):
        # Add this message to base subtypes.
        if self._discriminator_value and self.base:
            self.base._add_subtype(self)

        return []

    def validate(self):
        errors = []
        errors += self._validate_base()
        if errors:
            # Cannot continue validation when the base is wrong.
            return errors

        errors += self._validate_discriminator()
        errors += self._validate_subtypes()
        errors += self._validate_fields()
        return errors

    def _validate_base(self):
        base = self.base
        if not base:
            return []

        # The base must be a message.
        if not base.is_message:
            return [self._error('%s: base must be a message, got %s', self, base)]

        errors = []

        if self.is_exception != base.is_exception:
            # The base exception/message flag must match this message flag.
            errors.append(self._error('%s: wrong base type (message/exc) %s', self, base))

        # Validate the reference.
        errors += self._base.validate()

        # The message must be defined after the base.
        errors += self._validate_is_defined_after(base)

        # Prevent circular inheritance.
        while base:
            if base is self:
                errors.append(self._error('%s: circular inheritance', self))
                break

            base = base.base

        return errors

    def _validate_discriminator(self):
        base = self.base
        dvalue = self.discriminator_value

        if not dvalue:
            if not base or not base.is_polymorphic:
                return []

            # The base is present and it is polymorphic,
            # so it requires a discriminator value.
            return [self._error('%s: discriminator value required', self)]

        # The discriminator value is present.

        errors = []
        if not dvalue.is_enum_value:
            errors.append(self._error('%s: discriminator value must be an enum value', self))

        if not base:
            # No base but the discriminator value is present.
            errors.append(self._error('%s: cannot set a discriminator value, no base', self))
            return errors

        if not base.is_polymorphic:
            # The base is not polymorphic, i.e. does not have a discriminator field.
            errors.append(self._error('%s: cannot set a discriminator value, the base '
                                      'is not polymorphic (does not have a discriminator)', self))
            return errors

        if dvalue not in base.discriminator.type:
            # The discriminator value is not a base discriminator enum value.
            errors.append(self._error('%s: discriminator value does not match the base '
                                      'discriminator type', self))
            return errors

        # Validate the reference.
        errors += self._discriminator_value.validate()

        # The message must be defined after the discriminator enum.
        errors += self._validate_is_defined_after(dvalue.enum)
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
                errors.append(self._error('%s: duplicate subtype with a discriminator value %r',
                                          self, value.name))
            values.add(value)

        return errors

    def _validate_fields(self):
        errors = []

        # Prevent duplicate field names.
        names = set()
        for field in self.fields:
            if field.name in names:
                errors.append(self._error('%s: duplicate field %r', self, field.name))
            names.add(field.name)

        # Prevent multiple discriminator fields.
        discriminator = None
        for field in self.fields:
            if not field.is_discriminator:
                continue

            if discriminator:
                errors.append(self._error('%s: multiple discriminator fields', self))
                break  # One multiple discriminator error is enough.

            discriminator = field

        for field in self.fields:
            errors += field.validate()

        return errors


class Field(Located):
    '''Message field.'''
    def __init__(self, name, type0, is_discriminator=False, location=None):
        self.name = name
        self._type = references.reference(type0)
        self.is_discriminator = is_discriminator
        self.location = location

        self.message = None

    def __str__(self):
        return self.name

    def __repr__(self):
        return '<%s %s at %s>' % (self.__class__.__name__, self.name, hex(id(self)))

    @property
    def type(self):
        return self._type.dereference()

    def link(self, scope):
        return self._type.link(scope)

    def validate(self):
        errors = []

        if not self.type.is_data_type:
            errors.append(self._error('%s: field must be a data type', self))

        if self.is_discriminator and not self.type.is_enum:
            errors.append(self._error('%s: discriminator field must be an enum', self))

        # Validate the reference (it can be a collection).
        errors += self._type.validate()
        return errors
