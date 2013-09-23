# encoding: utf-8
from pdef_java.generator import JavaGenerator
from pdef_compiler.generator import GeneratorModule


class JavaGeneratorModule(GeneratorModule):
    def fill_cli_group(self, group):
        '''Fill a java source code generator argparse group.'''
        group.add_argument('--java', help='output directory for java files')
        group.add_argument('--java-modules', dest='java_modules',
                           help='java package name mappings')

    def create_generator(self, out, name_mapping=None, **kwargs):
        '''Create a java source code generator.'''
        return JavaGenerator(out, name_mapping)


def module():
    '''Create a java generator module interface.'''
    return JavaGeneratorModule()
