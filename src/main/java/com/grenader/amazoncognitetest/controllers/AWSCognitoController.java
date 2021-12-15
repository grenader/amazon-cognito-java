package com.grenader.amazoncognitetest.controllers;

import com.amazonaws.services.cognitoidp.model.AdminGetUserResult;
import com.grenader.amazoncognitetest.services.CognitoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AWSCognitoController {

    @Autowired
    private CognitoService service;

    @GetMapping("/enableMFA")
    public String getEnableMFAForCognitoUser()
    {
        AdminGetUserResult userResult = service.createUserAndEnableMFA();

        return "Call result: "+ userResult;
    }
}
