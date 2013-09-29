# encoding: utf-8
import logging
from collections import deque


class Type(object):
    '''Pdef type enum.'''

    # Base value types.
    BOOL = 'bool'
    INT16 = 'int16'
    INT32 = 'int32'
    INT64 = 'int64'
    FLOAT = 'float'
    DOUBLE = 'double'
    STRING = 'string'

    # Collection types.
    LIST = 'list'
    MAP = 'map'
    SET = 'set'

    # Special data type.
    OBJECT = 'object'

    # User defined data types.
    REFERENCE = 'reference'  # A special type.
    ENUM = 'enum'
    ENUM_VALUE = 'enum_value'
    MESSAGE = 'message'
    EXCEPTION = 'exception'

    # Interface and void.
    INTERFACE = 'interface'
    VOID = 'void'

    PRIMITIVES = (BOOL, INT16, INT32, INT64, FLOAT, DOUBLE, STRING)
    DATA_TYPES = PRIMITIVES + (OBJECT, LIST, MAP, SET, REFERENCE, ENUM, MESSAGE, EXCEPTION)


class Location(object):
    def __init__(self, path, line=0):
        self.path = path
        self.line = line

    def __str__(self):
        s = self.path if self.path else 'nofile'
        return '%s, line %s' % (s, self.line) if self.line else s


# === Packages and modules ===


class Package(object):
    '''Protocol definition.'''
    def __init__(self, modules=None):
        self.modules = []

        if modules:
            map(self.add_module, modules)

    def __str__(self):
        return 'package'

    def add_module(self, module):
        '''Add a module to this package.'''
        if module.package:
            raise ValueError('Module is already in a package, %s' % module)

        self.modules.append(module)
        module.package = self

        logging.debug('%s: added a module %s', self, module)

    def get_module(self, name):
        '''Find a module by its name.'''
        for module in self.modules:
            if module.name == name:
                return module

    def _validate(self):
        '''Validate the package.'''
        for module in self.modules:
            module.validate()


class Module(object):
    '''Module in a pdef package, usually, a module is parsed from one file.'''
    def __init__(self, name, imports=None, definitions=None):
        self.name = name

        self.imports = []
        self.imported_modules = []
        self.definitions = []
        self.imports_linked = False

        if imports:
            map(self.add_import, imports)

        if definitions:
            map(self.add_definition, definitions)

    def add_import(self, import0):
        '''Add a module import to this module.'''
        if import0.module:
            raise ValueError('Import is already in a module, %s' % import0)

        self.imports.append(import0)
        import0.module = self

    def add_imported_module(self, alias, module):
        '''Add an imported module to this module.'''
        imported = ImportedModule(alias, module)
        self.imported_modules.append(imported)
        return imported

    def get_imported_module(self, alias):
        '''Find a module by its import alias.'''
        for imported_module in self.imported_modules:
            if imported_module.alias == alias:
                return imported_module.module

    def add_definition(self, def0):
        '''Add a new definition to this module.'''
        if def0.module:
            raise ValueError('Definition is already in a module, def=%s,' % def0)

        self.definitions.append(def0)
        def0.module = self

        logging.debug('%s: added a definition, def=%s', self, def0.name)

    def add_definitions(self, *defs):
        '''Add definitions to this module.'''
        for def0 in defs:
            self.add_definition(def0)

    def get_definition(self, name):
        '''Find a definition in this module by a name.'''
        for d in self.definitions:
            if d.name == name:
                return d

    def _validate(self):
        '''Validate imports and definitions.'''
        # Check definition duplicates
        names = set()
        for def0 in self.definitions:
            name = def0.name
            self._check(name not in names, 'Duplicate definition, module=%s, def=%s', self, def0)
            names.add(name)

        # Check clashes with imports.
        for def0 in self.definitions:
            name = def0.name
            for import0 in self.imports:
                iname = import0.name
                self._check(iname != name and not iname.startswith(name + '.'),
                            'Definition clashes with an import, module=%s, def=%s', self, def0)

        for def0 in self.definitions:
            def0.validate()

    def _has_import_circle(self, another):
        '''Return true if this module has an import circle with another module.'''
        if another is self:
            return False

        q = deque(imp.module for imp in self.imports)
        while q:
            module = q.pop()
            if module is self:
                return True

            for imp in module.imports:
                q.append(imp.module)

        return False


# === Imports ===


class AbstractImport(object):
    def __init__(self):
        self.module = None
        self.module_names = ()


class AbsoluteImport(AbstractImport):
    def __init__(self, name):
        super(AbsoluteImport, self).__init__()

        self.name = name
        self.module_names = (name,)


