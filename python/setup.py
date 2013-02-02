# encoding: utf-8
from distutils.core import setup


setup(
    name='pdef',
    version='1.0-alpha1',
    url='http://github.com/ivan-korobkov/pdef',
    author='Ivan Korobkov',
    author_email='ivan.korobkov@gmail.com',
    description='Pdef interface language',
    license='Apache 2.9',
    package_dir={'': 'src'},
    packages=['pdef', 'pdef.translators'],
    package_data={'pdef': ['translators/*.jinja2', 'builtin.pdef']},
    scripts=['scripts/pdefc'],
    requires=['argparse', 'jinja2', 'ply']
)
