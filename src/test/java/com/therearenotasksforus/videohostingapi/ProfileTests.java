package com.therearenotasksforus.videohostingapi;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = VideoHostingApiApplication.class,
        properties = { "spring.jpa.hibernate.ddl-auto=create-drop" })
@WebAppConfiguration
@AutoConfigureMockMvc
class ProfileTests {

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
    public void profilesListLoads() throws Exception {
        String uri = "/api/profiles";
        MvcResult mvcResult = TestMethods.getRequest(mvc, uri, userToken);
        ArrayList<Map<String, Object>> responseBodyArray = TestMethods.mapFromJsonList(mvcResult);

        Assertions.assertEquals(200, mvcResult.getResponse().getStatus());
        Assertions.assertNotEquals(0, responseBodyArray.size());
        Assertions.assertNotEquals(null, responseBodyArray.get(0).get("user"));
        Assertions.assertEquals(responseBodyArray.get(0).get("id"), responseBodyArray.get(0).get("user"));
    }

    @Test
    public void profileByIdLoads() throws Exception {
        String uri = "/api/profile/id/1";
        MvcResult mvcResult = TestMethods.getRequest(mvc, uri, userToken);
        Map<String, Object> responseBody = TestMethods.mapFromJson(mvcResult);

        Assertions.assertEquals(200, mvcResult.getResponse().getStatus());
        Assertions.assertNotEquals(null, responseBody.get("customUrl"));
        Assertions.assertEquals(responseBody.get("id"), responseBody.get("user"));
    }

    @Test
    public void profileByTokenLoads() throws Exception {
        String uri = "/api/profile";
        MvcResult mvcResult = TestMethods.getRequest(mvc, uri, userToken);
        Map<String, Object> responseBody = TestMethods.mapFromJson(mvcResult);

        Assertions.assertEquals(200, mvcResult.getResponse().getStatus());
        Assertions.assertNotEquals(null, responseBody.get("customUrl"));
        Assertions.assertEquals(responseBody.get("id"), responseBody.get("user"));
    }

    @Test
    public void profileUpdate() throws Exception {
        String uri = "/api/profile/update";

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("aboutProfileInfo", "Updated profile information");
        requestBody.put("gender", "M");
        requestBody.put("country", "USA");
        requestBody.put("customUrl", "test_custom_url");
        requestBody.put("privateSublist", "true");

        String jsonBody = TestMethods.mapToJson(requestBody);
        MvcResult mvcResult = TestMethods.postRequest(mvc, uri, userToken, jsonBody);
        Map <String, Object> responseBody = TestMethods.mapFromJson(mvcResult);

        Assertions.assertEquals(200, mvcResult.getResponse().getStatus());
        Assertions.assertEquals("Updated profile information", responseBody.get("aboutProfileInfo"));
        Assertions.assertEquals("M", responseBody.get("gender"));
        Assertions.assertEquals("USA", responseBody.get("country"));
        Assertions.assertEquals("test_custom_url", responseBody.get("customUrl"));
        Assertions.assertEquals(true, responseBody.get("privateSublist"));

        uri = "/api/profile";
        mvcResult = TestMethods.getRequest(mvc, uri, userToken);
        responseBody = TestMethods.mapFromJson(mvcResult);

        Assertions.assertEquals(200, mvcResult.getResponse().getStatus());
        Assertions.assertEquals("Updated profile information", responseBody.get("aboutProfileInfo"));
        Assertions.assertEquals("M", responseBody.get("gender"));
        Assertions.assertEquals("USA", responseBody.get("country"));
        Assertions.assertEquals("test_custom_url", responseBody.get("customUrl"));
        Assertions.assertEquals(true, responseBody.get("privateSublist"));
    }

    @Test
    public void profileUpdateAvatar() throws Exception {
        String uri = "/api/profile/upload/avatar";
        String prevAvatar = TestMethods
                .mapFromJson(TestMethods
                        .getRequest(mvc, "/api/profile", userToken))
                .get("avatarUrl").toString();

        final MockMultipartFile avatar = new MockMultipartFile("file",
                "test.png",
                "image/png",
                "test.png".getBytes());
        final MvcResult mvcResultUploadedImage = mvc.perform(MockMvcRequestBuilders
                .multipart(uri)
                .file(avatar)
                .headers(TestMethods.getHttpHeaders(userToken))
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andReturn();

        final String currentAvatar = TestMethods
                .mapFromJson(TestMethods
                        .getRequest(mvc, "/api/profile", userToken))
                .get("avatarUrl").toString();

        final int profileId = (int) TestMethods
                .mapFromJson(TestMethods
                        .getRequest(mvc, "/api/profile", userToken))
                .get("id");

        Assertions.assertEquals(201, mvcResultUploadedImage.getResponse().getStatus());
        Assertions.assertNotEquals(prevAvatar, currentAvatar);

        uri = "/api/profile/" + profileId + "/download/avatar";
        MvcResult mvcResultDownloadedImage = TestMethods.getRequest(mvc, uri, userToken);

        Assertions.assertEquals(200, mvcResultDownloadedImage.getResponse().getStatus());
    }
}
