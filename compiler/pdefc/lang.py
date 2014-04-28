# encoding: utf-8
import re


class Node(object):
    doc = None
    location = None

    def __init__(self):
        self.children = []

    def add_child(self, child):
        self.children.append(child)
        return child

    def create_reference(self, type_or_name):
        return self.add_child(Reference(type_or_name))

    def walk(self):
        yield self
        for child in self.children:
            for x in child.walk():
                yield x

    def link(self, errors, scope):
        '''Link this node.'''
        pass

    def validate(self, errors):
        '''Validate this node.'''
        pass


class Package(Node):
    '''Protocol definition.'''
    name_pattern = re.compile(r'^[a-zA-Z]{1}[a-zA-Z0-9_\-]*$')

    def __init__(self, name, files=None):
        super(Package, self).__init__()

        self.name = name
        self.files = []
        for file in files or ():
            self.add_file(file)

    def __str__(self):
        return self.name

    def __repr__(self):
        return '<Package %r at %s>' % (self.name, hex(id(self)))

    def add_file(self, file):
        if file.package:
            raise ValueError('Cannot add a file to %s, the file is already in a package, %s',
                             self, file)

        self.files.append(file)
        self.add_child(file)
        file.package = self

    def compile(self):
        '''Compile this package and return errors.'''
        errors = Errors()

        # Prevent duplicate types.
        if self._has_duplicate_types(errors):
            return errors

        # Link.
        for node in self.walk():
            node.link(errors, self)
        if errors:
            return errors

        # Validate.
        for node in self.walk():
            node.validate(errors)

        return errors

    def _has_duplicate_types(self, errors):
        names = set()
        for file in self.files:
            for def0 in file.definitions:
                errors.assert_that(def0.name not in names, def0.location,
                                   'duplicate package type "%s"', def0.name)
                names.add(file.name)
        return errors


class File(Node):
    name_pattern = re.compile(r'^[a-zA-Z]{1}[a-zA-Z0-9_]*(\.[a-zA-Z]{1}[a-zA-Z0-9_]*)*$')

    def __init__(self, name, types=None, path=None):
        super(File, self).__init__()

        self.name = name
        self.path = path
        self.package = None

        self.types = []
        for def0 in types or ():
            self.add_type(def0)

    def __str__(self):
        return self.name

    def __repr__(self):
        return '<%s %s at %s>' % (self.__class__.__name__, self.name, hex(id(self)))

    def add_type(self, type0):
        '''Add a new type to this file.'''
        if type0.file:
            raise ValueError('Cannot add a definition to %s, the definition is already in an '
                             'file, %s', self, type0)

        self.types.append(type0)
        self.add_child(type0)
        type0.file = self

        return type0

    def validate(self, errors):
        errors.assert_that(
            self.name_pattern.match(self.name), self.location,
            'Wrong file name "%s". File names must contain only latin letters, digits and '
            'underscores, and must start with a letter."' % self.name)


class Type(Node):
    def __init__(self, name):
        super(Type, self).__init__()
        self.name = name
        self.file = None
        self.is_exception = False

    def __str__(self):
        return self.name

    def __repr__(self):
        return '<%s %s at %s>' % (self.__class__.__name__, self.name, hex(id(self)))

    @property
    def fullname(self):
        return self.file.fullname + '.' + self.name if self.file.name else self.name

    @property
    def is_primitive(self):
        return self in (BOOL, INT16, INT32, INT64, FLOAT, DOUBLE, STRING, DATETIME)

    @property
    def is_data_type(self):
        return not isinstance(self, Interface)

    @property
    def referenced_types(self):
        '''Return a set of all types referenced in this definition (in fields, methods, etc).'''
        types = set()
        for node in self.walk():
            if isinstance(node, Type):
                types.add(node)

        types.discard(self)
        return types


class List(Type):
    element = ReferenceProperty('_element')

    def __init__(self, element):
        super(List, self).__init__('list')
        self._element = self.create_reference(element)

    def __repr__(self):
        return '<List %s>' % self.element

    def __str__(self):
        return 'list<%s>' % self.element

    def validate(self, errors):
        errors.assert_that(self.element.is_data_type, self.location,
                           'List element must be a data type')


