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

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = VideoHostingApiApplication.class,
        properties = {"spring.jpa.hibernate.ddl-auto=create-drop"})
@WebAppConfiguration
@AutoConfigureMockMvc
class ChannelTests {

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
    public void channelCreate() throws Exception {
        String uri = "/api/channel/create";

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("name", "FirstTestChannel");
        requestBody.put("info", "FirstTestChannel information");

        String requestBodyJson = TestMethods.mapToJson(requestBody);
        MvcResult mvcResult = TestMethods.postRequest(mvc, uri, userToken, requestBodyJson);
        Map<String, Object> responseBody = TestMethods.mapFromJson(mvcResult);
        int channelId = (int) responseBody.get("id");

        Assertions.assertEquals(201, mvcResult.getResponse().getStatus());
        Assertions.assertEquals("FirstTestChannel", responseBody.get("name"));
        Assertions.assertEquals("FirstTestChannel information", responseBody.get("info"));

        uri = "/api/channels";

        mvcResult = TestMethods.getRequest(mvc, uri, userToken);
        ArrayList<Map<String, Object>> responseBodyArray = TestMethods.mapFromJsonList(mvcResult);

        Assertions.assertEquals(200, mvcResult.getResponse().getStatus());
        Assertions.assertNotEquals(0, responseBodyArray.size());
        Assertions.assertEquals("FirstTestChannel", responseBodyArray.get(channelId - 1).get("name"));
        Assertions.assertEquals("FirstTestChannel information", responseBodyArray.get(channelId - 1).get("info"));

        uri = "/api/channel/" + channelId;
        mvcResult = TestMethods.getRequest(mvc, uri, userToken);
        responseBody = TestMethods.mapFromJson(mvcResult);

        Assertions.assertEquals(200, mvcResult.getResponse().getStatus());
        Assertions.assertNotEquals(0, responseBodyArray.size());
        Assertions.assertEquals("FirstTestChannel", responseBody.get("name"));
        Assertions.assertEquals("FirstTestChannel information", responseBody.get("info"));
    }

    @Test
    public void channelListLoads() throws Exception {
        String uri = "/api/channel/create";

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("name", "SecondTestChannel");
        requestBody.put("info", "SecondTestChannel information");

        String requestBodyJson = TestMethods.mapToJson(requestBody);
        TestMethods.postRequest(mvc, uri, userToken, requestBodyJson);

        uri = "/api/channels";
        MvcResult mvcResult = TestMethods.getRequest(mvc, uri, userToken);
        ArrayList<Map<String, Object>> responseBodyArray = TestMethods.mapFromJsonList(mvcResult);

        Assertions.assertEquals(200, mvcResult.getResponse().getStatus());
        Assertions.assertNotEquals(0, responseBodyArray.size());
    }

    @Test
    public void channelUpdate() throws Exception {
        MvcResult mvcResult = TestMethods.createChannel(mvc, userToken);

        int channelId = (int) TestMethods.mapFromJson(mvcResult).get("id");
        String uri = "/api/channel/" + channelId + "/update";

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("name", "Updated name");
        requestBody.put("info", "Updated info");

        String requestBodyJson = TestMethods.mapToJson(requestBody);
        mvcResult = TestMethods.postRequest(mvc, uri, userToken, requestBodyJson);
        Map<String, Object> responseBody = TestMethods.mapFromJson(mvcResult);

        Assertions.assertEquals(200, mvcResult.getResponse().getStatus());
        Assertions.assertEquals("Updated name", responseBody.get("name"));
        Assertions.assertEquals("Updated info", responseBody.get("info"));
    }

    @Test
    public void channelSubscriptionByOwner() throws Exception {
        MvcResult mvcResult = TestMethods.createChannel(mvc, userToken);

        int channelId = (int) TestMethods.mapFromJson(mvcResult).get("id");
        String uri = "/api/channel/" + channelId + "/subscribe";

        mvcResult = mvc.perform(MockMvcRequestBuilders.post(uri)
                .headers(TestMethods.getHttpHeaders(userToken))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        Assertions.assertEquals(400, mvcResult.getResponse().getStatus());
    }

    @Test
    public void channelSubscriptionByRandomUser() throws Exception {
        MvcResult mvcResult = TestMethods.createChannel(mvc, userToken);

        int channelId = (int) TestMethods.mapFromJson(mvcResult).get("id");

        String uri = "/api/channel/" + channelId + "/subscribe";
        int secondUserId = TestMethods.registerWithEmailAndUsername(
                mvc,
                "random@com.com",
                "SubTestUser"
        );

        String customUserToken = TestMethods.getTokenWithUsername(mvc, "SubTestUser");
        mvcResult = mvc.perform(MockMvcRequestBuilders.post(uri)
                .headers(TestMethods.getHttpHeaders(customUserToken))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        Assertions.assertEquals(200, mvcResult.getResponse().getStatus());

        uri = "/api/channel/" + channelId;
        mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)
                .headers(TestMethods.getHttpHeaders(customUserToken)))
                .andReturn();

        Map<String, Object> responseBody = TestMethods.mapFromJson(mvcResult);
        ArrayList<Object> subList = TestMethods.getLongArrayByKey(responseBody, "subscribers");

        Assertions.assertNotEquals(0, subList.size());
        Assertions.assertTrue(subList.contains(secondUserId));
    }

