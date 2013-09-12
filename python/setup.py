# encoding: utf-8
from distutils.core import setup

setup(
    name='pdef',
    version='1.0-alpha9',
    url='http://github.com/ivan-korobkov/pdef',
    description='Protocol definition language',
    license='Apache License 2.0',

    author='Ivan Korobkov',
    author_email='ivan.korobkov@gmail.com',

    package_dir={'': 'src'},
    packages=['pdef'],
    py_modules=['pdef_rpc'],

    requires=['requests']
)
