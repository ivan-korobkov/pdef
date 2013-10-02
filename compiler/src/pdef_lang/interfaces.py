# encoding: utf-8
import logging
from pdef_lang import definitions, validation, references


class Interface(definitions.Definition):
    '''User-defined interface.'''
    def __init__(self, name, base=None, exc=None, declared_methods=None, doc=None, location=None):
        super(Interface, self).__init__(definitions.Type.INTERFACE, name, doc=doc,
                                        location=location)

        self.base = base
        self.exc = exc
        self.declared_methods = []

        if declared_methods:
            map(self.add_method, declared_methods)

    @property
    def base(self):
        return self._base.dereference()

    @base.setter
    def base(self, value):
        self._base = references.reference(value)

    @property
    def exc(self):
        return self._exc.dereference()

    @exc.setter
    def exc(self, value):
        self._exc = references.reference(value)

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

    def create_method(self, name, result=definitions.NativeTypes.VOID, *arg_tuples):
        '''Add a new method to this interface and return the method.'''
        method = Method(name, result=result)
        for arg_tuple in arg_tuples:
            method.create_arg(*arg_tuple)

        self.add_method(method)
        return method

    def link(self, linker):
        '''Link the base, the exception and the methods.'''
        errors = []
        errors += self._base.link(linker)
        errors += self._exc.link(linker)

        for method in self.declared_methods:
            errors += method.link(linker)

        return errors

    def validate(self):
        errors = []
        errors += self._validate_base()

        if not errors:
            # Cannot validate methods if the base is wrong.
            errors += self._validate_methods()

        errors += self._validate_exc()
        return errors

    def _validate_base(self):
        if not self.base:
            return []

        if not self.base.is_interface:
            return [validation.error(self, 'base must be an interface')]

        # The base is in interface, continue validation.
        errors = []
        errors += self.base._validate_is_defined_before(self)

        # Prevent circular inheritance.
        base = self.base
        while base:
            if base is self:
                errors.append(validation.error(self, 'circular inheritance'))
                break
            base = base.base

        return errors

    def _validate_exc(self):
        if self.exc and not self.exc.is_exception:
            return [validation.error(self, 'interface exc must be an exception, got %s', self.exc)]

        return []

    def _validate_methods(self):
        errors = []

        names = set()
        for method in self.methods:
            if method.name in names:
                errors.append(validation.error(self, 'duplicate method %r', method.name))
            names.add(method.name)

        for method in self.methods:
            errors += method.validate()

        return errors


class Method(object):
    '''Interface method.'''
    def __init__(self, name, result=definitions.NativeTypes.VOID, args=None, is_index=False,
                 is_post=False, doc=None, location=None):
        self.name = name
        self.args = []
        self.result = result

        self.is_index = is_index
        self.is_post = is_post

        self.doc = doc
        self.location = location

        self.interface = None

        if args:
            map(self.add_arg, args)

    def __str__(self):
        return self.name

    def __repr__(self):
        return '<%s %s at %s>' % (self.__class__.__name__, self.name, hex(id(self)))

    @property
    def result(self):
        return self._result.dereference()

    @result.setter
    def result(self, value):
        self._result = references.reference(value)

    @property
    def is_remote(self):
        return self.result and (not self.result.is_interface)

    def add_arg(self, arg):
        '''Append an argument to this method.'''
        if arg.method:
            raise ValueError('Argument is already in a method, %s' % arg)

        arg.method = self
        self.args.append(arg)

    def create_arg(self, name, definition):
        '''Create a new arg and add it to this method.'''
        arg = MethodArg(name, definition)
        self.add_arg(arg)
        return arg

    def link(self, linker):
        errors = []
        errors += self._result.link(linker)

        for arg in self.args:
            errors += arg.link(linker)

        return errors

    def validate(self):
        errors = []

        if self.is_post and not self.is_remote:
            errors.append(validation.error(self, '@post method must be remote (return a data type '
                                                 'or void)'))

        # Prevent duplicate arguments.
        names = set()
        for arg in self.args:
            if arg.name in names:
                errors.append(validation.error(self, 'duplicate argument %r', arg.name))
            names.add(arg.name)

        # Prevent form arg fields and arguments name clashes.
        for arg in self.args:
            type0 = arg.type
            if not (type0.is_message and type0.is_form):
                continue

            # It's a form.
            for field in type0.fields:
                if field.name not in names:
                    continue

                errors.append(validation.error(self, 'form fields clash with method args, '
                                                     'form arg=%s', arg.name))
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
    def type(self):
        return self._type.dereference()

    @type.setter
    def type(self, value):
        self._type = references.reference(value)

    @property
    def fullname(self):
        return '%s.%s' % (self.method, self.name)

    def link(self, linker):
        return self._type.link(linker)

    def validate(self):
        if not self.type.is_data_type:
            return [validation.error(self, 'argument must be a data type')]

        return []
