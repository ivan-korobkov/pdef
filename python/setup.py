# encoding: utf-8
try:
    from setuptools import setup
except ImportError:
    import ez_setup
    ez_setup.use_setuptools()
    from setuptools import setup


setup(
    name='pdef',
    version='1.0-dev',
    license='Apache License 2.0',
    description='Protocol definition language',
    url='http://github.com/ivan-korobkov/pdef',

    author='Ivan Korobkov',
    author_email='ivan.korobkov@gmail.com',

    packages=['pdef'],
    package_dir={'': 'src'},
    py_modules=['pdef.rpc'],

    install_requires=['requests>=1.2']
)