    @Test
    public void channelUnsubscribeByOwner() throws Exception {
        MvcResult mvcResult = TestMethods.createChannel(mvc, userToken);

        int channelId = (int) TestMethods.mapFromJson(mvcResult).get("id");
        String uri = "/api/channel/" + channelId + "/unsubscribe";
        mvcResult = mvc.perform(MockMvcRequestBuilders.post(uri)
                .headers(TestMethods.getHttpHeaders(userToken))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        Assertions.assertEquals(400, mvcResult.getResponse().getStatus());
    }

    @Test
    public void channelUnsubscribeByUnsubscribedUser() throws Exception {
        MvcResult mvcResult = TestMethods.createChannel(mvc, userToken);

        int channelId = (int) TestMethods.mapFromJson(mvcResult).get("id");
        String uri = "/api/channel/" + channelId + "/unsubscribe";
        TestMethods.registerWithEmailAndUsername(mvc, "random@mail.com", "unSubTestUser");
        String customUserToken = TestMethods.getTokenWithUsername(mvc, "unSubTestUser");
        mvcResult = mvc.perform(MockMvcRequestBuilders.post(uri)
                .headers(TestMethods.getHttpHeaders(customUserToken))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        Assertions.assertEquals(400, mvcResult.getResponse().getStatus());
    }

    @Test
    public void channelUnsubscribeBySubscribedUser() throws Exception {
        MvcResult mvcResult = TestMethods.createChannel(mvc, userToken);
        int channelId = (int) TestMethods.mapFromJson(mvcResult).get("id");
        String uri = "/api/channel/" + channelId + "/subscribe";
        TestMethods.registerWithEmailAndUsername(mvc, "random@gmail.com", "unSubTestUserSubscribed");
        String customUserToken = TestMethods.getTokenWithUsername(mvc, "unSubTestUserSubscribed");

        mvc.perform(MockMvcRequestBuilders.post(uri)
                .headers(TestMethods.getHttpHeaders(customUserToken))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        uri = "/api/channel/" + channelId + "/unsubscribe";
        mvcResult = mvc.perform(MockMvcRequestBuilders.post(uri)
                .headers(TestMethods.getHttpHeaders(customUserToken))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        Assertions.assertEquals(200, mvcResult.getResponse().getStatus());
    }

    @Test
    public void channelDelete() throws Exception {
        int channelId = (int) TestMethods
                .mapFromJson(TestMethods
                        .createChannel(mvc, userToken))
                .get("id");

        int ownedChannelBefore = TestMethods
                .mapFromJsonList(TestMethods.getRequest(mvc, "/api/channels/owned", userToken))
                .size();

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders
                .delete("/api/channel/" + channelId)
                .headers(TestMethods.getHttpHeaders(userToken)))
                .andReturn();

        int ownedChannelAfter = TestMethods
                .mapFromJsonList(TestMethods.getRequest(mvc, "/api/channels/owned", userToken))
                .size();

        Assertions.assertEquals(200, mvcResult.getResponse().getStatus());
        Assertions.assertNotEquals(ownedChannelBefore, ownedChannelAfter);
    }

    @Test
    public void channelUpdateAvatar() throws Exception {
        int channelId = (int) TestMethods
                .mapFromJson(TestMethods.createChannel(mvc, userToken)).get("id");

        String uri = "/api/channel/" + channelId + "/upload/avatar";
        String prevAvatar = TestMethods
                .mapFromJson(TestMethods
                        .getRequest(mvc, "/api/channel/" + channelId, userToken))
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
                        .getRequest(mvc, "/api/channel/" + channelId, userToken))
                .get("avatarUrl").toString();

        Assertions.assertEquals(201, mvcResultUploadedImage.getResponse().getStatus());
        Assertions.assertNotEquals(prevAvatar, currentAvatar);
        Assertions.assertNotEquals(null, currentAvatar);
        Assertions.assertNotEquals("", currentAvatar);

        uri = "/api/channel/" + channelId + "/download/avatar";
        MvcResult mvcResultDownloadedImage = TestMethods.getRequest(mvc, uri, userToken);

        Assertions.assertEquals(200, mvcResultDownloadedImage.getResponse().getStatus());
    }

}
