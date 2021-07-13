package com.therearenotasksforus.videohostingapi;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = VideoHostingApiApplication.class,
        properties = { "spring.jpa.hibernate.ddl-auto=create-drop" })
@WebAppConfiguration
@AutoConfigureMockMvc
class UserTests {

    @Autowired
    public MockMvc mvc;

    @Autowired
    protected WebApplicationContext webApplicationContext;

    public String userToken;

    public void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @BeforeEach
    public void preTest() throws Exception {
        TestMethods.register(mvc);
        userToken = TestMethods.getToken(mvc);
    }

    @Test
    public void usersListLoads() throws Exception {
        String uri = "/api/users";
        MvcResult mvcResult = TestMethods.getRequest(mvc, uri, userToken);
        ArrayList<Map<String, Object>> responseBodyArray = TestMethods.mapFromJsonList(mvcResult);

        Assertions.assertEquals(200, mvcResult.getResponse().getStatus());
        Assertions.assertNotEquals(0, responseBodyArray.size());
        Assertions.assertNotEquals(null, responseBodyArray.get(0).get("username"));
        Assertions.assertEquals(responseBodyArray.get(0).get("id"), responseBodyArray.get(0).get("profile"));
        Assertions.assertNotEquals(null, responseBodyArray.get(0).get("email"));
    }

    @Test
    public void userByIdLoads() throws Exception {
        String uri = "/api/user/id/1";
        MvcResult mvcResult = TestMethods.getRequest(mvc, uri, userToken);
        Map<String, Object> responseBody = TestMethods.mapFromJson(mvcResult);

        Assertions.assertEquals(200, mvcResult.getResponse().getStatus());
        Assertions.assertNotEquals(null, responseBody.get("username"));
        Assertions.assertEquals(responseBody.get("id"), responseBody.get("profile"));
        Assertions.assertNotEquals(null, responseBody.get("email"));
    }

    @Test
    public void userByTokenLoads() throws Exception {
        String uri = "/api/user";
        MvcResult mvcResult = TestMethods.getRequest(mvc, uri, userToken);
        Map<String, Object> responseBody = TestMethods.mapFromJson(mvcResult);

        Assertions.assertEquals(200, mvcResult.getResponse().getStatus());
        Assertions.assertNotEquals(null, responseBody.get("username"));
        Assertions.assertEquals(responseBody.get("id"), responseBody.get("profile"));
        Assertions.assertNotEquals(null, responseBody.get("email"));
    }

    @Test
    public void userUpdate() throws Exception {
        String uri = "/api/user/update";

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("firstName", "updated_first_name");
        requestBody.put("lastName", "updated_last_name");

        String jsonBody = TestMethods.mapToJson(requestBody);
        MvcResult mvcResult = TestMethods.postRequest(mvc, uri, userToken, jsonBody);
        Map <String, Object> responseBody = TestMethods.mapFromJson(mvcResult);

        Assertions.assertEquals(200, mvcResult.getResponse().getStatus());
        Assertions.assertEquals("updated_first_name", responseBody.get("firstName"));
        Assertions.assertEquals("updated_last_name", responseBody.get("lastName"));

        uri = "/api/user";
        mvcResult = TestMethods.getRequest(mvc, uri, userToken);
        responseBody = TestMethods.mapFromJson(mvcResult);

        Assertions.assertEquals(200, mvcResult.getResponse().getStatus());
        Assertions.assertEquals("updated_first_name", responseBody.get("firstName"));
        Assertions.assertEquals("updated_last_name", responseBody.get("lastName"));
    }

    @Test
    public void passwordUpdate() throws Exception {
        String uri = "/api/auth/password/update";
        String update_password = "updatedPassword123!";
        String old_password = "Asdf123!";
        String jsonBody = TestMethods.mapToJson(Map.ofEntries(
                Map.entry("updated_password", update_password),
                Map.entry("old_password", old_password)
        ));

        MvcResult mvcResult = TestMethods.postRequest(mvc, uri, userToken, jsonBody);
        Map<String, Object> response = TestMethods.mapFromJson(mvcResult);

        Assertions.assertEquals(200, mvcResult.getResponse().getStatus());
        Assertions.assertTrue(new BCryptPasswordEncoder().matches(update_password, (String) response.get("password")));

        TestMethods.postRequest(mvc, uri, userToken, TestMethods.mapToJson(Map.ofEntries(
                Map.entry("updated_password", old_password),
                Map.entry("old_password", update_password)
        )));
    }
}