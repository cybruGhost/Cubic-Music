#!/bin/python

import os
import xml.etree.ElementTree as ET
from xml.dom import minidom

res_path: str = "composeApp/src/androidMain/res"


def fix_quot_and_header():
    for root, dirs, files in os.walk(res_path):
        if "values" in os.path.basename(root):
            file_path = os.path.join(root, "strings.xml")
            if os.path.exists(file_path):
                tree = ET.parse(file_path)
                root_elem = tree.getroot()
                for string in root_elem.findall("string"):
                    if string.text:
                        string.text = string.text.replace('&quot;', '"')

                rough_string = ET.tostring(root_elem, encoding="utf-8")
                reparsed = minidom.parseString(b'<?xml version="1.0" encoding="utf-8"?>' + rough_string)
                pretty_xml = reparsed.toprettyxml(indent="    ", encoding="utf-8")
                with open(file_path, "wb") as f:
                    f.write(pretty_xml)
                print(f"File fixed : {file_path}")

if __name__ == '__main__':
    fix_quot_and_header()