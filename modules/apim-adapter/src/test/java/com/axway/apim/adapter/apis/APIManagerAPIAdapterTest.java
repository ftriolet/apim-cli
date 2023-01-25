package com.axway.apim.adapter.apis;

import com.axway.apim.WiremockWrapper;
import com.axway.apim.adapter.APIManagerAdapter;
import com.axway.apim.api.API;
import com.axway.apim.api.apiSpecification.APISpecification;
import com.axway.apim.api.apiSpecification.APISpecificationFactory;
import com.axway.apim.api.model.Organization;
import com.axway.apim.lib.CoreParameters;
import com.axway.apim.lib.errorHandling.AppException;
import com.axway.apim.lib.utils.Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class APIManagerAPIAdapterTest extends WiremockWrapper {

    APIManagerAPIAdapter adapter;

    @BeforeClass
    public void initWiremock() {
        super.initWiremock();
    }

    @AfterClass
    public void close() {
        super.close();
    }


    @BeforeClass
    public void setup() throws AppException {
        adapter = new APIManagerAPIAdapter();
    }

    @Test
    public void testUniqueAPIWithAPIPath() throws AppException {
        API noVHostAPI = createTestAPI("/api/v1/resource", null, null);
        API vhostAPI = createTestAPI("/api/v1/other", "api.customer.com", null);
        API anotherVHostAPI = createTestAPI("/api/v1/other", "otherapi.customer.com", null);
        List<API> testAPIs = new ArrayList<>();
        testAPIs.add(noVHostAPI);
        testAPIs.add(vhostAPI);
        testAPIs.add(anotherVHostAPI);

        APIFilter filter = new APIFilter.Builder().hasApiPath("/api/v1/resource").build();

        // Must return the default API (testAPI1) as we don't filter specifically for the V-Host
        API uniqueAPI = adapter.getUniqueAPI(testAPIs, filter);
        Assert.assertEquals(uniqueAPI, noVHostAPI);
    }

    @Test
    public void testUniqueAPISamePathDiffQueryString() throws AppException {
        API queryVersionAPI = createTestAPI("/api/v1/resource", null, "1.0");
        API noQueryVersionAPI = createTestAPI("/api/v1/resource", null, null);
        API anotherQueryVersionAPI = createTestAPI("/api/v1/resource", null, "2.0");
        API nextTextAPI = createTestAPI("/api/v1/resource", "api.customer.com", "3.0");
        List<API> testAPIs = new ArrayList<>();
        testAPIs.add(queryVersionAPI);
        testAPIs.add(noQueryVersionAPI);
        testAPIs.add(anotherQueryVersionAPI);
        testAPIs.add(nextTextAPI);

        APIFilter filter = new APIFilter.Builder().hasApiPath("/api/v1/resource").build();

        // Must return testAPI2 as the default, because no queryStringVersion is given in the filter
        API uniqueAPI = adapter.getUniqueAPI(testAPIs, filter);
        Assert.assertEquals(uniqueAPI, noQueryVersionAPI);
    }

    @Test
    public void testUniqueAPIVHostDefault() throws AppException {
        API queryVersionAPI = createTestAPI("/api/v1/resource", "api.customer.com", "1.0");
        API noQueryVersionAPI = createTestAPI("/api/v1/resource", "api.customer.com", null);
        API anotherQueryVersionAPI = createTestAPI("/api/v1/resource", null, "2.0");
        List<API> testAPIs = new ArrayList<>();
        testAPIs.add(queryVersionAPI);
        testAPIs.add(noQueryVersionAPI);
        testAPIs.add(anotherQueryVersionAPI);

        APIFilter filter = new APIFilter.Builder().hasApiPath("/api/v1/resource").hasVHost("api.customer.com").build();

        // Must return testAPI2 which is the default for the requested VHost as this API has not QueryVersion
        API uniqueAPI = adapter.getUniqueAPI(testAPIs, filter);
        Assert.assertEquals(uniqueAPI, noQueryVersionAPI);
    }

    @Test(expectedExceptions = AppException.class, expectedExceptionsMessageRegExp = "No unique API found.*")
    public void testNoUniqueFoundWithQueryVersion() throws AppException {
        API testAPI1 = createTestAPI("/api/v1/resource", null, "1.0");
        API testAPI2 = createTestAPI("/api/v1/resource", null, "1.0");
        List<API> testAPIs = new ArrayList<>();
        testAPIs.add(testAPI1);
        testAPIs.add(testAPI2);

        APIFilter filter = new APIFilter.Builder().hasApiPath("/api/v1/resource").hasQueryStringVersion("1.0").build();

        // Must fail (throw an Exception) as the API is really not unique, even if we filter with the QueryVersion
        API uniqueAPI = adapter.getUniqueAPI(testAPIs, filter);
        Assert.assertEquals(uniqueAPI, testAPI2);
    }

    @Test(expectedExceptions = AppException.class, expectedExceptionsMessageRegExp = "No unique API found.*")
    public void testNoUniqueFoundWithQueryVersionAndVHost() throws AppException {
        API testAPI1 = createTestAPI("/api/v1/resource", "host.customer.com", "1.0");
        API testAPI2 = createTestAPI("/api/v1/resource", "host.customer.com", "1.0");
        List<API> testAPIs = new ArrayList<>();
        testAPIs.add(testAPI1);
        testAPIs.add(testAPI2);

        APIFilter filter = new APIFilter.Builder().hasApiPath("/api/v1/resource").hasQueryStringVersion("1.0").hasVHost("host.customer.com").build();

        // Must fail (throw an Exception) as the API is really not unique, even if we filter with the QueryVersion and VHost
        API uniqueAPI = adapter.getUniqueAPI(testAPIs, filter);
        Assert.assertEquals(uniqueAPI, testAPI2);
    }

    @Test(expectedExceptions = AppException.class, expectedExceptionsMessageRegExp = "No unique API found.*")
    public void testNoUniqueFoundMixedVHost() throws AppException {
        API testAPI1 = createTestAPI("/api/v1/resource", "host.customer.com", "1.0");
        API testAPI2 = createTestAPI("/api/v1/resource", "other.customer.com", "1.0");
        List<API> testAPIs = new ArrayList<>();
        testAPIs.add(testAPI1);
        testAPIs.add(testAPI2);

        APIFilter filter = new APIFilter.Builder().hasApiPath("/api/v1/resource").hasQueryStringVersion("1.0").build();

        // Must fail (throw an Exception) as the API is really not unique, if we filter with the QueryVersion only
        API uniqueAPI = adapter.getUniqueAPI(testAPIs, filter);
        Assert.assertEquals(uniqueAPI, testAPI2);
    }

    private static API createTestAPI(String apiPath, String vhost, String queryVersion) {
        API testAPI = new API();
        testAPI.setPath(apiPath);
        testAPI.setVhost(vhost);
        testAPI.setApiRoutingKey(queryVersion);
        return testAPI;
    }

    @Test
    public void createAPIProxy() throws AppException {
        setupParameters();
        APIManagerAPIAdapter apiManagerAPIAdapter = APIManagerAdapter.getInstance().apiAdapter;
        Organization organization = APIManagerAdapter.getInstance().orgAdapter.getOrgForName("orga");
        API api = new API();
        api.setName("petstore");
        api.setPath("/api/v3");
        api.setVersion("1.1");
        api.setDescriptionType("original");
        api.setSummary("Petstore api");
        api.setState("published");
        api.setOrganization(organization);
        API createdAPI = apiManagerAPIAdapter.createAPIProxy(api);
        Assert.assertNotNull(createdAPI);
    }

    @Test
    public void updateAPIProxy() throws AppException {
        setupParameters();
        APIManagerAPIAdapter apiManagerAPIAdapter = APIManagerAdapter.getInstance().apiAdapter;
        Organization organization = APIManagerAdapter.getInstance().orgAdapter.getOrgForName("orga");
        API api = new API();
        api.setName("petstore");
        api.setPath("/api/v3");
        api.setVersion("1.1");
        api.setDescriptionType("original");
        api.setSummary("Petstore api");
        api.setState("published");
        api.setOrganization(organization);
        api.setId("e4ded8c8-0a40-4b50-bc13-552fb7209150");
        API updateAPI = apiManagerAPIAdapter.updateAPIProxy(api);
        Assert.assertEquals(updateAPI.getState(), "published");
    }

    @Test
    public void deleteAPIProxy() throws AppException {
        setupParameters();
        APIManagerAPIAdapter apiManagerAPIAdapter = APIManagerAdapter.getInstance().apiAdapter;
        Organization organization = APIManagerAdapter.getInstance().orgAdapter.getOrgForName("orga");
        API api = new API();
        api.setName("petstore");
        api.setPath("/api/v3");
        api.setVersion("1.1");
        api.setDescriptionType("original");
        api.setSummary("Petstore api");
        api.setState("published");
        api.setOrganization(organization);
        api.setId("e4ded8c8-0a40-4b50-bc13-552fb7209150");
        try {
            apiManagerAPIAdapter.deleteAPIProxy(api);
        } catch (AppException e) {
            Assert.fail("Unable to delete API", e);
        }
    }

    @Test
    public void deleteBackendAPI() throws AppException {
        setupParameters();
        APIManagerAPIAdapter apiManagerAPIAdapter = APIManagerAdapter.getInstance().apiAdapter;
        Organization organization = APIManagerAdapter.getInstance().orgAdapter.getOrgForName("orga");
        API api = new API();
        api.setName("petstore");
        api.setPath("/api/v3");
        api.setVersion("1.1");
        api.setDescriptionType("original");
        api.setSummary("Petstore api");
        api.setState("published");
        api.setOrganization(organization);
        api.setId("e4ded8c8-0a40-4b50-bc13-552fb7209150");
        api.setApiId("1f4263ca-7f03-41d9-9d34-9eff79d29bd8");
        try {
            apiManagerAPIAdapter.deleteBackendAPI(api);
        } catch (AppException e) {
            Assert.fail("Unable to delete backend API", e);
        }
    }

    @Test
    public void publishAPIDoNothing() throws AppException {
        setupParameters();
        APIManagerAPIAdapter apiManagerAPIAdapter = APIManagerAdapter.getInstance().apiAdapter;
        Organization organization = APIManagerAdapter.getInstance().orgAdapter.getOrgForName("orga");
        API api = new API();
        api.setName("petstore");
        api.setPath("/api/v3");
        api.setVersion("1.1");
        api.setDescriptionType("original");
        api.setSummary("Petstore api");
        api.setState("published");
        api.setOrganization(organization);
        api.setId("e4ded8c8-0a40-4b50-bc13-552fb7209150");
        try {
            apiManagerAPIAdapter.publishAPI(api, "api.axway.com");
        } catch (AppException e) {
            Assert.fail("Unable to publish API", e);
        }
    }

    @Test
    public void publishAPI() throws AppException {
        setupParameters();
        APIManagerAPIAdapter apiManagerAPIAdapter = APIManagerAdapter.getInstance().apiAdapter;
        Organization organization = APIManagerAdapter.getInstance().orgAdapter.getOrgForName("orga");
        API api = new API();
        api.setName("petstore");
        api.setPath("/api/v3");
        api.setVersion("1.1");
        api.setDescriptionType("original");
        api.setSummary("Petstore api");
        api.setState("unpublished");
        api.setOrganization(organization);
        api.setId("e4ded8c8-0a40-4b50-bc13-552fb7209150");

        try {
            apiManagerAPIAdapter.publishAPI(api, "api.axway.com");
        } catch (AppException e) {
            Assert.fail("Unable to publish API", e);
        }
    }

    @Test
    public void getAPIDatFile() {

    }

    @Test
    public void updateAPIStatus() throws AppException {
        setupParameters();
        APIManagerAPIAdapter apiManagerAPIAdapter = APIManagerAdapter.getInstance().apiAdapter;
        Organization organization = APIManagerAdapter.getInstance().orgAdapter.getOrgForName("orga");
        API api = new API();
        api.setName("petstore");
        api.setPath("/api/v3");
        api.setVersion("1.1");
        api.setDescriptionType("original");
        api.setSummary("Petstore api");
        api.setState("unpublished");
        api.setOrganization(organization);
        api.setId("e4ded8c8-0a40-4b50-bc13-552fb7209150");

        try {
            apiManagerAPIAdapter.updateAPIStatus(api, "published", "api.axway.com");
        } catch (AppException e) {
            Assert.fail("Unable to update API status", e);
        }
    }

    @Test
    public void updateRetirementDate() throws AppException {
        setupParameters();
        APIManagerAPIAdapter apiManagerAPIAdapter = APIManagerAdapter.getInstance().apiAdapter;
        Organization organization = APIManagerAdapter.getInstance().orgAdapter.getOrgForName("orga");
        API api = new API();
        api.setName("petstore");
        api.setPath("/api/v3");
        api.setVersion("1.1");
        api.setDescriptionType("original");
        api.setSummary("Petstore api");
        api.setState("unpublished");
        api.setOrganization(organization);
        api.setId("e4ded8c8-0a40-4b50-bc13-552fb7209150");
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 30);

        try {
            apiManagerAPIAdapter.updateRetirementDate(api, calendar.getTimeInMillis());
        } catch (AppException e) {
            Assert.fail("Unable to update Retirement status", e);
        }
    }

    @Test
    public void importBackendAPI() throws AppException {
        setupParameters();
        APIManagerAPIAdapter apiManagerAPIAdapter = APIManagerAdapter.getInstance().apiAdapter;
        Organization organization = APIManagerAdapter.getInstance().orgAdapter.getOrgForName("orga");
        API api = new API();
        api.setName("petstore");
        api.setPath("/api/v3");
        api.setVersion("1.1");
        api.setDescriptionType("original");
        api.setSummary("Petstore api");
        api.setState("unpublished");
        api.setOrganization(organization);
        ClassLoader classLoader = this.getClass().getClassLoader();
        String specDirPath = classLoader.getResource("com/axway/apim/adapter/spec/").getFile();
        APISpecification apiSpecification = APISpecificationFactory.getAPISpecification("openapi.json", specDirPath, "petstore");
        api.setApiDefinition(apiSpecification);
        API importedAPI = apiManagerAPIAdapter.importBackendAPI(api);
        Assert.assertNotNull(importedAPI);
    }

    @Test
    public void upgradeAccessToNewerAPI() {

    }

    @Test
    public void grantClientOrganization() {

    }

    public void setupParameters() throws AppException {
        APIManagerAdapter.deleteInstance();
        CoreParameters coreParameters = new CoreParameters();
        coreParameters.setHostname("localhost");
        coreParameters.setUsername("test");
        coreParameters.setPassword(Utils.getEncryptedPassword());

    }

}
