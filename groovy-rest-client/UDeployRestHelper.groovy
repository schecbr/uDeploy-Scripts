import groovyx.net.http.HTTPBuilder
import org.apache.http.HttpRequestInterceptor
import org.apache.http.HttpRequest
import org.apache.http.protocol.HttpContext
import java.security.KeyStore
import org.apache.http.conn.ssl.SSLSocketFactory
import org.apache.http.conn.scheme.Scheme
import org.apache.http.conn.scheme.PlainSocketFactory
import org.apache.http.client.HttpResponseException
import groovyx.net.http.Method
import groovyx.net.http.ContentType

public class UDeployRestHelper {

    static final def String GET_APPLICATION_INFO = '/cli/application/info'
    static final def String GET_APPLICATION_PROPS = '/cli/application/getProperties'
    static final def String GET_COMPONENT_INFO = '/cli/component/info'
    static final def String SET_SYSTEM_PROP = '/cli/systemConfiguration/propValue'
    static final def String GET_SYSTEM_PROPS = '/cli/systemConfiguration/getProperties'
    static final def String SET_APPLICATION_PROP = '/cli/application/propValue'
    static final def String SET_ENV_PROP = '/cli/environment/propValue'
    static final def String GET_ENV_PROPS = '/cli/environment/getProperties'
    static final def String SET_COMPONENT_PROP = '/cli/component/propValue'
    static final def String GET_COMPONENT_PROPS = '/cli/component/getProperties'
    static final def String CREATE_COMPONENT_ENV_DEF = '/property/propSheetDef'
    static final def String CREATE_COMPONENT = '/cli/component/create'
    static final def String UPDATE_COMPONENT = '/rest/deploy/component'
    static final def String SET_COMPONENT_ENVIRONMENT_PROP = '/cli/componentEnvironmentMapping/propValue'
    static final def String GET_COMPONENT_ENVIRONMENT_PROPS = '/cli/componentEnvironmentMapping/getProperties'
    static final def String SET_RESOURCE_PROP = '/cli/resource/setProperty'
    static final def String GET_RESOURCE_PROP = '/cli/resource/getProperty'
    static final def String GET_RESOURCES = '/cli/resource'
    static final def String GET_COMPONENTS = '/cli/component'
    static final def String GET_COMPONENTS_IN_APPLICATION = '/cli/application/componentsInApplication'
    static final def String GET_ENVS_IN_APPLICATION = '/cli/application/environmentsInApplication'

    static private def http

    private serverUrl
    private user
    private password

    public UDeployRestHelper(serverUrl, user, password) {
        this.serverUrl = serverUrl
        this.user = user
        this.password = password
    }

    private def getBuilder = {
        if (!http) {
            http = new HTTPBuilder(serverUrl)
	    http.auth.basic user, password

            http.client.addRequestInterceptor(new HttpRequestInterceptor() {
                void process(HttpRequest httpRequest, HttpContext httpContext) {
//                    httpRequest.addHeader('Authorization', 'Basic ' + "$user:$password".toString().bytes.encodeBase64().toString())
                    httpRequest.addHeader('User-Agent', 'HTTPClient')
                }
            })


            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);

            SSLSocketFactory sf = new CustomSSLSocketFactory(trustStore);
            sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            http.client.connectionManager.schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            http.client.connectionManager.schemeRegistry.register(new Scheme("https", sf, 443));
        }

