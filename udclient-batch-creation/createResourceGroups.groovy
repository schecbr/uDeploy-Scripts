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


for (int i = 33; i <= 50; i++) {
	def firstGroupName = sprintf("resgroup-%03d", i)
	
    runCommandAndWait(["./udclient", "--weburl", "http://localhost:8080",
    		"createResourceGroup",
    		"--path", "/",
    		"--name", firstGroupName], udClientDir)

	for (int j = 1; j <= 10; j++) {
		def secondGroupName = sprintf("resgroup-%03d-%02d", i, j)
		runCommandAndWait(["./udclient", "--weburl", "http://localhost:8080",
				"createResourceGroup",
				"--path", "/"+firstGroupName,
				"--name", secondGroupName], udClientDir)

		for (int k = 1; k <= 3; k++) {
			def thirdGroupName = sprintf("resgroup-%03d-%02d-%d", i, j, k)
			runCommandAndWait(["./udclient", "--weburl", "http://localhost:8080",
					"createResourceGroup",
					"--path", "/"+firstGroupName+"/"+secondGroupName,
					"--name", thirdGroupName], udClientDir)
					
			for (int l = 1; l <= 10; l++) {
				def resourceName = "resource-"+sprintf("%03d", new Random().nextInt(2500)+1)

				runCommandAndWait(["./udclient", "--weburl", "http://localhost:8080",
						"addResourceToGroup",
						"--group", "/"+firstGroupName+"/"+secondGroupName+"/"+thirdGroupName,
						"--resource", resourceName], udClientDir)
			}
		}
	}
}
