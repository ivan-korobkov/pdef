# encoding: utf-8
try:
    from setuptools import setup
except ImportError:
    import ez_setup
    ez_setup.use_setuptools()
    from setuptools import setup


setup(
    name='pdef-compiler',
    version='1.0-dev',
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
)
