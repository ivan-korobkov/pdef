# encoding: utf-8
from distutils.core import setup

setup(
    name='pdef-compiler',
    version='1.0-alpha9',
    url='http://github.com/ivan-korobkov/pdef',
    description='Pdef compiler',
    license='Apache License 2.0',

    author='Ivan Korobkov',
    author_email='ivan.korobkov@gmail.com',

    package_dir={'': 'src'},
    packages=['pdef_compiler', 'pdef_compiler.java', 'pdef_compiler.python'],
    package_data={
        'pdef_compiler.java': ['*.template'],
        'pdef_compiler.python': ['*.template']},

    scripts=['scripts/pdefc'],
    requires=['argparse', 'jinja2', 'ply']
)
