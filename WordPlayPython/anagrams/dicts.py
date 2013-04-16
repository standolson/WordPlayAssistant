
DICTIONARY_TWL06 = 0x1
DICTIONARY_TWL98 = 0x2
DICTIONARY_SOWPODS = 0x4
DICTIONARY_COLLINS_FEB07 = 0x8
DICTIONARY_COLLINS_APR07 = 0x10
DICTIONARY_ENABLE = 0x20

DICTS = {
    'twl98' : DICTIONARY_TWL98,
    'twl06' : DICTIONARY_TWL06,
    'cwsfeb07' : DICTIONARY_COLLINS_FEB07,
    'cwsapr07' : DICTIONARY_COLLINS_APR07,
    'sowpods' : DICTIONARY_SOWPODS,
    'enable' : DICTIONARY_ENABLE,
}


def dict_to_string(bitmask):

    ret_str = ""
    for k, v in DICTS.iteritems():
        if (bitmask & v) == v:
            if len(ret_str) == 0:
                ret_str = k
            else:
                ret_str += "|" + k

    return ret_str
