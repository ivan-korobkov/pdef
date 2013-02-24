# encoding: utf-8
import logging


class _Errors(object):
    def __init__(self):
        super(_Errors, self).__init__()
        self.errors = []

    def add(self, src, msg, *args):
        error = self._format(src, msg, *args)
        self.errors.append(error)
        logging.error(error)

    def _format(self, src, msg, *args):
        msg = msg % args
        return '%s: %s' % (src.fullname, msg)

_INSTANCE = None


def init():
    global _INSTANCE
    _INSTANCE = _Errors()


def add(src, msg, *args):
    _INSTANCE.add(src, msg, *args)


def aslist():
    return list(_INSTANCE.errors)


def present():
    return bool(_INSTANCE.errors)


init()
