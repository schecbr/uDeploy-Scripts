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

for (int i = 1588; i <= 2500; i++) {
	def resourceName = sprintf("resource-%03d", i)
	
	for (int j = 1; j <= 10; j++) {
		runCommandAndWait(["./udclient", "--weburl", "http://localhost:8080",
				"setResourceProperty",
				"--resource", resourceName,
				"--name", sprintf("property-%03d", j),
				"--value", UUID.randomUUID().toString(),
				"--isSecure", (j % 5 == 0)], udClientDir)
	}
}