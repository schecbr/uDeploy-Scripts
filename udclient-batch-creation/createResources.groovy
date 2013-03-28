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

for (int i = 1; i <= 2500; i++) {
    runCommandAndWait(["./udclient", "--weburl", "http://localhost:8080",
    		"createResource",
    		"--parentResource", "mjw-mbpr.local",
    		"--name", sprintf("resource-%03d", i)], udClientDir)
}