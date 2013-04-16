#!/usr/bin/env python
# -*- coding: utf-8; mode: python; tab-width: 4  -*-
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

DICTIONARY_TWL06 = 0x1
DICTIONARY_TWL98 = 0x2
DICTIONARY_SOWPODS = 0x4
DICTIONARY_COLLINS_FEB07 = 0x8
DICTIONARY_COLLINS_APR07 = 0x10


from google.appengine.ext import db
from google.appengine.tools import bulkloader


class Dictionary(db.Model):
    word = db.StringProperty()
    dicts = db.IntegerProperty()
    # std_word_value - compute on the fly



class Anagrams(db.Model):
    value = db.IntegerProperty()
    words = db.StringListProperty()
    # word list can contain words from all avail dicts -
    # must do a lookup to see if the word applies to active dictionary