class RelativeImport(AbstractImport):
    def __init__(self, prefix, relative_names):
        super(RelativeImport, self).__init__()

        self.prefix = prefix
        self.relative_names = relative_names
        self.module_names = tuple(prefix + '.' + name for name in relative_names)


class ImportedModule(object):
    '''Alias/module pair, i.e. from package.module import submodule.'''
    def __init__(self, alias, module):
        self.alias = alias
        self.module = module


# === Definitions ===


class Definition(object):
    '''Base definition.'''
    linked = False
    location = None
    is_exception = False  # The flag is set in a message constructor.

    def __init__(self, type0, name, doc=None, location=None):
        self.type = type0
        self.name = name
        self.doc = doc
        self.location = location

        self.module = None

        self.is_primitive = self.type in Type.PRIMITIVES
        self.is_datatype = self.type in Type.DATA_TYPES
        self.is_interface = self.type == Type.INTERFACE
        self.is_message = self.type == Type.MESSAGE

        self.is_enum = self.type == Type.ENUM

        self.is_list = self.type == Type.LIST
        self.is_set = self.type == Type.SET
        self.is_map = self.type == Type.MAP

    def __repr__(self):
        return '<%s %s>' % (self.__class__.__name__, self.fullname)

    def __str__(self):
        return self.name

    @property
    def fullname(self):
        return '%s.%s' % (self.module.name, self.name) if self.module else self.name

    def _must_be_referenced_before(self, another):
        '''Validate that this definition is reference before another one.'''
        if not self.module or not another.module:
            return True

        if another.module is self.module:
            # They are in the same module.

            for def0 in self.module.definitions:
                if def0 is self:
                    return True
                if def0 is another:
                    self._raise('%s must be referenced before %s. Move it above in the file.',
                                self, another)

            raise AssertionError('Wrong module state')

        if self.module._has_import_circle(another.module):
            self._raise('%s must be referenced before %s, but their modules circularly import '
                        'each other. Move %s into another module.',
                        self, another, self)

        return True

    def _must_be_referenced_after(self, another):
        '''Validate that this definition is referenced after another one.'''
        return another._must_be_referenced_before(self)


class NativeType(Definition):
    '''Native type definition, i.e. it defines a native language type such as string, int, etc.'''
    def __init__(self, type0):
        super(NativeType, self).__init__(type0, type0)


class NativeTypes(object):
    '''Native types.'''
    BOOL = NativeType(Type.BOOL)
    INT16 = NativeType(Type.INT16)
    INT32 = NativeType(Type.INT32)
    INT64 = NativeType(Type.INT64)
    FLOAT = NativeType(Type.FLOAT)
    DOUBLE = NativeType(Type.DOUBLE)
    STRING = NativeType(Type.STRING)

    OBJECT = NativeType(Type.OBJECT)
    VOID = NativeType(Type.VOID)

    _BY_TYPE = None

    @classmethod
    def get_by_type(cls, t):
        '''Returns a value by its type or none.'''
        if cls._BY_TYPE is None:
            cls._BY_TYPE = {}
            for k, v in cls.__dict__.items():
                if not isinstance(v, NativeType):
                    continue
                cls._BY_TYPE[v.type] = v

        return cls._BY_TYPE.get(t)


# === Enums ===


class Enum(Definition):
    '''Enum definition.'''
    def __init__(self, name, values=None):
        super(Enum, self).__init__(Type.ENUM, name)
        self.values = []

        if values:
            map(self.add_value, values)

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

    def _validate(self):
        names = set()
        for value in self.values:
            self._check(value.name not in names, 'Duplicate value, enum=%s, value=%s', self, value)
            names.add(value.name)


class EnumValue(object):
    '''Single enum value which has a name and a pointer to the declaring enum.'''
    def __init__(self, enum, name):
        self.enum = enum
        self.name = name


