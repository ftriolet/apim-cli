package com.axway.apim.test.envProperties;

import com.axway.apim.TestSetup;
import com.axway.apim.WiremockWrapper;
import com.axway.apim.adapter.APIManagerAdapter;
import com.axway.apim.api.API;
import com.axway.apim.apiimport.APIImportConfigAdapter;
import com.axway.apim.lib.CoreParameters;
import com.axway.apim.lib.EnvironmentProperties;
import com.axway.apim.lib.errorHandling.AppException;
import com.axway.apim.lib.utils.TestIndicator;
import com.axway.apim.lib.utils.Utils;
import com.axway.apim.test.basic.APIImportConfigAdapterTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

public class SubstituteVariablesTest  extends WiremockWrapper {

    @BeforeClass
    public void initWiremock() {
        super.initWiremock();
        APIManagerAdapter.apiManagerVersion = "7.7.20221130";
        TestIndicator.getInstance().setTestRunning(true);
        try {
            new TestSetup().initCliHome();
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }

    }
    @AfterClass
    public void close() {
        super.close();
    }
    public void setupParameters() throws AppException {
        APIManagerAdapter.deleteInstance();
        CoreParameters coreParameters = new CoreParameters();
        coreParameters.setHostname("localhost");
        coreParameters.setUsername("test");
        coreParameters.setPassword(Utils.getEncryptedPassword());

    }

    private static String OS = null;


    @Test
    public void validateSystemOSAreSubstituted() throws IOException {
        setupParameters();
        String configFile = "com/axway/apim/test/files/envProperties/1_config-with-os-variable.json";
        String pathToConfigFile = this.getClass().getClassLoader().getResource(configFile).getFile();
        String apiDefinition = "/api_definition_1/petstore.json";

        APIImportConfigAdapter importConfig = new APIImportConfigAdapter(pathToConfigFile, null, apiDefinition, null);

        API testAPI = importConfig.getApiConfig();
        if (System.getenv("TRAVIS") != null && System.getenv("TRAVIS").equals("true")) {
            // At Travis an environment-variable USER exists which should have been replaced
            Assert.assertNotEquals(testAPI.getName(), "${USER}");
        } else if (System.getenv("GITHUB_ACTIONS") != null && System.getenv("GITHUB_ACTIONS").equals("true")) {
            // When running at GitHub actions
            Assert.assertNotEquals(testAPI.getVhost(), "${GITHUB_ACTION}");
        } else {
            // On Windows use USERNAME in the version
            if (isWindows()) {
                Assert.assertNotEquals(testAPI.getVersion(), "${USERNAME}");
            } else { // MacOS X and Linux use USER in the name
                Assert.assertNotEquals(testAPI.getName(), "${USER}");
            }
        }
    }

    @Test
    public void validateBaseEnvReplacedOSAttribute() throws IOException {
        setupParameters();
        String path = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath() + "apimcli";
        Properties props = System.getProperties();
        props.setProperty("OS_AND_MAIN_ENV_PROPERTY", "valueFromOS");

        EnvironmentProperties envProps = new EnvironmentProperties(null, path);
        CoreParameters.getInstance().setProperties(envProps);

        String configFile = "com/axway/apim/test/files/envProperties/1_config-with-os-variable.json";
        String pathToConfigFile = this.getClass().getClassLoader().getResource(configFile).getFile();
        String apiDefinition = "/api_definition_1/petstore.json";

        APIImportConfigAdapter importConfig = new APIImportConfigAdapter(pathToConfigFile, null, apiDefinition, null);

        API testAPI = importConfig.getApiConfig();

        Assert.assertEquals(testAPI.getPath(), "valueFromMainEnv");
    }

    @Test
    public void validateStageEnvOveridesAll() throws IOException {
        setupParameters();
        String path = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath() + "apimcli";
        Properties props = System.getProperties();
        props.setProperty("OS_MAIN_AND_STAGE_ENV_PROPERTY", "valueFromOS");

        EnvironmentProperties envProps = new EnvironmentProperties("anyOtherStage", path);
        CoreParameters.getInstance().setProperties(envProps);

        String configFile = "com/axway/apim/test/files/envProperties/1_config-with-os-variable.json";
        String pathToConfigFile = this.getClass().getClassLoader().getResource(configFile).getFile();
        String apiDefinition = "/api_definition_1/petstore.json";

        APIImportConfigAdapter importConfig = new APIImportConfigAdapter(pathToConfigFile, null, apiDefinition, null);

        API testAPI = importConfig.getApiConfig();

        Assert.assertEquals(testAPI.getDescriptionManual(), "valueFromAnyOtherStageEnv");
    }

    private static String getOsName() {
        if (OS == null) {
            OS = System.getProperty("os.name");
        }
        return OS;
    }

    public static boolean isWindows() {
        return getOsName().startsWith("Windows");
    }
}
