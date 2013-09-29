# encoding: utf-8
import logging
from .definitions import Definition, Type, NativeTypes
from .validator import ValidatorError


class Interface(Definition):
    '''User-defined interface.'''
    def __init__(self, name, base=None, exc=None, declared_methods=None, doc=None, location=None):
        super(Interface, self).__init__(Type.INTERFACE, name, doc=doc, location=location)
        self.base = base
        self.exc = exc
        self.declared_methods = []

        if declared_methods:
            map(self.add_method, declared_methods)

    @property
    def methods(self):
        return self.inherited_methods + self.declared_methods

    @property
    def inherited_methods(self):
        if not self.base:
            return []
        return self.base.methods

    def add_method(self, method):
        '''Add a method to this interface.'''
        if method.interface:
            raise ValueError('Method is already in an interface, %s' % method)

        method.interface = self
        self.declared_methods.append(method)

        logging.debug('%s: added a method, method=%s', self, method)

    def create_method(self, name, result=NativeTypes.VOID, *args_tuples):
        '''Add a new method to this interface and return the method.'''
        method = Method(name, result)
        for arg_tuple in args_tuples:
            method.create_arg(*arg_tuple)

        self.add_method(method)
        return method

    def link(self, linker):
        '''Link the base, the exception and the methods.'''
        errors = []

        self.base, errors0 = linker.link(self.base)
        errors += errors0

        if self.base:
            self.base.link(linker)

        self.exc, errors0 = linker.link(self.exc)
        errors += errors0

        for method in self.declared_methods:
            errors += method.link(linker)

        return errors

    def validate(self):
        errors = []

        errors += self._validate_base()
        errors += self._validate_exc()
        errors += self._validate_methods()

        return errors

    def _validate_base(self):
        errors = []

        if not self.base:
            return []

        base = self.base
        if not base.is_interface:
            errors.append(ValidatorError(self, 'Base must be an interface'))

        errors += base._validate_is_defined_before(self)

        # Check circular inheritance.
        while base:
            if base is self:
                errors.append(ValidatorError(self, 'Circular inheritance'))
                break

            base = base.base

        return errors

    def _validate_exc(self):
        if not self.exc:
            return []

        if not self.exc.is_exception:
            return [ValidatorError(self, 'Wrong exception')]

        return []

    def _validate_methods(self):
        errors = []

        names = set()
        for method in self.methods:
            name = method.name
            if name in names:
                errors.append(ValidatorError(self, 'Duplicate method %r', name))

            names.add(name)

        for method in self.methods:
            errors += method.validate()

        return errors


class Method(object):
    '''Interface method.'''
    def __init__(self, name, args=None, result=None, is_index=False, is_post=False,
                 doc=None, location=None):
        self.name = name
        self.args = []
        self.result = result
        self.interface = None

        self.is_index = is_index
        self.is_post = is_post

        self.doc = doc
        self.location = location

        if args:
            map(self.add_arg, args)

    def __str__(self):
        return self.fullname

    @property
    def fullname(self):
        if not self.interface:
            return self.name

        return '%s.%s' % (self.interface.fullname, self.name)

    @property
    def is_remote(self):
        return not self.result.is_interface

    def add_arg(self, arg):
        '''Append an argument to this method.'''
        arg.method = self
        self.args.append(arg)

    def create_arg(self, name, definition):
        '''Create a new arg and add it to this method.'''
        arg = MethodArg(name, definition)
        self.add_arg(arg)
        return arg

    def link(self, linker):
        errors = []

        self.result, errors0 = linker.link(self.result)
        errors += errors0

        for arg in self.args:
            arg.type, errors0 = arg.link(linker)
            errors += errors0

        return errors

    def validate(self):
        errors = []

        if self.is_post and not self.is_remote:
            errors.append(ValidatorError(self,
                '@post method must be remote (return a data type or void)'))

        # Check that all form args fields do not clash with method arguments.
        names = {arg.name for arg in self.args}
        for arg in self.args:
            type0 = arg.type
            if not type0.is_message or not type0.is_form:
                continue

            # It's a form.
            for field in type0.fields:
                if field.name not in names:
                    continue

                errors.append(ValidatorError(self,
                    'Form fields clash with method args, form arg=%s', arg.name))
                break  # One error is enough

        for arg in self.args:
            errors += arg.validate()

        return errors


class MethodArg(object):
    '''Single method argument.'''
    def __init__(self, name, type0):
        self.name = name
        self.type = type0
        self.method = None

    @property
    def fullname(self):
        return '%s.%s' % (self.method, self.name)

    def link(self, linker):
        self.type, errors = linker.link(self.type)
        return errors

    def validate(self):
        if not self.type.is_datatype:
            return [ValidatorError(self, 'Argument must be a data type')]

        return []