        return http
    }

    public def doesApplicationExist = { application ->

        if (application) {
            try {
                getBuilder().get(path: GET_APPLICATION_INFO, query: [application: application]){ resp, json ->
                    println json.name + ':' + json.id
                }
                return true
            }
            catch (HttpResponseException e) {
                if (e.statusCode == 404) {
                    return false
                }
                else {
                    throw new Exception(e.message + ':' + e.statusCode)
                }
            }
        }
        else {
            return false
        }
    }

    public def getComponentId = { component ->

        def result
        if (component) {
            try {
                getBuilder().get(path: GET_COMPONENT_INFO, query: [component: component]){ resp, json ->
                    result = json.id
                }
            }
            catch (HttpResponseException e) {
                if (e.statusCode == 404) {
                    throw new Exception("Cound not find component $component")
                }
                else {
                    throw new Exception(e.message + ':' + e.statusCode)
                }
            }
        }

        return result
    }

    public def setSystemProp = { name, value ->
        if (name) {
            getBuilder().request(Method.PUT) {
                uri.path = SET_SYSTEM_PROP
                uri.query = [name: name, value: value]
                response.failure = { resp ->
                    if (resp.status == 400) {
                        throw new Exception("You do not have permissions to create/update system properties")
                    }
                    else {
                        throw new Exception(resp.statusLine.toString())
                    }
                }
            }
        }
    }

    public def setApplicationProp = { application, propName, propValue ->
        if (application && propName) {
            getBuilder().request(Method.PUT) {
                uri.path = SET_APPLICATION_PROP
                uri.query = [name: propName, value: propValue, application: application]
                response.failure = { resp ->
                    if (resp.status == 404) {
                        throw new Exception("Applicatoin $application not found!")
                    }
                    else if (resp.status == 400) {
                        throw new Exception("You do not have write permissions to $application!")
                    }
                    else {
                        throw new Exception(resp.statusLine.toString())
                    }
                }
            }
        }
    }

    public def setEnvironmentProp = { application, environment, name, value, isSecure ->
        if (application && environment && name) {
            getBuilder().request(Method.PUT) {
                uri.path = SET_ENV_PROP
                uri.query = [name: name, value: value, application: application, environment: environment, isSecure:isSecure]
                response.failure = { resp ->
                    if (resp.status == 404) {
                        throw new Exception("Application $application not found!")
                    }
                    else if (resp.status == 400) {
                        throw new Exception("Cannot write to environment $environment, check name and permissions!")
                    }
                    else {
                        throw new Exception(resp.statusLine.toString())
                    }
                }
            }
        }
    }

    public def setComponentProp = { component, name, value ->
        if (component && name) {
            getBuilder().request(Method.PUT) {
                uri.path = SET_COMPONENT_PROP
                uri.query = [name: name, value: value, component: component]
                response.failure = { resp ->
                    if (resp.status == 404) {
                        throw new Exception("Component $component not found!")
                    }
                    else if (resp.status == 400) {
                        throw new Exception("Cannot write to component $component!")
                    }
                    else {
                        throw new Exception(resp.statusLine.toString())
                    }
                }
            }
        }
    }

    public def setResourceProp = { resource, name, value ->
        if (resource && name) {
            getBuilder().request(Method.PUT) {
                uri.path = SET_RESOURCE_PROP
                uri.query = [name: name, value: value, resource: resource]
                response.failure = { resp ->
                    if (resp.status == 404) {
                        throw new Exception("Resource $resource not found!")
                    }
                    else if (resp.status == 400) {
                        throw new Exception("Cannot write to resource $resource!")
                    }
                    else {
                        throw new Exception(resp.statusLine.toString())
                    }
                }
            }
        }
    }

    public def createComponentEnvDef = { componentId, name ->
        if (componentId && name) {
            getBuilder().request(Method.PUT) {
                requestContentType = ContentType.JSON
                uri = serverUrl + '/' + CREATE_COMPONENT_ENV_DEF + '/components%2f' + componentId + '%2FenvironmentPropSheetDef.-1/propDefs'
                body = [
                        name: name,
                        description:'',
                        label:'',
                        required:'true',
                        type:'TEXT',
                        value:''
                ]
                response.failure = { resp ->
                    if (resp.status == 404) {
                        throw new Exception("Property definition group not found!")
                    }
                    else if (resp.status == 500 ) {
                        throw new Exception("Component environment property definition $name already exists!")
                    }
                    else if (resp.status == 400) {
                        throw new Exception("Cannot write to component $componentId!")
                    }
                    else {
                        throw new Exception(resp.statusLine.toString())
                    }
                }
                
                response.success = { resp, json ->
                    return json
                }
            }
        }
    }

    public def createComponent = { input ->
        if (input != null) {
            getBuilder().request(Method.PUT) {
                requestContentType = ContentType.JSON
                uri.path = CREATE_COMPONENT
                body = input
                response.failure = { resp ->
                    if (resp.status == 500 ) {
                        throw new Exception("Component already exists!")
                    }
                    else if (resp.status == 400) {
                        throw new Exception("Cannot create component!")
                    }
                    else {
                        throw new Exception(resp.statusLine.toString())
                    }
                }

                response.success = { resp, json ->
                    return json
                }
            }
        }
    }

    public def updateComponent = { input ->
        if (input != null) {
            getBuilder().request(Method.PUT) {
                requestContentType = ContentType.JSON
                uri.path = UPDATE_COMPONENT
                body = input
                response.failure = { resp ->
                    if (resp.status == 500 ) {
                        throw new Exception("Component does not exist!")
                    }
                    else if (resp.status == 400) {
                        throw new Exception("Cannot update component!")
                    }
                    else {
                        throw new Exception(resp.statusLine.toString())
                    }
                }

                response.success = { resp, json ->
                    return json
                }
            }
        }
    }

    public def updateComponentEnvDef = { componentId, name ->
        if (componentId && name) {
            getBuilder().request(Method.PUT) {
                requestContentType = ContentType.JSON
                uri = serverUrl + '/' + CREATE_COMPONENT_ENV_DEF + '/components%2f' + componentId + '%2FenvironmentPropSheetDef.-1/propDefs/' + name
                body = [
                        name: name,
                        description:'',
                        label:'',
                        required:'true',
                        type:'TEXT',
                        value:''
                ]
                response.failure = { resp ->
                    if (resp.status == 404) {
                        throw new Exception("Property definition group not found!")
                    }
                    else if (resp.status == 500 ) {
                        throw new Exception("Component environment property definition $name does not exist!")
                    }
                    else if (resp.status == 400) {
                        throw new Exception("Cannot write to component $componentId!")
                    }
                    else {
                        throw new Exception(resp.statusLine.toString())
                    }
                }

                response.success = { resp, json ->
                    println json
                }
            }
        }
    }

    public def getComponentEnvDefSet = { componentId ->
        def result = []
        if (componentId) {
            
            getBuilder().request(Method.GET) {
                uri = serverUrl + '/' + CREATE_COMPONENT_ENV_DEF + '/components%2f' + componentId + '%2FenvironmentPropSheetDef.-1/propDefs'
                response.failure = { resp ->
                    if (resp.status == 404) {
                        throw new Exception("Component $componentId not found!")
                    }
                    else if (resp.status == 400) {
                        throw new Exception("Cannot write to component $componentId!")
                    }
                    else {
                        throw new Exception(resp.statusLine.toString())
                    }
                }

                response.success = { resp, json ->
                    json.each {
                        result << it.name
                    }
                }
            }
        }
        
        return result
    }

    public def setComponentEnvironmentProp = { application, environment, component, name, value ->
        if (application && environment && component && name) {
            getBuilder().request(Method.PUT) {
                uri.path = SET_COMPONENT_ENVIRONMENT_PROP
                uri.query = [name: name, value: value, component: component, environment: environment, application: application]
                response.failure = { resp ->
                    if (resp.status == 404) {
                        throw new Exception("Property $name not found!")
                    }
                    else if (resp.status == 400) {
                        throw new Exception("Application, environment or component not found or not accessible!")
                    }
                    else {
                        throw new Exception(resp.statusLine.toString())
                    }
                }
            }
        }
    }

    public def getAppProperties = { application ->
        def result = [:] as Map
        if (application) {

            getBuilder().request(Method.GET) {
                uri.path = GET_APPLICATION_PROPS
                uri.query = [application: application]
                response.failure = { resp ->
                    if (resp.status == 404) {
                        throw new Exception("Application $application not found!")
                    }
                    else {
                        throw new Exception(resp.statusLine.toString())
                    }
                }

                response.success = { resp, json ->
                    json.each {
                        result[it.name] = it.value
                    }
                }
            }
        }

        return result
    }

    public def getComponentProperties = { component ->
        def result = [:] as Map
        if (component) {

            getBuilder().request(Method.GET) {
                uri.path = GET_COMPONENT_PROPS
                uri.query = [component: component]
                response.failure = { resp ->
                    if (resp.status == 404) {
                        throw new Exception("Component $component not found!")
                    }
                    else {
                        throw new Exception(resp.statusLine.toString())
                    }
                }

                response.success = { resp, json ->
                    json.each {
                        result[it.name] = it.value
                    }
                }
            }
        }

        return result
    }

    public def getEnvProperties = { application, environment->
        def result = [:] as Map
        if (application && environment) {

            getBuilder().request(Method.GET) {
                uri.path = GET_ENV_PROPS
                uri.query = [application: application, environment: environment]
                response.failure = { resp ->
                    if (resp.status == 404) {
                        throw new Exception("Environment $environment not found in application $application!")
                    }
                    else {
                        throw new Exception(resp.statusLine.toString())
                    }
                }

                response.success = { resp, json ->
                    json.each {
                        result[it.name] = it.value
                    }
                }
            }
        }

        return result
    }

    public def getResourceProperty = { resource, propName ->
        def result
        if (resource && propName) {

            getBuilder().request(Method.GET) {
                uri.path = GET_RESOURCE_PROP
                uri.query = [name: propName, resource: resource]
                response.failure = { resp ->
                    if (resp.status == 404) {
                        if (resp.getEntity().getContent().getText().startsWith('Property')) {
                            // no property, return null for value
                        }
                        else {
                            throw new Exception("Resource $resource does not exist!")
                        }
                    }
                    else {
                        throw new Exception(resp.statusLine.toString())
                    }
                }

                response.success = { resp ->
                    result = resp.getEntity().getContent().getText().trim()
                }
            }
        }

        return result
    }

    public def getComponentEnvProperties = { application, environment, component ->
        def result = [:] as Map
        if (application && environment && component) {

            getBuilder().request(Method.GET) {
                uri.path = GET_COMPONENT_ENVIRONMENT_PROPS
                uri.query = [application: application, environment: environment, component: component]
                response.failure = { resp ->
                    if (resp.status == 400) {
                        throw new Exception("Application, environment or component not found or not accessible!")
                    }
                    else {
                        throw new Exception(resp.statusLine.toString())
                    }
                }

                response.success = { resp, json ->
                    json.each {
                        result[it.name] = it.value
                    }
                }
            }
        }

        return result
    }

    public def getSystemProperties = {
        def result = [:] as Map
        getBuilder().request(Method.GET) {
            uri.path = GET_SYSTEM_PROPS
            response.failure = { resp ->
                if (resp.status == 400) {
                    throw new Exception("You do not have permissions to view system properties")
                }
                else {
                    throw new Exception(resp.statusLine.toString())
                }
            }

            response.success = { resp, json ->
                json.each {
                    result[it.name] = it.value
                }
            }
        }

        return result
    }

    public def getResourceList = {
        def result = [] as Set
        getBuilder().request(Method.GET) {
            uri.path = GET_RESOURCES
            response.failure = { resp ->
                if (resp.status == 400) {
                    throw new Exception("You do not have permissions to view system properties")
                }
                else {
                    throw new Exception(resp.statusLine.toString())
                }
            }

            response.success = { resp, json ->
                json.each {
                    result << it.name
                }
            }
        }

        return result
    }

    public def getComponentList = {
        def result = [:] as Map
        getBuilder().request(Method.GET) {
            uri.path = GET_COMPONENTS
            response.failure = { resp ->
                if (resp.status == 400) {
                    throw new Exception("You do not have permissions to view components")
                }
                else {
                    throw new Exception(resp.statusLine.toString())
                }
            }

            response.success = { resp, json ->
                json.each {
                    result[it.name] = it.id
                }
            }
        }

        return result
    }

    public def getComponentInApplicationList = { application ->
        def result = [:] as Map
        getBuilder().request(Method.GET) {
            uri.path = GET_COMPONENTS_IN_APPLICATION
            uri.query = [application: application]
            response.failure = { resp ->
                if (resp.status == 400) {
                    throw new Exception("You do not have permissions to application $application")
                }
                else {
                    throw new Exception(resp.statusLine.toString())
                }
            }

            response.success = { resp, json ->
                json.each {
                    result[it.name] = it.id
                }
            }
        }

        return result
    }

    public def getEnvironmentInApplicationList = { application ->
        def result = [:] as Map
        getBuilder().request(Method.GET) {
            uri.path = GET_ENVS_IN_APPLICATION
            uri.query = [application: application]
            response.failure = { resp ->
                if (resp.status == 400) {
                    throw new Exception("You do not have permissions to application $application")
                }
                else {
                    throw new Exception(resp.statusLine.toString())
                }
            }

            response.success = { resp, json ->
                json.each {
                    result[it.name] = it.id
                }
            }
        }

        return result
    }
}