class Set(Type):
    element = ReferenceProperty('_element')

    def __init__(self, element):
        super(Set, self).__init__('set')
        self._element = self.create_reference(element)

    def __repr__(self):
        return '<Set %s>' % self.element

    def __str__(self):
        return 'set<%s>' % self.element

    def validate(self, errors):
        errors.assert_that(self.element.is_data_type, self.location,
                           'Set element must be a data type')


class Map(Type):
    key = ReferenceProperty('_key')
    value = ReferenceProperty('_value')

    def __init__(self, key, value):
        super(Map, self).__init__('map')
        self._key = self.create_reference(key)
        self._value = self.create_reference(value)

    def __repr__(self):
        return '<Map %s, %s>' % (self.key, self.value)

    def __str__(self):
        return 'map<%s, %s>' % (self.key, self.value)

    def validate(self, errors):
        errors.assert_that(self.key.is_primitive, self.location, 'Map key must be a primitive')
        errors.assert_that(self.value.is_data_type, self.location, 'Map value must be a data type')


class Enum(Type):
    def __init__(self, name, values=None):
        super(Enum, self).__init__(name)
        self.values = []

        for value in values or ():
            self.create_value(value)

    def add_value(self, value):
        '''Add a value to this enum.'''
        if value.enum:
            raise ValueError('Cannot add an enum value to %s, the value is already in an enum, %s',
                             self, value)

        self.values.append(value)
        self.add_child(value)
        value.enum = self

        return value

    def create_value(self, value_or_name):
        '''Create a new enum value by its name, add it to this enum, and return it.'''
        if isinstance(value_or_name, EnumValue):
            return self.add_value(value_or_name)

        return self.add_value(EnumValue(value_or_name))

    def validate(self, errors):
        names = set()
        for value in self.values:
            if value.name in names:
                errors.add_error(value.location, 'Duplicate enum value "%s"', value.name)
                continue

            names.add(value.name)


class EnumValue(Node):
    def __init__(self, name):
        super(EnumValue, self).__init__()
        self.name = name
        self.enum = None


class Struct(Type):
    def __init__(self, name, fields=None, is_exception=False):
        super(Struct, self).__init__(name)

        self.fields = []
        self.is_exception = is_exception

        for field in fields or ():
            self.add_field(field)

    def add_field(self, field):
        '''Add a new field to this message and return the field.'''
        if field.struct:
            raise ValueError('Cannot add a field to %s, the field is already in another '
                             'struct, %s', self, field)

        self.fields.append(field)
        self.add_child(field)
        field.struct = field

        return field

    def create_field(self, name, type0):
        '''Create a new field, add it to struct message and return.'''
        field = Field(name, type0)
        return self.add_field(field)

    def validate(self, errors):
        # Prevent duplicate field names.
        names = set()
        for field in self.fields:
            if field.name in names:
                errors.add_error(field.location, 'Duplicate field "%s"', field.name)
                continue
            names.add(field.name)


class Field(Node):
    type = ReferenceProperty('_type')

    def __init__(self, name, type0, struct=None, location=None):
        super(Field, self).__init__()
        self.name = name
        self._type = self.create_reference(type0)
        self.struct = struct
        self.location = location

    def __str__(self):
        return self.name

    def __repr__(self):
        return '<%s %s at %s>' % (self.__class__.__name__, self.name, hex(id(self)))

    def validate(self, errors):
        errors.assert_that(self.type.is_data_type, self.location, 'Field type must be a data type')


class Interface(Type):
    def __init__(self, name, methods=None):
        super(Interface, self).__init__(name)
        self.methods = []

        for method in methods or ():
            self.add_method(method)

    def add_method(self, method):
        '''Add a method to this interface.'''
        if method.interface:
            raise ValueError('Cannot add a method to %s, the method is already in '
                             'an interface: %s', self, method)

        self.methods.append(method)
        self.add_child(method)
        method.interface = self

        return method

    def create_method(self, name, result=None, args=None, is_post=False):
        '''Add a new method to this interface and return the method.'''
        method = Method(name, result=result, args=args, is_post=is_post)
        return self.add_method(method)

    def validate(self, errors):
        # Prevent duplicate methods.
        names = set()
        for method in self.methods:
            if method.name in names:
                errors.add_error(method.location, 'Duplicate method "%s"', method.name)

            names.add(method.name)


