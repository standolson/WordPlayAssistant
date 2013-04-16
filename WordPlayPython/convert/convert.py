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

from google.appengine.ext import webapp
from django.utils import simplejson
from unitdata import UnitData
from unitgroup import UnitGroup

class ConvertError(Exception):
    pass


data = UnitData()
data.readData()


def reduceUnit(unitText):
    unit = UnitGroup(data)
    unit.update(unitText)
    unit.reduceGroup()
    return unit.unitString(unit.reducedList)


def listUnits():
    return sorted(data.keys())


def convertUnits(value, fromUnitText, toUnitText):
    fromUnit = UnitGroup(data)
    fromUnit.update(fromUnitText)
    fromUnit.reduceGroup()

    toUnit = UnitGroup(data)
    toUnit.update(toUnitText)
    toUnit.reduceGroup()


    if not fromUnit.categoryMatch(toUnit):
        raise ConvertError("Catergory match error")
    else:
        return fromUnit.convert(value, toUnit)





class ConvertHandler(webapp.RequestHandler):

  # TODO: Add JSON, txt, and XML output

  def get(self):
    action = self.request.get('action', 'convert').lower()

    if action == 'convert':
        value = self.request.get('value')
        if not value:
            self.error(400)
            self.response.out.write("No value provided.")
            return

        value = float(value)

        fromUnit = self.request.get('from')
        if not fromUnit:
            self.error(400)
            self.response.out.write("No from unit provided.")
            return

        toUnit = self.request.get('to')
        if not toUnit:
            self.error(400)
            self.response.out.write("No to unit provided.")
            return

        try:
            self.response.headers['Content-Type'] = 'application/json'
            self.response.out.write(simplejson.dumps(convertUnits(value, fromUnit, toUnit)))
        except ConvertError:
            self.error(400)
            self.response.out.write("Incompatible units.")
            return

    elif action == 'reduce':
        unit = self.request.get('unit')
        if not unit:
            self.error(400)
            self.response.out.write("No unit provided.")
            return

        self.response.headers['Content-Type'] = 'application/json'
        self.response.out.write(simplejson.dumps(reduceUnit(unit)))

    elif action == 'list':
        sep = self.request.get('sep', 'lf')
        if sep == 'lf':
            sep = '\n'
        elif sep == 'cr':
            sep = '\r'
        elif sep == 'crlf':
            sep = '\r\n'

        self.response.headers['Content-Type'] = 'application/json'
        self.response.out.write(simplejson.dumps(sep.join(listUnits())))

    else:
        self.error(400)
        self.response.out.write("Invalid action.")





if __name__ == '__main__':
    print data.keys()
    print reduceUnit("ft*ft/s*s")
    print convertUnits(45, "acres", "ft^2")
