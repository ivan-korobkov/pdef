# encoding: utf-8
from pdef_compiler.generator import GeneratorCli as _GeneratorCli
from pdef_java.generator import JavaTranslator


class JavaGeneratorCli(_GeneratorCli):
    def get_name(self):
        return 'java'

    def fill_arg_group(self, group):
        group.add_argument('--java', help='output directory for java files')
        group.add_argument('--java-modules', dest='java_modules',
                           help='java package name mappings')

    def generate(self, args, package):
        out = args.java
        mapping = args.java_modules
        generator = JavaTranslator(out, mapping)
        generator.translate(package)


def generator_cli():
    return JavaGeneratorCli()
