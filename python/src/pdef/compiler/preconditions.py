# encoding: utf-8
from pdef.compiler.exc import PdefCompilerException


def check_state(expr, msg=None, *args):
    '''Checks the expression or raises PdefException.'''
    if expr:
        return True

    msg = msg % args if msg else 'Illegal state exception'
    raise PdefCompilerException(msg)


def check_isinstance(argument, class_or_type_or_typle, msg=None, *args):
    '''Checks argument type or raises TypeException.'''
    if isinstance(argument, class_or_type_or_typle):
        return argument

    msg = msg % args if msg else \
            'Wrong type, must be %s, got %s' % (class_or_type_or_typle, argument)
    raise TypeError(msg)
