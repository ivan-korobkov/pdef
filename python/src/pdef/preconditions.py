# encoding: utf-8


def check_not_none(argument, msg=None, *args):
    if argument:
        return argument

    if msg:
        msg = msg % args
    else:
        msg = "Cannot be none"

    raise ValueError(msg)


def check_isinstance(argument, class_or_type_or_typle, msg=None, *args):
    if not isinstance(argument, class_or_type_or_typle):
        if msg:
            msg = msg % args
        else:
            msg = 'Wrong type, must be %s, got %s' % (class_or_type_or_typle, argument)

        raise TypeError(msg)

    return argument


def check_argument(expr, msg=None, *args):
    if expr:
       return True

    if msg:
        msg = msg % args
    else:
        msg = 'Wrong argument'

    raise ValueError(msg)


def check_state(expr, msg=None, *args):
    if expr:
        return True

    if msg:
        msg = msg % args
    else:
        msg = 'Illegal state exception'

    raise IllegalStateException(msg)


class IllegalStateException(ValueError):
    pass
