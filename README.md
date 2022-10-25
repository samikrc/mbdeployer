This project helps to extend an website created by Mobirise by adding additional code to specific blocks in each HTML file.

## Usage

**Step 1**: Publish using Mobirise to a folder.

**Step 2**: Setup the JSON files for code enhancement.

**Step 3**: Run the following:

`
scala mbdeployer-<version>.jar [/path/to/source/folder] [/path/to/destination/folder]
`

The source folder is typically where the Mobirise project has been published. The `/path/to/source/folder` and `/path/to/destination/folder` can be omitted by specifying them in the `application.conf` file and recompiling.


## Code enhancement using JSON files
The code enhancements are encoded in the json file as a list of modifications that are to be done. The type of modifications supported are:

* Insert a code block at a particular line
* Delete one or more line(s)
* Replace one or more line(s) with one or more line(s)

The above two modifications performed in specific order can be used for replacement of lines as well. The modifications are applied in the same order as they are mentioned in the code.

### Type of enhancements
Two type of enhancements are supported:

* **Global enhancements:** Enhancements that will be applied to all _html_ files. These are encoded in a file named `global.json`, which has to be in the root folder of the website.
* **File specific enhancements:** These are enhancements specific to a file. If the enhancements are to be applied to `xxx.html`, then these are specified in a file named `xxx.json`. These files must exist in the same folder as the corresponding html file.

### Format of JSON file
The modifications are specified in the JSON file using a few specific fields. Below is a sample `global.json` file.

```$xslt
{
    "items" : 
    [
        {
            "desc": "Modal login dialog",
            "line": 43,
            "action": "insert",
            "code": [
                "<div class=\"modal fade\" id=\"myLoginForm\" tabindex=\"-1\" role=\"dialog\" aria-hidden=\"true\">",
                "    <div class=\"modal-dialog modal-lg\">",
                "        <div class=\"modal-content\">",
                "            <div class=\"modal-header\">",
                "                <button type=\"button\" class=\"close\" data-dismiss=\"modal\" aria-hidden=\"true\">×</button>",
                "                <h4 class=\"modal-title\">Login</h4>",
                "            </div>",
                "            <div class=\"modal-body\">",
                "                <div class=\"row\">",
                "                    <div class=\"col-xs-12 col-lg-10\" style=\"margin-left: 25%;\">                    ",
                "                        <input type=\"text\" name=\"email\" id=\"form1-1m-email\" value=\"\" style=\"display: none;\" />",
                "                        <div class=\"row row-sm-offset\" style=\"width: 40%; min-width: 200px; max-width: 400px;\">",
                "                            <div class=\"col-xs-12 col-md-12\">",
                "                                <div class=\"form-group\">",
                "                                    <label class=\"form-control-label\" for=\"form1-1m-uname\">Email<span class=\"form-asterisk\">*</span></label>",
                "                                    <input type=\"text\" class=\"form-control\" name=\"userName\" required=\"\" id=\"form1-1m-uname\">",
                "                                </div>",
                "                            </div>",
                "                        </div>",
                "                        <div class=\"row row-sm-offset\" style=\"width: 40%; min-width: 200px; max-width: 400px;\">",
                "                            <div class=\"col-xs-12 col-md-12\">",
                "                                <div class=\"form-group\">",
                "                                    <label class=\"form-control-label\" for=\"form1-1m-pass\">Password<span class=\"form-asterisk\">*</span></label>",
                "                                    <input type=\"password\" class=\"form-control\" name=\"password\" required=\"\" id=\"form1-1m-pass\">",
                "                                </div>",
                "                            </div>",
                "                        </div>",
                "                        <div>",
                "                            <button class=\"btn btn-primary\" onclick=\"submitForm()\">LOGIN</button>&nbsp;&nbsp;&nbsp;",
                "                            <button class=\"btn btn-primary\" onclick=\"alert('Please use the contact form to contact us!!')\">FORGOT PASSWORD</button>",
                "                        </div>",
                "                    </div>",
                "                </div>",
                "            </div>",
                "        </div>",
                "    </div>",
                "</div>"
            ]
        }
    ]
}
```  
Since JSON format doesn't allow multi-line strings, we are using an array construct, which will be joined in the code.


### Developing the code snippets

### Usage notes
The line numbers have to be specified carefully. Consecutive application of modifications mentioned in the code can result in shifting lines.
