/*
 * jPOS Project [http://jpos.org]
 * Copyright (C) 2000-2018 jPOS Software SRL
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jpos.qrest;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.builder.RequestSpecBuilder;
import org.jpos.q2.Q2;
import org.jpos.util.NameRegistrar;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.given;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;

import static com.jayway.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

public class RestTest  {
    private static final String BASE_URL = "http://localhost:8081/";
    private static Q2 q2;
    
    @BeforeClass
    public static void setUp() throws NameRegistrar.NotFoundException {
        RestAssured.baseURI = BASE_URL;
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.requestSpecification = new RequestSpecBuilder().build().contentType(APPLICATION_JSON.toString());
        if (q2 == null) {
            q2 = new Q2();
            q2.start();
            NameRegistrar.get("qrest", 60000L);
            NameRegistrar.get("qrest"); // this time throw exception
        }
    }

    @Test
    public void test404()  {
        given()
          .expect().statusCode(404)
          .then().log().all()
          .when().get("invalid");
    }

    @Test
    public void testVersion()  {
        given().log().all()
        .get("q2/version").then().statusCode(200).assertThat()
          .body("version", equalTo(
            String.format("jPOS %s %s/%s (%s)",
              Q2.getVersion(), Q2.getBranch(), Q2.getRevision(), Q2.getBuildTimestamp()
            )
          ));
    }

    @Test
    public void testAll()  {
        given().log().all()
        .get("q2").then().statusCode(200).assertThat()
          .body("version", equalTo(
            String.format("jPOS %s %s/%s (%s)",
              Q2.getVersion(), Q2.getBranch(), Q2.getRevision(), Q2.getBuildTimestamp()
            )
          ));
    }
}

