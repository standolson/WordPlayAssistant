# -*- coding: utf-8 -*-
#
# Copyright (C) 2010  Portland Portable Programs, LLC
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.

from __future__ import generators
from google.appengine.ext import webapp
from django.utils import simplejson
#import models
from primes import PRIMES
from letters import LETTER_VALUES
from dicts import *
from operator import mul, add
import os
import sys
import re

MAX_RESULTS = 1000
MIN_WORD_LENGTH = 2
MAX_WORD_LENGTH = 20
MAX_WILDCARDS = 1 # TODO: No wildcard support yet!


def powerset(s):
  sets = []
  indicator = lambda x: x & 1
  for element in xrange(2**len(s)):
    n = element
    subset = []
    for x in s:
        if indicator(n):
            subset.append(x)
        n >>= 1

    sets.append(''.join(subset))

  return sets


ANAGRAMS = {} # { prime_key : [word, word, ...], ... }
WORDS = {}    # { 'word' : dicts_collection, ... }

f = open('anagrams/combined.wl', 'r')
for l in f:
    ll = l.lower().strip()
    word, dicts = ll.split(',')
    try:
        dicts = int(dicts)
    except (TypeError, ValueError):
        dicts = DICTIONARY_TWL06

    v = reduce(mul, [PRIMES[w] for w in word])
    WORDS[word] = dicts #.append(word)

    try:
        ANAGRAMS[v].append(word)
    except KeyError:
        ANAGRAMS[v] = [word]




class AnagramHandler(webapp.RequestHandler):

    def get(self):
        action = self.request.get('action', 'lookup').lower()
        dictionary = self.request.get('dict', 'sowpods').lower()

        if dictionary not in DICTS:
            self.error(400)
            self.response.out.write("Invalid dictionary: Must be one of %s." % ', '.join(DICTS.keys()))
            return

        dict_value = DICTS[dictionary]

        #
        # LOOKUP
        #

        if action == 'lookup':
            # Does the specified word exist in the TWL?
            word = self.request.get('word', '').lower()
            if not word or len(word) < MIN_WORD_LENGTH or len(word) > MAX_WORD_LENGTH or not word.isalpha():
                self.error(400)
                self.response.out.write("Invalid word: Must be [A-Za-z] and %d <= length <= %d." % (MIN_WORD_LENGTH, MAX_WORD_LENGTH))
                return

            self.response.headers['Content-Type'] = 'application/json'
            self.response.out.write(simplejson.dumps(word in WORDS and WORDS[word] & dict_value == dict_value))

        #
        # ANAGRAM
        #

        elif action == 'anagram':
            # Return a (full) list of anagrams based on the list of letters provided
            # . and ? can be used for wildcards
            letters = self.request.get('letters', '').replace('?', '.').lower()
            word = self.request.get('word', '').replace('?', '.').lower() # Alt.
            fmt = self.request.get('fmt', 'std').lower()

            if not letters and word:
                letters = word

            if fmt not in ('std', 'scored'):
                self.error(400)
                self.response.out.write("Invalid fmt: Must be 'std' or 'scored'.")
                return

            if not letters or len(letters) > MAX_WORD_LENGTH or len(letters) < MIN_WORD_LENGTH or letters.count('.') > MAX_WILDCARDS:
                self.error(400)
                self.response.out.write("Invalid letters: Must be [A-Za-z], %d <= length <= %d, and no more than %d wildcards [.?]." % (MIN_WORD_LENGTH, MAX_WORD_LENGTH, MAX_WILDCARDS))
                return

            result = []

            # TODO: This only handles n = 1 wildcard... generalize the solution for n > 1
            if '.' in letters:
                for wc in 'abcdefghijklmnopqrstuvwxyz':
                    for w in powerset(letters+wc):
                        try:
                            result.extend(ANAGRAMS[reduce(mul, [PRIMES[l] for l in w])])
                        except (KeyError, TypeError):
                            continue

            else:
                for w in powerset(letters):
                    try:
                        result.extend(ANAGRAMS[reduce(mul, [PRIMES[l] for l in w])])
                    except (KeyError, TypeError):
                        continue

            result2 = []
            result = set(result)
            for r in result:
                if WORDS[r] & dict_value == dict_value:
                    result2.append(r)

            result2.sort()

            self.response.headers['Content-Type'] = 'application/json'
            #result = sorted(set(result))

            if fmt == 'std':
                self.response.out.write(simplejson.dumps(result2))

            else: # scored
                d = {}
                for r in result2:
                    d[r] = reduce(add, [LETTER_VALUES[l] for l in r])

                self.response.out.write(simplejson.dumps(d))

        #
        # SEARCH
        #

        elif action == 'search':
            s = self.request.get('str', '').lower()
            word = self.request.get('word', '').lower() # Alt.
            fmt = self.request.get('fmt', 'std').lower()

            if fmt not in ('std', 'scored'):
                self.error(400)
                self.response.out.write("Invalid fmt: Must be 'std' or 'scored'.")
                return

            if not s and word:
                s = word

            if not s:
                self.error(400)
                self.response.out.write("Invalid search string.")
                return

            try:
                regex = re.compile(s, re.I)
            except re.error, e:
                self.error(400)
                self.response.out.write("Invalid regex: %s" % e)
                return

            result = []
            for w in WORDS:
                m = regex.search(w)
                if m is not None and WORDS[w] & dict_value == dict_value:
                    result.append(w)

                    if len(result) > MAX_RESULTS:
                        break

            self.response.headers['Content-Type'] = 'application/json'
            result = sorted(set(result))

            if fmt == 'std':
                self.response.out.write(simplejson.dumps(result))

            else: # scored
                d = {}
                for r in result:
                    d[r] = reduce(add, [LETTER_VALUES[l] for l in r])

                self.response.out.write(simplejson.dumps(d))

        else:
            self.error(400)
            self.response.out.write("Invalid action.")


# POST /judge
class JudgeHandler(webapp.RequestHandler):
    def get(self):
        self.error(400)
        self.response.out.write("Judge requires POST.")


    def post(self):
        dictionary = self.request.get('dict', 'sowpods').lower()

        if dictionary not in DICTS:
            self.error(400)
            self.response.out.write("Invalid dictionary: Must be one of %s." % ', '.join(DICTS.keys()))
            return

        dict_value = DICTS[dictionary]

        try:
            words = simplejson.loads(self.request.body)
        except ValueError:
            self.error(400)
            self.response.out.write("Invalid POST data.")
            return

        self.response.headers['Content-Type'] = 'application/json'
        for w in words:
            ww = w.lower()
            if not (ww in WORDS and WORDS[ww] & dict_value == dict_value):
                self.response.out.write(simplejson.dumps(False))
                return

        self.response.out.write(simplejson.dumps(True))

