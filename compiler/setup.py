# encoding: utf-8
import os.path
try:
    from setuptools import setup
except ImportError:
    import ez_setup
    ez_setup.use_setuptools()
    from setuptools import setup


# Load the version.
__version__ = None
with open(os.path.join('src', 'pdefc', 'version.py')) as f:
    exec(f.read())


setup(
    name='pdef-compiler',
    version=__version__,
    license='Apache License 2.0',
    description='Pdef compiler',
    url='http://github.com/ivan-korobkov/pdef',

    author='Ivan Korobkov',
    author_email='ivan.korobkov@gmail.com',

    packages=['pdefc',
              'pdefc.ast',
              'pdefc.generators',
              'pdefc.generators.java',
              'pdefc.generators.python'],
    package_dir={'': 'src'},
    package_data={
        '': ['*.template']
    },

    install_requires=['argparse', 'jinja2>=2.7', 'ply>=3.4'],
    entry_points={
        'console_scripts': ['pdefc = pdefc:main'],
        'pdefc.generators': [
            'java = pdefc.generators.java:generate',
            'python = pdefc.generators.python:generate'
        ]
    },

    classifiers=[
        'Development Status :: 4 - Beta',
        'Environment :: Console',
        'Intended Audience :: Developers',
        'License :: OSI Approved :: Apache Software License',
        'Operating System :: OS Independent',
        'Programming Language :: Python :: 2.7',
        'Topic :: Software Development :: Code Generators',
        'Topic :: Software Development :: Compilers'
    ]
)
