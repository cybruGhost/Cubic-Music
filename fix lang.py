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
                    string.tail = '\n'  
                root_elem.text = '\n'  
                tree.write(file_path, encoding="utf-8", xml_declaration=True)
                print(f"File fixed : {file_path}")

if __name__ == '__main__':
    fix_quot_and_header()