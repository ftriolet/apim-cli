package com.axway.apim.appimport.it.basic;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.axway.apim.appimport.ApplicationImportTestAction;
import com.axway.apim.lib.errorHandling.AppException;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.annotations.CitrusTest;
import com.consol.citrus.context.TestContext;
import com.consol.citrus.dsl.testng.TestNGCitrusTestRunner;
import com.consol.citrus.functions.core.RandomNumberFunction;
import com.consol.citrus.message.MessageType;

@Test
public class ImportCompleteApplicationWithOauthResourcesTestIT extends TestNGCitrusTestRunner {

	private ApplicationImportTestAction appImport = new ApplicationImportTestAction();
	
	private static String PACKAGE = "/com/axway/apim/appimport/apps/basic/";
	
	@CitrusTest
	@Test @Parameters("context")
	public void importApplicationBasicTest(@Optional @CitrusResource TestContext context) throws IOException, AppException {
		description("Import application into API-Manager");
		
		variable("appNumber", RandomNumberFunction.getRandomNumber(4, true));
		variable("appName", "Complete-App-${appNumber}");
		variable("phone", "123456789-${appNumber}");
		variable("description", "My App-Description ${appNumber}");
		variable("email", "email-${appNumber}@customer.com");
		variable("quotaMessages", "9999");
		variable("quotaPeriod", "week");
		variable("state", "approved");
		variable("appImage", "app-image.jpg");
		variable("scope1","READ");
		variable("isDefault1",true);
		variable("scope2","WRITE");
		variable("isDefault2",false);

		echo("####### Import application: '${appName}' #######");		
		createVariable(ApplicationImportTestAction.CONFIG,  PACKAGE + "CompleteApplicationWithOauthResources.json");
		createVariable("expectedReturnCode", "0");
		appImport.doExecute(context);
		
		echo("####### Validate application: '${appName}' has been imported #######");
		http(builder -> builder.client("apiManager").send().get("/applications?field=name&op=eq&value=${appName}").header("Content-Type", "application/json"));

		http(builder -> builder.client("apiManager").receive().response(HttpStatus.OK).messageType(MessageType.JSON)
			.validate("$.[?(@.name=='${appName}')].name", "@assertThat(hasSize(1))@")
			.validate("$.[?(@.name=='${appName}')].phone", "${phone}")
			.validate("$.[?(@.name=='${appName}')].description", "${description}")
			.validate("$.[?(@.name=='${appName}')].email", "${email}")
			.validate("$.[?(@.name=='${appName}')].state", "${state}")
			.validate("$.[?(@.name=='${appName}')].image", "@assertThat(containsString(/api/portal/v))@")
			.extractFromPayload("$.[?(@.name=='${appName}')].id", "appId"));
		
		echo("####### Validate application: '${appName}' with id: ${appId} oauth resources has been imported #######");
		http(builder -> builder.client("apiManager").send().get("/applications/${appId}/oauthresource").header("Content-Type", "application/json"));
		
		http(builder -> builder.client("apiManager").receive().response(HttpStatus.OK).messageType(MessageType.JSON)
				.validate("$[1].applicationId", "${appId}")
				.validate("$.[?(@.scope=='${scope1}')].scope", "${scope1}")
				.validate("$.[?(@.scope=='${scope1}')].isDefault", "${isDefault1}")
				.validate("$.[?(@.scope=='${scope2}')].scope", "${scope2}")
				.validate("$.[?(@.scope=='${scope2}')].isDefault", "${isDefault2}"));

		
		echo("####### Validate application: '${appName}' with id: ${appId} OAuth has been imported #######");
		http(builder -> builder.client("apiManager").send().get("/applications/${appId}/oauth").header("Content-Type", "application/json"));
				
		echo("####### Re-Import same application - Should be a No-Change #######");
		createVariable("expectedReturnCode", "10");
		appImport.doExecute(context);
		
		echo("####### Re-Import with change in oauth resource isDefaultFlag - Existing App should be updated #######");
		variable("isDefault2",true);
		createVariable("expectedReturnCode", "0");
		appImport.doExecute(context);
		
		echo("####### Validate application: '${appName}' with id: ${appId} oauth resources has been imported #######");
		http(builder -> builder.client("apiManager").send().get("/applications/${appId}/oauthresource").header("Content-Type", "application/json"));
		
		http(builder -> builder.client("apiManager").receive().response(HttpStatus.OK).messageType(MessageType.JSON)
				.validate("$[1].applicationId", "${appId}")
				.validate("$.[?(@.scope=='${scope1}')].scope", "${scope1}")
				.validate("$.[?(@.scope=='${scope1}')].isDefault", "${isDefault1}")
				.validate("$.[?(@.scope=='${scope2}')].scope", "${scope2}")
				.validate("$.[?(@.scope=='${scope2}')].isDefault", "${isDefault2}"));
		
		
		echo("####### Re-Import change in scope name - Existing App should be updated #######");
		variable("scope2","WRITE2");
		createVariable("expectedReturnCode", "0");
		appImport.doExecute(context);
		
		echo("####### Validate application: '${appName}' (${appId}) has been updated #######");
		echo("####### Validate application: '${appName}' with id: ${appId} oauth resources has been imported #######");
		http(builder -> builder.client("apiManager").send().get("/applications/${appId}/oauthresource").header("Content-Type", "application/json"));
		
		http(builder -> builder.client("apiManager").receive().response(HttpStatus.OK).messageType(MessageType.JSON)
				.validate("$[1].applicationId", "${appId}")
				.validate("$.[?(@.scope=='${scope1}')].scope", "${scope1}")
				.validate("$.[?(@.scope=='${scope1}')].isDefault", "${isDefault1}")
				.validate("$.[?(@.scope=='${scope2}')].scope", "${scope2}")
				.validate("$.[?(@.scope=='${scope2}')].isDefault", "${isDefault2}"));
	}
}
