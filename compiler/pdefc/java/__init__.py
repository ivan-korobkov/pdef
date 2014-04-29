# encoding: utf-8
from __future__ import unicode_literals
import os.path

from pdefc import lang, __version__
from pdefc.templates import Templates, write_file


ENUM_TEMPLATE = 'enum.jinja2'
STRUCT_TEMPLATE = 'struct.jinja2'
INTERFACE_TEMPLATE = 'interface.jinja2'

STRUCT_SUFFIX = 'Struct'
INTERFACE_SUFFIX = 'Interface'
GENERATED_BY = 'Generated by Pdef compiler %s. DO NOT EDIT.' % __version__


def generate(package, dst, jpackage_name):
    generator = Generator(jpackage_name)
    return generator.generate(package, dst)


class Generator(object):
    def __init__(self, package_name=None, struct_suffix=STRUCT_SUFFIX, 
                 iface_suffix=INTERFACE_SUFFIX):
        self.package_name = package_name
        self.struct_suffix = struct_suffix or ''
        self.iface_suffix = iface_suffix or ''
        
        self.templates = Templates(__file__, filters=self)

    def generate(self, package, dst):
        for file in package.files:
            for type0 in file.types:
                code = self._render(type0, self.templates)
                filepath = self._filepath(type0)
                write_file(dst, filepath, code)

    def _render(self, type0, templates):
        name = self.jname(type0)
        
        if type0.is_enum:
            return templates.render(ENUM_TEMPLATE, enum=type0, name=name,
                                    generated_by=GENERATED_BY)
        elif type0.is_struct:
            return templates.render(STRUCT_TEMPLATE, struct=type0, name=name,
                                    generated_by=GENERATED_BY)
        elif type0.is_interface:
            return templates.render(INTERFACE_TEMPLATE, interface=type0, name=name,
                                    generated_by=GENERATED_BY)

        raise ValueError('Unsupported definition %r' % type0)


    def _filepath(self, type0):
        package = self.jpackage(type0)
        dirs = package.split('.')
        dirpath = os.path.join(*dirs)
        filename = '%s.java' % type0.name
        return os.path.join(dirpath, filename)

    def jpackage(self, type0):
        name = type0.file.dotname
        if not self.package_name:
            return name

        return '%s.%s' % (self.package_name, name)

    def jname(self, type0):
        name = type0.name
        
        if type0.is_struct:
            return name if name.endswith(self.struct_suffix) else name + self.struct_suffix
        
        if type0.is_interface:
            return name if name.endswith(self.iface_suffix) else name + self.iface_suffix
        
        return type0.name

    def jtype(self, type0):
        if type0 in _TYPES:
            return _TYPES[type0]

        if isinstance(type0, lang.List):
            return 'java.util.List<%s>' % (self.jtype_boxed(type0.element))

        if isinstance(type0, lang.Set):
            return 'java.util.Set<%s>' % (self.jtype_boxed(type0.element))

        if isinstance(type0, lang.Map):
            return 'java.util.Map<%s, %s>' % (self.jtype_boxed(type0.key),
                                              self.jtype_boxed(type0.value))

        if isinstance(type0, lang.EnumValue):
            return '%s.%s' % (self.jtype(type0.enum), type0.name)

        package = self.jpackage(type0)
        name = self.jname(type0)
        return '%s.%s' % (package, name)

    def jtype_boxed(self, type0):
        if type0 in _BOXED_TYPES:
            return _BOXED_TYPES[type0]

        return self.jtype(type0)
    
    def is_jobject(self, type0):
        return type0.is_string \
               or type0.is_datetime \
               or type0.is_enum \
               or type0.is_collection \
               or type0.is_struct \
               or type0.is_interface


_TYPES = {
    lang.BOOL: 'boolean',
    lang.INT16: 'short',
    lang.INT32: 'int',
    lang.INT64: 'long',
    lang.FLOAT: 'float',
    lang.DOUBLE: 'double',
    lang.VOID: 'void',
    lang.STRING: 'String',
    lang.DATETIME: 'java.util.Date',
}

_BOXED_TYPES = {
    lang.BOOL: 'Boolean',
    lang.INT16: 'Short',
    lang.INT32: 'Integer',
    lang.INT64: 'Long',
    lang.FLOAT: 'Float',
    lang.DOUBLE: 'Double',
    lang.VOID: 'Void'
}
