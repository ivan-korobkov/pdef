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

    packages=['pdef_compiler', 'pdef_compiler.ast', 'pdef_java', 'pdef_python'],
    package_dir={'': 'src'},
    package_data={
        'pdef_java': ['*.template'],
        'pdef_python': ['*.template']},

    install_requires=['argparse', 'jinja2>=2.7', 'ply>=3.4'],
    entry_points={
        'console_scripts': ['pdef = pdef_compiler:main'],
        'pdef_compiler.generators': [
            'java = pdef_java:generate_source_code',
            'python = pdef_python:generate_source_code'
        ]
    },
)
