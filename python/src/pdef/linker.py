# encoding: utf-8
from collections import deque
import logging

from pdef import lang
from pdef.preconditions import *


class FieldCompiler(object):

    def __init__(self, pool):
        self.pool = check_not_none(pool)
        self.errors = []

    def _error(self, msg, *args):
        self.errors.append(msg % args)
        logging.error(msg, *args)

    def compile(self):
        self.compiled_bases = set()
        for package in self.pool.packages:
            for definition in package.definitions:
                if isinstance(definition, lang.Message):
                    self._compile_base(definition, package)
        for special in self.pool.specials:
            if isinstance(special, lang.Message):
                self._compile_base(special, special.package)
        if self.errors:
            return

        for package in self.pool.packages:
            for definition in package.definitions:
                if isinstance(definition, lang.Message):
                    self._compile_type_base(definition, package)
        for special in self.pool.specials:
            if isinstance(special, lang.Message):
                self._compile_type_base(special, special.package)
        if self.errors:
            return

        for package in self.pool.packages:
            for definition in package.definitions:
                if isinstance(definition, lang.Message):
                    self._compile_subtypes(definition, package)
        for special in self.pool.specials:
            if isinstance(special, lang.Message):
                self._compile_subtypes(special, special.package)
        if self.errors:
            return

        for package in self.pool.packages:
            for definition in package.definitions:
                if isinstance(definition, lang.Message):
                    self._compile_all_fields(definition, package)
        for special in self.pool.specials:
            if isinstance(special, lang.Message):
                self._compile_all_fields(special, special.package)

    def _compile_base(self, message, package):
        # Can be called multiple times for the same message.
        if message in self.compiled_bases:
            return

        self.compiled_bases.add(message)
        if not message.base:
            message.all_bases = []
            return

        message.all_bases = []
        base = message.base
        if base:
            if base == message:
                self._error('%s: circular inheritance %s', message, package)
                return

            self._compile_base(base, package) # TODO: (base, base.package)
            for subbase in base.all_bases:
                if subbase == message:
                    self._error('%s: circular inheritance %s', message, package)
                    return

                if subbase in message.all_bases:
                    continue
                message.all_bases.append(subbase)
            message.all_bases.append(base)

    def _compile_type_base(self, message, package):
        type_field_name = message.options.type_field
        if not type_field_name:
            message.is_type_base = False
            return

        type_field = message.declared_field_map.get(type_field_name)
        if not type_field:
            self._error('%s.%s: no type field named "%s"', package, message, type_field_name)
            return

        if message.variables:
            self._error("%s.%s: generic messages cannot have type fields, %s",
                        package, message, type_field)
            return

        message.is_type_base = True
        message.type_field = type_field
        field_name = type_field.name

        # Check, that all its bases do not have such a field.
        for base in message.all_bases:
            present = base.declared_field_map.get(field_name)
            if not present:
                continue
            self._error('%s.%s: cannot override a type field %s from %s with %s',
                        package, message, present, base, type_field)
            break

        # Ensure, that the type field has a specified value.
        if message.type_field and not message.type_field.value:
            self._error("%s.%s: Type field %s must have a specified value",
                        package, message, message.type_field)
            return

        # So, the message is a typed one.
        type_field.read_only = True
        type_field.is_type_field = True
        type_field.is_type_base_field = True
        message.subtypes = [message]
        message.subtype_map = {message.type_field.value: message}

    def _compile_subtypes(self, message, package):
        # All messages have correct type_fields here.
        # Ensure that all subtypes have correct type values.
        base = message.base
        if not base or not base.is_type_base:
            return

        base_type_field = base.type_field
        # Check, that this type has the same field with a value.

        field_name = base_type_field.name
        type_field = message.declared_field_map.get(field_name)
        if not type_field:
            self._error("%s.%s: is a wrong subtype, it must have a type field %s with a specified value",
                        package, message, base_type_field)
            return

        if type_field.type != base_type_field.type:
            self._error("%s.%s: subtype field %s must have the same type as base type field %s",
                        package, message, type_field, base_type_field)
            return

        if not type_field.value:
            self._error("%s.%s: subtype field %s must have a value of type %s",
                        package, message, type_field, base_type_field.type)
            return

        # The type, name and value of the subtype field are OK.
        # Save the message as a subtype of the base.
        value = type_field.value
        present = base.subtype_map.get(value)
        if present:
            self._error('%s.%s: subtype value "%s" clashes with %s, field %s',
                        package, message, value, present, type_field)
            return

        type_field.read_only = True
        type_field.is_type_field = True
        base.subtypes.append(message)
        base.subtype_map[value] = message

    def _compile_all_fields(self, message, package):
        message.fields = []

        for base in message.all_bases:
            for field in base.declared_fields:
                self._add_field(field, message, package)

        for field in message.declared_fields:
            self._add_field(field, message, package, declared=True)

    def _add_field(self, field, message, package, declared=False):
        if field.is_type_field and not declared:
            # Skip all type fields except for the declared ones, because they override the base ones.
            return

        present_field = message.field_map.get(field.name)
        if present_field:
            self._error('%s.%s: %s clashes with %s', package, message, field, present_field)
            return

        message.fields.append(field)
        message.field_map[field.name] = field
