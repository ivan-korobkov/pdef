# encoding: utf-8
import logging
import os.path
from pdef.lang import Message, Enum, Interface
from pdef.java.datatypes import JavaMessage, JavaEnum
from pdef.java.interfaces import JavaInterface


class JavaPackage(object):
    def __init__(self, package):
        self.package = package
        self.files = []

        for module in package.modules:
            for definition in module.definitions:
                self._add_definition(definition)

    def _add_definition(self, definition):
        if isinstance(definition, Message):
            jdef = JavaMessage(definition)
        elif isinstance(definition, Enum):
            jdef = JavaEnum(definition)
        elif isinstance(definition, Interface):
            jdef = JavaInterface(definition)
        else:
            return

        file = JavaFile(jdef)
        self.files.append(file)

    def write_to(self, outdir):
        for file in self.files:
            file.write_to(outdir)


class JavaFile(object):
    def __init__(self, jdef):
        self.jdef = jdef

    @property
    def directory(self):
        dirs = self.jdef.package.split('.')
        return os.path.join(*dirs)

    @property
    def filename(self):
        return '%s.java' % self.jdef.name

    def write_to(self, outdir):
        fulldir = os.path.join(outdir, self.directory)
        mkdir_p(fulldir)

        fullpath = os.path.join(fulldir, self.filename)

        code = self.jdef.code
        with open(fullpath, 'wt') as f:
            f.write(code)

        logging.info('Created %s', fullpath)


def mkdir_p(dirname):
    if os.path.exists(dirname):
        return
    os.makedirs(dirname)