class Method(Node):
    result = ReferenceProperty('_result')

    def __init__(self, name, result=None, args=None, is_post=False):
        super(Method, self).__init__()
        self.name = name
        self.args = []
        self._result = self.create_reference(result or VOID)
        self.is_post = is_post

        self.interface = None

        for arg in args or ():
            self.add_arg(arg)

    def __str__(self):
        return self.name

    def __repr__(self):
        return '<%s %s at %s>' % (self.__class__.__name__, self.name, hex(id(self)))

    def add_arg(self, arg):
        '''Append an argument to this method.'''
        if arg.method:
            raise ValueError('Argument is already in a method, %s' % arg)

        self.args.append(arg)
        return arg

    def create_arg(self, name, type):
        '''Create a new arg and add it to this method.'''
        arg = Argument(name, type)
        return self.add_arg(arg)

    def validate(self, errors):
        # Assert post methods have data type results.
        if self.is_post:
            errors.assert_that(self.result.is_data_type, self.location,
                               'POST method must have a data type result or be void')

        # Prevent duplicate arguments.
        names = set()
        for arg in self.args:
            if arg.name in names:
                errors.add_error(arg.location, 'Duplicate method argument "%s"', arg.name)
            names.add(arg.name)


class Argument(Node):
    type = ReferenceProperty('_type')

    def __init__(self, name, type0):
        super(Argument, self).__init__()

        self.name = name
        self._type = self.create_reference(type0)
        self.method = None

    def __str__(self):
        return self.name

    def __repr__(self):
        return '<%s %s at %s>' % (self.__class__.__name__, self.name, hex(id(self)))

    def validate(self, errors):
        errors.assert_that(self.type.is_data_type, 'argument must be a data type')


class Reference(Node):
    '''Type reference.'''

    def __init__(self, type_or_name=None):
        super(Reference, self).__init__()

        self.name = None
        self.type = None
        self.set(type_or_name)

    def __bool__(self):
        return bool(self.type)

    def __nonzero__(self):
        return bool(self.type)

    def set(self, type_or_name):
        is_type = isinstance(type_or_name, Type)
        self.name = None if is_type else type_or_name
        self.type = type_or_name if is_type else None

    def dereference(self):
        '''Return a type this references points to or raise ValueError when not linked.'''
        if not self.type:
            raise ValueError('Reference is not linked: %s' % self)
        return self.type

    def link(self, errors, scope):
        '''Link this reference in a scope.'''
        if self.type:
            return

        self.type = scope.find(self.name)
        errors.assert_that(self.type is not None, self.location,
                           'Symbol not found "%s"', self.name)


class ReferenceProperty(object):
    def __init__(self, name):
        self.name = name

    def __get__(self, instance, owner):
        ref = getattr(instance, self.name)
        return ref.dereference()

    def __set__(self, instance, value):
        ref = getattr(instance, self.name)
        ref.set(value)


class Location(object):
    def __init__(self, path, lineno):
        self.path = path
        self.lineno = lineno

    def __str__(self):
        return '%s: line %s' % (self.path, self.lineno)

    def __repr__(self):
        return '<%s %s at %s>' % (self.__class__.__name__, self.lineno, hex(id(self)))


class Errors(object):
    def __init__(self):
        self.errors = []

    def __bool__(self):
        return bool(self.errors)

    def __nonzero__(self):
        return bool(self.errors)

    def __iter__(self):
        return iter(self.errors)

    def __str__(self):
        return '\n'.join(self.errors)

    def assert_that(self, expression, location, message, *args):
        if expression:
            return

        self.add_error(location, message, args)

    def add_error(self, location, message, *args):
        msg = message % args
        error = '%s: %s' % (location, msg)
        self.errors.append(error)


BOOL = Type('bool')
INT16 = Type('int16')
INT32 = Type('int32')
INT64 = Type('int64')
FLOAT = Type('float')
DOUBLE = Type('double')
STRING = Type('string')
DATETIME = Type('datetime')
VOID = Type('void')
