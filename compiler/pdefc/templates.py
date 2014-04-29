# encoding: utf-8
import io
import inspect
import logging
import os

from jinja2 import Environment

ENCODING = 'utf-8'


class Templates(object):
    '''Templates class is a default Jinja templates loader relative to a directory or a file.

    Get a template::
        >>> templates = Templates(__file__)
        >>> templates.get('my_jinja.jinja2')

    Render a template::
        >>> templates = Templates(__file__)
        >>> templates.render('mytemplate.jinja2', key='value')

    '''
    def __init__(self, dir_or_file, filters=None):
        '''Create a templates loader relative to a directory or a file.'''
        if os.path.isdir(dir_or_file):
            self._dir = dir_or_file
        else:
            self._dir = os.path.dirname(dir_or_file)

        self._env = Environment(trim_blocks=True, lstrip_blocks=True)
        self._cache = {}

        self.add_filter('upper_first', upper_first)
        if isinstance(filters, dict):
            self.add_filters(**filters)
        elif filters:
            self.add_filters_from_methods(filters)

    def add_filter(self, name, filter0):
        '''Add a Jinja filter.'''
        self._env.filters[name] = filter0

    def add_filters(self, **name_to_filter):
        '''Add Jinja filters.'''
        for name, filter0 in name_to_filter.items():
            self.add_filter(name, filter0)

    def add_filters_from_methods(self, obj):
        '''Add all public methods as Jinja filters.'''
        for name in dir(obj):
            if name.startswith('_'):
                continue

            attr = getattr(obj, name)
            if inspect.ismethod(attr):
                self.add_filter(name, attr)

    def get(self, filename):
        '''Read and return a Jinja template, the templates are cached.'''
        if filename in self._cache:
            return self._cache[filename]

        # Get the template file.
        path = os.path.join(self._dir, filename)
        with open(path, 'r') as module_file:
            text = module_file.read()

        template = self._env.from_string(text)
        self._cache[filename] = template

        return template

    def render(self, template_name, **kwargs):
        '''Get a template and render it using the keyword arguments.'''
        template = self.get(template_name)
        return template.render(**kwargs)


def upper_first(s):
    '''Uppercase the first letter in a string.'''
    if not s:
        return s
    return s[0].upper() + s[1:]


def mkdir_p(dirname):
    '''Make directories, ignore errors'''
    if os.path.exists(dirname):
        return
    os.makedirs(dirname)


def write_file(dst, filename, text):
    '''Write a text file to the output directory, filename can contain subdirectories.'''
    
    # Join the filename with the destination directory.
    filepath = os.path.join(dst, filename)
    
    # Create a directory with its children for a file.
    dirpath = os.path.dirname(filepath)
    mkdir_p(dirpath)

    # Write the file contents.
    with io.open(filepath, 'wt', encoding=ENCODING) as f:
        f.write(text)
    logging.info('Created %s', filepath)
