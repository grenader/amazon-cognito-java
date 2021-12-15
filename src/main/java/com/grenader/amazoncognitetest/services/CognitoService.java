package com.grenader.amazoncognitetest.services;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder;
import com.amazonaws.services.cognitoidp.model.*;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class CognitoService {

    private static final String AWS_REGION = System.getenv("AWS_REGION");
    private static final String USERNAME = "test{0}"; // Input an unique username for the UserPool
    private static final String PHONE_NUMBER = "+16473458932"; // Input the user phone number for the user Attribute
    private static final String USERPOOL_ID = System.getenv("USERPOOL_ID"); // Input the UserPool Id, e.g. us-east-1_xxxxxxxx
    private static final String USER_TEMP_PASSWORD = System.getenv("USER_TEMP_PASSWORD"); // Input the temporary password for the user
    private static final String USER_EMAIL = "test{0}-pool-snoopy-{1}@gmail.com"; // Input the email for the user attribute
    private boolean smsEnabled = false;
    private boolean emailEnabled = false;

     List<UserPoolDescriptionType> listAllUserPools(AWSCognitoIdentityProvider cognitoClient ) {

            ListUserPoolsRequest request = new ListUserPoolsRequest();
                    request.setMaxResults(10);

            ListUserPoolsResult response = cognitoClient.listUserPools(request);
        List<UserPoolDescriptionType> userPools = response.getUserPools();
        userPools.forEach(userpool -> {
                        System.out.println("User pool " + userpool.getName() + ", User ID " + userpool.getId() );
                    }
            );
            return userPools;
    }

    public List<UserPoolDescriptionType> listAllPools() {
        AWSCognitoIdentityProvider cognitoIdentityProvider = AWSCognitoIdentityProviderClientBuilder
                .standard()
                .withCredentials(new EnvironmentVariableCredentialsProvider())
//                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .withRegion(Regions.fromName(AWS_REGION))
                .build();

        return listAllUserPools(cognitoIdentityProvider);
    }


    public AdminGetUserResult createUserAndEnableMFA() {
        AWSCognitoIdentityProvider cognitoIdentityProvider = AWSCognitoIdentityProviderClientBuilder
                .standard()
                .withCredentials(new EnvironmentVariableCredentialsProvider())
//                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .withRegion(Regions.fromName(AWS_REGION))
                .build();

        // Create User in UserPool using AdminCreateUser
        // @see <a href="https://docs.aws.amazon.com/cognito-user-identity-pools/latest/APIReference/API_AdminCreateUser.html">label</a>
        int rnd1 = ThreadLocalRandom.current().nextInt(10, 1000);
        int rnd2 = ThreadLocalRandom.current().nextInt(10, 1000);
        String newUserName = MessageFormat.format(USERNAME, rnd1);
        String newUserEmail = MessageFormat.format(USER_EMAIL, rnd1, rnd2);
        System.out.println("newUserName = " + newUserName);
        System.out.println("newUserEmail = " + newUserEmail);
        cognitoIdentityProvider.adminCreateUser(
                new AdminCreateUserRequest()
                        .withUserPoolId(USERPOOL_ID)
                        .withUsername(newUserName)
                        .withTemporaryPassword(USER_TEMP_PASSWORD)
                        .withMessageAction(MessageActionType.SUPPRESS)
                        .withUserAttributes(
                                new AttributeType()
                                        .withName("phone_number")
                                        .withValue(PHONE_NUMBER),
                                new AttributeType()
                                        .withName("phone_number_verified")
                                        .withValue("true"),
                                new AttributeType()
                                        .withName("email")
                                        .withValue(newUserEmail)));


        if (smsEnabled) {
            SMSMfaSettingsType sMSMfaSettings = new SMSMfaSettingsType().withPreferredMfa(Boolean.TRUE).withEnabled(Boolean.TRUE);

            // Set MFA preferred type for the User using AdminSetUserMFAPreference
            // @see <a href="https://docs.aws.amazon.com/cognito-user-identity-pools/latest/APIReference/API_AdminSetUserMFAPreference.html">label</a>
            cognitoIdentityProvider.adminSetUserMFAPreference(
                    new AdminSetUserMFAPreferenceRequest()
                            .withSMSMfaSettings(sMSMfaSettings)
                            .withUserPoolId(USERPOOL_ID)
                            .withUsername(newUserName));
        } if (emailEnabled) {
            SoftwareTokenMfaSettingsType softwareTokenMfaSettings = new SoftwareTokenMfaSettingsType();

            // Set MFA preferred type for the User using AdminSetUserMFAPreference
            // @see <a href="https://docs.aws.amazon.com/cognito-user-identity-pools/latest/APIReference/API_AdminSetUserMFAPreference.html">label</a>
            cognitoIdentityProvider.adminSetUserMFAPreference(
                    new AdminSetUserMFAPreferenceRequest()
                            .withSoftwareTokenMfaSettings(softwareTokenMfaSettings)
                            .withUserPoolId(USERPOOL_ID)
                            .withUsername(newUserName));

        }

        if (emailEnabled || smsEnabled) {
            // Add MFA Options type for the User using AdminSetUserSettings
            // @see <a href="https://docs.aws.amazon.com/cognito-user-identity-pools/latest/APIReference/API_AdminSetUserSettings.html">label</a>
            cognitoIdentityProvider.adminSetUserSettings(
                    new AdminSetUserSettingsRequest()
                            .withUserPoolId(USERPOOL_ID)
                            .withUsername(newUserName));
            ;
/*
                        .withMFAOptions(Arrays.asList(
                                new MFAOptionType()
                                        .withDeliveryMedium("SMS")
                                        .withAttributeName("phone_number"))));
*/
        }

        // Validate the data created/updated in this class.
        AdminGetUserResult user = cognitoIdentityProvider.adminGetUser(
                new AdminGetUserRequest()
                        .withUserPoolId(USERPOOL_ID)
                        .withUsername(newUserName));

        return user;
    }

    public void signUp(AWSCognitoIdentityProvider identityProviderClient,
                              String clientId,
                              String secretKey,
                              String userName,
                              String password,
                              String email) {

        AttributeType attributeType = new AttributeType();
        attributeType.setName("email");
        attributeType.setValue(email);

        List<AttributeType> attrs = new ArrayList<>();
        attrs.add(attributeType);

        try {
            String secretVal = calculateSecretHash(clientId, secretKey, userName);
            SignUpRequest signUpRequest = new SignUpRequest();
            signUpRequest.setUserAttributes(attrs);
            signUpRequest.setUsername(userName);
            signUpRequest.setClientId(clientId);
            signUpRequest.setPassword(password);
            signUpRequest.setSecretHash(secretVal);

            identityProviderClient.signUp(signUpRequest);
            System.out.println("User has been signed up");

        } catch(PreconditionNotMetException | NotAuthorizedException e) {
            System.err.println(e.getRawResponseContent());
        }
    }

    String calculateSecretHash(String userPoolClientId, String userPoolClientSecret, String userName) {
        final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

        SecretKeySpec signingKey = new SecretKeySpec(
                userPoolClientSecret.getBytes(StandardCharsets.UTF_8),
                HMAC_SHA256_ALGORITHM);
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            mac.init(signingKey);
            mac.update(userName.getBytes(StandardCharsets.UTF_8));
            byte[] rawHmac = mac.doFinal(userPoolClientId.getBytes(StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Error while calculating ");
        }
    }

    void listUserPoolClients(AWSCognitoIdentityProvider cognitoIdentityProvider, String userPoolId)
    {
        ListUserPoolClientsRequest request = new ListUserPoolClientsRequest();
        request.setUserPoolId(userPoolId);
        ListUserPoolClientsResult result = cognitoIdentityProvider.listUserPoolClients(request);

        result.getUserPoolClients().forEach(userPoolClient -> {
                    System.out.println("User pool client " + userPoolClient.getClientName() + ", Pool ID " + userPoolClient.getUserPoolId() + ", Client ID " + userPoolClient.getClientId() );
                }
        );
    }

    public void poolClients() {
        AWSCognitoIdentityProvider cognitoIdentityProvider = AWSCognitoIdentityProviderClientBuilder
                .standard()
                .withCredentials(new EnvironmentVariableCredentialsProvider())
//                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .withRegion(Regions.fromName(AWS_REGION))
                .build();
        listUserPoolClients(cognitoIdentityProvider, USERPOOL_ID);

    }

    public void userSignUp() {

        AWSCognitoIdentityProvider cognitoIdentityProvider = AWSCognitoIdentityProviderClientBuilder
                .standard()
                .withCredentials(new EnvironmentVariableCredentialsProvider())
//                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .withRegion(Regions.fromName(AWS_REGION))
                .build();

        signUp(cognitoIdentityProvider,
                System.getenv("CLIENT_ID"),
                System.getenv("SECRET_ID"),
                "test02",
                USER_TEMP_PASSWORD,
                "test01-pool-samantha-01@gmail.com");
    }
}
