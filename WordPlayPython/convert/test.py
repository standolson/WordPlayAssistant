# Pyth
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

import sys
from unitdata import UnitData
from unitgroup import UnitGroup


data = UnitData()
data.readData()
fromText = "acres/s^2"
fromUnit = UnitGroup(data)
fromUnit.update(fromText)
toText = "ft^2/ms^2"
toUnit = UnitGroup(data)
toUnit.update(toText)
fromUnit.reduceGroup()
toUnit.reduceGroup()

if not fromUnit.categoryMatch(toUnit):
    print 'NO MATCH'
else:
    num = float("45")
    #print u'%f   IS  %f' % (num, fromUnit.convert(num, toUnit))
    print num, fromUnit.unitString(), '-->', fromUnit.convert(num, toUnit), toUnit.unitString()
