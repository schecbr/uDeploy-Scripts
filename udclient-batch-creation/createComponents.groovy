import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

def udClientDir = new File("/Users/mjw/Desktop/udclient")

def runCommandAndWait = { cmd, file ->
    def process = cmd.execute(null, file)
    process.consumeProcessOutput(System.out, System.err)
    process.waitFor()
    if (process.exitValue() != 0) {
        println "Running command ${cmd}"
        throw new Exception("Command failed with exit value ${process.exitValue()}")
    }
}

for (int i = 1; i <= 100; i++) {
    def jsonFile = File.createTempFile("jsonContent", ".txt");

    try {
        def jsonContent = new JSONObject();
        jsonContent.put("name", "component-"+sprintf("%03d", i));
        jsonContent.put("templateName", "template-001");
    
        jsonFile.write(jsonContent.toString())
    
        runCommandAndWait(["./udclient", "--weburl", "http://localhost:8080", "--verbose", "createComponent", jsonFile.absolutePath], udClientDir)
    }
    finally {
        jsonFile.delete()
    }
}