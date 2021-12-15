package com.grenader.amazoncognitetest.services;

import com.amazonaws.services.cognitoidp.model.AdminGetUserResult;
import com.amazonaws.services.cognitoidp.model.UserPoolDescriptionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
class CognitoServiceTest {

    CognitoService service = new CognitoService();

    @Test
    void testCreateUserAndEnableMFA() {

        AdminGetUserResult user = service.createUserAndEnableMFA();

        System.out.println("user = " + user);
        assertNull(user.getMFAOptions());
        assertTrue(user.getEnabled());
    }

    @Test
    void testListAllPools() {
        List<UserPoolDescriptionType> userPools = service.listAllPools();

        System.out.println("userPools.size() = " + userPools.size());
    }


    @Test
    void testUserSignUp() {
        service.userSignUp();
    }

    @Test
    void testPoolClients() {
        service.poolClients();
    }


}