# === Messages and fields ===


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
        '''Add a new field to this message and return the field.'''
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

    def _validate(self):
        self._validate_base()
        self._validate_discriminator_value()
        self._validate_subtypes()
        self._validate_fields()

    def _validate_base(self):
        if not self.base:
            return

        base = self.base
        self._check(base.is_message, 'Base must be a message, message=%s', self)
        self._check(self.is_exception == base.is_exception,
                    'Wrong base type (message/exc), message=%s', self)
        self._must_be_referenced_after(base)

        # Check circular inheritance.
        while base:
            self._check(base is not self, 'Circular inheritance, message=%s', self)
            base = base.base

    def _validate_discriminator_value(self):
        base = self.base
        dvalue = self.discriminator_value

        if not dvalue:
            is_polymorphic = base and base.discriminator
            self._check(not is_polymorphic, 'Discriminator value required for %s base', self)
            return

        self._check(dvalue.is_enum_value,
                    'Discriminator value must be an enum value, message=%s', self)

        self._check(base.discriminator,
                    'Cannot set a discriminator value, '
                    'the base does not have a discriminator, message=%s', self)

        self._must_be_referenced_after(dvalue)

    def _validate_subtypes(self):
        if not self.subtypes:
            return

        dvalues = set()
        for subtype in self.subtypes:
            dvalue = subtype.discriminator_value

            # Check that the value is a discriminator enum instance.
            self._check(dvalue in self.discriminator.type,
                        'Wrong discriminator value type, message=%s, subtype=%s', self, subtype)

            self._check(dvalue not in dvalues,
                        'Duplicate discriminator value, message=%s, subtype=%s', self, dvalue)
            dvalues.add(dvalue)

    def _validate_fields(self):
        names = set()
        for field in self.fields:
            self._check(not field.name in names, 'Duplicate field, message=%s, field%s', self,
                        field.name)
            names.add(field.name)

        discriminator = None
        for field in self.fields:
            if field.is_discriminator:
                self._check(not discriminator, 'Multiple discriminator fields, message=%s', self)
                discriminator = field


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

    def _validate(self):
        self._check(self.type.is_datatype, 'Field must be a data type, field=%s', self)

        if self.is_discriminator:
            self._check(self.type.is_enum, 'Discriminator field must be an enum, field=%s', self)


# === Interfaces and methods ===


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

    def _validate(self):
        self._validate_base()
        self._validate_exc()
        self._validate_methods()

    def _validate_base(self):
        if not self.base:
            return

        base = self.base
        self._check(base.is_interface, 'Base must be an interface, interface=%s', self)
        self._must_be_referenced_after(base)

        # Check circular inheritance.
        while base:
            self._check(base is not self, 'Circular inheritance, interface=%s', self)
            base = base.base

    def _validate_exc(self):
        if not self.exc:
            return
        self._check(self.exc.is_exception, 'Wrong exception, interface=%s', self)

    def _validate_methods(self):
        names = set()
        for method in self.methods:
            self._check(method.name not in names,
                        'Duplicate method, interface=%s, method=%s', self, method.name)
            names.add(method.name)

        for method in self.methods:
            method.validate()


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

    def _validate(self):
        for arg in self.args:
            arg.validate()

        if self.is_post:
            self._check(self.is_remote, 'Only remote methods can be @post, method=%s', self)

        # Check that all form args fields do not clash with method arguments.
        names = {arg.name for arg in self.args}
        for arg in self.args:
            type0 = arg.type
            if not type0.is_message or not type0.is_form:
                continue

            for field in type0.fields:
                self._check(field.name not in names,
                            'Form fields clash with method args, method=%s, form_arg=%s, field=%s',
                            self, arg, field.name)


class MethodArg(object):
    '''Single method argument.'''
    def __init__(self, name, type0):
        self.name = name
        self.type = type0
        self.method = None

    @property
    def fullname(self):
        return '%s.%s' % (self.method, self.name)

    def _validate(self):
        self._check(self.type.is_datatype, 'Argument must be a data type, arg=%s', self)


# === Collections ===


class List(Definition):
    '''List definition.'''
    def __init__(self, element):
        super(List, self).__init__(Type.LIST, 'list')
        self.element = element

    def _validate(self):
        self._check(self.element.is_datatype, 'List elements must be data types, %s', self)


class Set(Definition):
    '''Set definition.'''
    def __init__(self, element):
        super(Set, self).__init__(Type.SET, 'set')
        self.element = element

    def _validate(self):
        self._check(self.element.is_datatype, 'Set elements must be data types, %s', self)


class Map(Definition):
    '''Map definition.'''
    def __init__(self, key, value):
        super(Map, self).__init__(Type.MAP, 'map')
        self.key = key
        self.value = value

    def _validate(self):
        self._check(self.key.is_primitive, 'Map keys must be primitives, %s', self)
        self._check(self.value.is_datatype, 'Map values must be data types, %s', self)


# === References ===


class Reference(Definition):
    def __init__(self, name):
        super(Reference, self).__init__(Type.REFERENCE, name)


class ListReference(Reference):
    def __init__(self, element):
        super(ListReference, self).__init__(Type.LIST)
        self.element = element


class SetReference(Reference):
    def __init__(self, element):
        super(SetReference, self).__init__(Type.SET)
        self.element = element


class MapReference(Reference):
    def __init__(self, key, value):
        super(MapReference, self).__init__(Type.MAP)
        self.key = key
        self.value = value
