# StdLib
import os
import sys

# Local
from dicts import *
from primes import PRIMES, ZIM_PRIMES, CORNELL_PRIMES, DICT_PRIMES
from letters import LETTER_VALUES

# The wordlist
WORDLIST_FILE="combined.wl"
NEW_DICTIONARY_FILE="enable.wl"
NEW_WORDLIST_FILE="new_combined.wl"
NEW_DICTMASK = DICTIONARY_ENABLE

# Open the current wordlist file
f = open(WORDLIST_FILE, 'r')

# Process each word
wordlist={}
for l in f:

    # Get the word and its dictionary bitmask
    ll = l.lower().strip()
    word, dicts = ll.split(',')
    try:
        dicts = int(dicts)
    except (TypeError, ValueError):
        pass

    wordlist[word] = dicts

# Close the wordlist file
f.close()

# Open the new dictionary
f = open(NEW_DICTIONARY_FILE, 'r')

# Get the keys for quick updates
wordlist_keys = wordlist.keys()

# Process each word in the file
new_wordcount = 0
for l in f:

    # Get the word
    word = l.lower().strip()

    # Update the dictionary entry
    if word in wordlist_keys:
        wordlist[word] |= NEW_DICTMASK
    else:
        wordlist[word] = NEW_DICTMASK
        print "added new word " + word
        new_wordcount += 1

# Close the new dictionary
f.close()

# Report how many new words we found
print "found " + str(new_wordcount) + " words"

# Open the new wordlist
f = open(NEW_WORDLIST_FILE, 'w')

# Write each word in the dictionary, sorted by key
for word in sorted(wordlist.iterkeys()):
#   print word + "," + str(wordlist[word])
    f.write(word + "," + str(wordlist[word]) + "\n")

# Close
f.close()
