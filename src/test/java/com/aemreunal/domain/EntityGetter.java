package com.aemreunal.domain;

/*
 ***************************
 * Copyright (c) 2014      *
 *                         *
 * This code belongs to:   *
 *                         *
 * @author Ahmet Emre Ünal *
 * S001974                 *
 *                         *
 * aemreunal@gmail.com     *
 * emre.unal@ozu.edu.tr    *
 *                         *
 * aemreunal.com           *
 ***************************
 */

import com.aemreunal.helper.RestHelper;
import com.jayway.restassured.path.json.JsonPath;

public class EntityGetter {

    protected static JsonPath getEntity(String path) {
        JsonPath responseJson = RestHelper.getEntityRequest(path);
        System.out.println("Get response:");
        responseJson.prettyPrint();
        return responseJson;
    }
}
