# encoding: utf-8
from pdef_code.generator import GeneratorModule
from pdef_python.generator import PythonGenerator


def create_generator_module():
    '''Create a python generator module interface.'''
    return PythonGeneratorModule()


class PythonGeneratorModule(GeneratorModule):
    '''PythonGeneratorModule is a command interface to a python generator.'''
    def get_name(self):
        return 'python'

    def fill_cli_group(self, group):
        '''Fill a python source code generator argparse group.'''
        group.add_argument('--python', help='output directory for python files')
        group.add_argument('--python-module', dest='python_modules', action='append',
                           help='python package name mappings')

    def create_generator_from_cli_args(self, args):
        if not args.python:
            return

        out, module_name_map = self._parse_cli_args(args)
        return PythonGenerator(out, module_name_map)

    def _parse_cli_args(self, args):
        modules = args.python_modules
        module_name_map = dict(s.split(':') for s in modules) if modules else {}
        out = args.python
        return out, module_name_map
