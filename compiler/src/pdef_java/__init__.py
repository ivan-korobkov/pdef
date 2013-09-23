# encoding: utf-8
from pdef_compiler.generator import GeneratorModule
from pdef_java.generator import JavaGenerator


class JavaGeneratorModule(GeneratorModule):
    def fill_cli_group(self, group):
        '''Fill a java source code generator argparse group.'''
        group.add_argument('--java', help='output directory for java files')
        group.add_argument('--java-module', dest='java_modules', action='append',
                           help='java package name mappings')

    def create_generator_from_cli_args(self, args):
        if not hasattr(args, 'java'):
            return

        out, module_name_map = self._parse_cli_args(args)
        return JavaGenerator(out, module_name_map)

    def _parse_cli_args(self, args):
        modules = args.java_modules
        module_name_map = dict(s.split(':') for s in modules) if modules else {}
        out = args.java
        return out, module_name_map


def module():
    '''Create a java generator module interface.'''
    return JavaGeneratorModule()
