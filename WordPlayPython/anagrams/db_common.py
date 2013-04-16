# StdLib
import os
import sys
import sqlite3

# Common primary key row
PRIMARY_KEY_ROW = "_id"


def open_database(filename):

    # Open the sqlite database
    db = sqlite3.connect(filename)
    if db is None:
        error("unable to open connection to database '%s'" % filename)
        return db

    return db


def create_table(db, table_name, column_info):

    # Create the SQL statement that creates the table
    create_str = "create table " + table_name + " ("
    for i in range(len(column_info)):
        tuple = column_info[i]
        create_str += tuple[0] + " " + tuple[1]
        if (len(column_info) > 1) and (i != len(column_info) - 1):
            create_str += ", "
    create_str += ");"

    # Create the table
    try:
        db.execute(create_str)
    except sqlite3.OperationalError:
        pass

    db.commit()


def create_insert_statement(table_name, column_info, values=None):

    # Create the string that will insert the data
    insert_str = "insert into " + table_name + "("
    values_str = ""
    for i in range(len(column_info)):
        tuple = column_info[i]
        if (tuple[0] == '_id'):
            continue
        insert_str += tuple[0]
        if values is None:
            values_str += "?"
        else:
            value = values[i - 1]
            if type(value) == str:
                values_str += "\"" + value + "\""
            else:
                values_str += str(value)
        if (len(column_info) > 1) and (i != len(column_info) - 1):
            insert_str += ", "
            values_str += ","
    insert_str += ") values (" + values_str + ");"

    return insert_str


def create_index(db, table_name, column_name):

    # Create the statement that will create the index
    create_str = "create index " + column_name + " on "
    create_str += table_name + " (" + column_name + ")"

    # Create the index
    try:
        db.execute(create_str)
    except sqlite3.OperationalError:
        pass
