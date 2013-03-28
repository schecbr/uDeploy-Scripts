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

for (int i = 818; i <= 1000; i++) {
    def groupName = "group-"+sprintf("%03d", i);
    
    runCommandAndWait(["./udclient", "--weburl", "http://localhost:8080",
    		"createGroup",
    		"--authorizationRealm", "Internal Security",
    		"--group", groupName], udClientDir)
    
    for (int j = 0; j < 10; j++) {
    	def userName = "user-"+sprintf("%03d", new Random().nextInt(500)+1)
    	
	    runCommandAndWait(["./udclient", "--weburl", "http://localhost:8080",
	    		"addUserToGroup",
	    		"--user", userName,
	    		"--group", groupName], udClientDir)
    }
}