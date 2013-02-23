# encoding: utf-8
from collections import deque


class Walker(object):
    def __init__(self, root, depth_first=True):
        self.root = root
        self.depth_first = depth_first

    def __iter__(self):
        return _dfs(self.root) if self.depth_first else _bfs(self.root)

    def dfs(self):
        return Walker(self.root, True)

    def bfs(self):
        return Walker(self.root, False)

    def packages(self):
        from pdef import lang
        return self._filter(lang.Package)

    def proxies(self):
        from pdef import lang
        return self._filter(lang.AbstractRef)

    def modules(self):
        from pdef import lang
        return self._filter(lang.Module)

    def module_refs(self):
        from pdef import lang
        return self._filter(lang.ImportRef)

    def refs(self):
        from pdef import lang
        return self._filter(lang.Ref)

    def ptypes(self):
        from pdef import lang
        return self._filter(lang.ParameterizedType)

    def types(self):
        from pdef import lang
        return self._filter(lang.Type)

    def variables(self):
        from pdef import lang
        return self._filter(lang.Variable)

    def natives(self):
        from pdef import lang
        return self._filter(lang.Native)

    def enums(self):
        from pdef import lang
        return self._filter(lang.Enum)

    def messages(self):
        from pdef import lang
        return self._filter(lang.Message)

    def message_polymorphisms(self):
        from pdef import lang
        return self._filter(lang.MessagePolymorphism)

    def fields(self):
        from pdef import lang
        return self._filter(lang.Field)

    def pmessages(self):
        from pdef import lang
        return self._filter(lang.ParameterizedMessage)

    def _filter(self, cls):
        return filter(lambda x: isinstance(x, cls), self)


class _IdentitySet(object):
    def __init__(self):
        self._set = set()

    def add(self, item):
        item_id = id(item)
        self._set.add(item_id)

    def __contains__(self, item):
        item_id = id(item)
        return item_id in self._set


def _dfs(root):
    to_visit = deque()
    to_visit.append(root)
    visited = _IdentitySet()

    while len(to_visit) > 0:
        node = to_visit.popleft()
        if node in visited:
            continue

        visited.add(node)
        yield node

        to_visit.extendleft(node.children)


def _bfs(root):
    to_visit = deque()
    to_visit.append(root)
    visited = _IdentitySet()

    while len(to_visit) > 0:
        node = to_visit.popleft()
        if node in visited:
            continue

        visited.add(node)
        yield node

        to_visit.extend(node.children)
