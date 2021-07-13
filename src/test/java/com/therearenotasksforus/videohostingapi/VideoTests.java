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
class VideoTests {

    @Autowired
    public MockMvc mvc;

    @Autowired
    protected WebApplicationContext webApplicationContext;

    public String userToken;
    public int channelId;

    public void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @BeforeEach
    public void preTest() throws Exception {
        TestMethods.register(mvc);
        userToken = TestMethods.getToken(mvc);
        channelId = (int) TestMethods.mapFromJson(TestMethods.createChannel(mvc, userToken)).get("id");
    }

    @Test
    public void videoListLoads() throws Exception {
        String uri = "/api/videos";

        MvcResult mvcResult = TestMethods.getRequest(mvc, uri, userToken);
        int status = mvcResult.getResponse().getStatus();

        Assertions.assertEquals(200, status);
    }

    @Test
    public void uploadVideoByChannelOwner() throws Exception {
        String uri = "/api/channel/" + channelId + "/upload/video";

        final MockMultipartFile videoFile = new MockMultipartFile("file",
                "test.mp4",
                "video/mp4",
                "test video".getBytes());

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders
                .multipart(uri)
                .file(videoFile)
                .headers(TestMethods.getHttpHeaders(userToken))
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andReturn();

        Map<String, Object> responseBody = TestMethods.mapFromJson(mvcResult);

        Assertions.assertEquals(201, mvcResult.getResponse().getStatus());
        Assertions.assertEquals(channelId, responseBody.get("channel"));

        mvcResult = TestMethods.getRequest(mvc, "/api/channel/" + channelId + "/videos", userToken);
        ArrayList<Map<String, Object>> responseBodyChannelVideosList = TestMethods
                .mapFromJsonList(mvcResult);

        Assertions.assertNotEquals(0, responseBodyChannelVideosList.size());
    }

    @Test
    public void uploadVideoByChannelRandomUser() throws Exception {
        String uri = "/api/channel/" + channelId + "/upload/video";

        TestMethods.registerWithEmailAndUsername(
                mvc, "randomUserVideo@upload.com", "randomUserVideo");

        String randomUserToken = TestMethods.getTokenWithUsername(mvc, "randomUserVideo");

        final MockMultipartFile videoFile = new MockMultipartFile("file",
                "test.mp4",
                "video/mp4",
                "test video".getBytes());

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders
                .multipart(uri)
                .file(videoFile)
                .headers(TestMethods.getHttpHeaders(randomUserToken))
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andReturn();

        Assertions.assertEquals(403, mvcResult.getResponse().getStatus());
    }

    @Test
    public void videoLoadsById() throws Exception {
        MvcResult uploadedVideo = TestMethods
                .uploadVideoWithUriAndToken(mvc, "/api/channel/"
                        + channelId
                        + "/upload/video", userToken);
        String uri = "/api/video/" + (int) TestMethods.mapFromJson(uploadedVideo).get("id");
        MvcResult mvcResult = TestMethods.getRequest(mvc, uri, userToken);

        Assertions.assertEquals(200, mvcResult.getResponse().getStatus());
    }

    @Test
    public void likeVideo() throws Exception {

        MvcResult uploadedVideo = TestMethods
                .uploadVideoWithUriAndToken(mvc, "/api/channel/"
                        + channelId
                        + "/upload/video", userToken);

        TestMethods.mapFromJson(uploadedVideo).get("id");
        String uri = "/api/video/" + TestMethods.mapFromJson(uploadedVideo).get("id") + "/like";
        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders
                .post(uri)
                .headers(TestMethods.getHttpHeaders(userToken)))
                .andReturn();
        ArrayList<Object> likeList = TestMethods.getLongArrayByKey(TestMethods.mapFromJson(mvcResult), "likes");

        Assertions.assertEquals(200, mvcResult.getResponse().getStatus());
        Assertions.assertNotEquals(0, likeList.size());

        mvcResult = TestMethods.getRequest(mvc, "/api/profile", userToken);
        likeList = TestMethods.getLongArrayByKey(TestMethods.mapFromJson(mvcResult), "likes");

        Assertions.assertNotEquals(0, likeList.size());

        mvcResult = mvc.perform(MockMvcRequestBuilders
                .post(uri)
                .headers(TestMethods.getHttpHeaders(userToken)))
                .andReturn();
        likeList = TestMethods.getLongArrayByKey(TestMethods.mapFromJson(mvcResult), "likes");

        Assertions.assertEquals(200, mvcResult.getResponse().getStatus());
        Assertions.assertEquals(0, likeList.size());
    }

    @Test
    public void dislikeVideo() throws Exception {
        MvcResult uploadedVideo = TestMethods
                .uploadVideoWithUriAndToken(mvc, "/api/channel/" + channelId + "/upload/video", userToken);
        String uri = "/api/video/" + TestMethods.mapFromJson(uploadedVideo).get("id") + "/dislike";

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders
                .post(uri)
                .headers(TestMethods.getHttpHeaders(userToken)))
                .andReturn();
        ArrayList<Object> dislikeList = TestMethods.getLongArrayByKey(TestMethods.mapFromJson(mvcResult), "dislikes");

        Assertions.assertEquals(200, mvcResult.getResponse().getStatus());
        Assertions.assertNotEquals(0, dislikeList.size());

        mvcResult = mvc.perform(MockMvcRequestBuilders
                .post(uri)
                .headers(TestMethods.getHttpHeaders(userToken)))
                .andReturn();
        dislikeList = TestMethods.getLongArrayByKey(TestMethods.mapFromJson(mvcResult), "dislikes");

        Assertions.assertEquals(200, mvcResult.getResponse().getStatus());
        Assertions.assertEquals(0, dislikeList.size());
    }

    @Test
    public void commentVideo() throws Exception {
        MvcResult uploadedVideo = TestMethods
                .uploadVideoWithUriAndToken(mvc, "/api/channel/" + channelId + "/upload/video", userToken);

        int videoId = (int) TestMethods.mapFromJson(uploadedVideo).get("id");
        String uri = "/api/video/" + videoId + "/comment";

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("commentBody", "Test comment");

        String requestBodyJson = TestMethods.mapToJson(requestBody);
        MvcResult mvcResult = TestMethods.postRequest(mvc, uri, userToken, requestBodyJson);

        Assertions.assertEquals(201, mvcResult.getResponse().getStatus());

        uri = "/api/video/" + videoId + "/get/comments";
        mvcResult = TestMethods.getRequest(mvc, uri, userToken);
        ArrayList<Map<String, Object>> responseBody = TestMethods.mapFromJsonList(mvcResult);

        Assertions.assertEquals(1, responseBody.size());
        Assertions.assertTrue(responseBody.get(0).containsKey("commentBody") &&
                responseBody.get(0).containsValue("Test comment"));

        uri = "/api/video/" + videoId + "/comment/" + responseBody.get(0).get("id");
        mvcResult = mvc.perform(MockMvcRequestBuilders.delete(uri)
                .headers(TestMethods.getHttpHeaders(userToken)))
                .andReturn();

        Assertions.assertEquals(200, mvcResult.getResponse().getStatus());

        uri = "/api/video/" + videoId + "/get/comments";
        mvcResult = TestMethods.getRequest(mvc, uri, userToken);

        Assertions.assertEquals(0, TestMethods.mapFromJsonList(mvcResult).size());
    }

    @Test
    public void usersLikedVideosListLoads() throws Exception {
        MvcResult uploadedVideo = TestMethods
                .uploadVideoWithUriAndToken(mvc, "/api/channel/" + channelId + "/upload/video", userToken);
        String uri = "/api/video/" + TestMethods.mapFromJson(uploadedVideo).get("id") + "/like";
        mvc.perform(MockMvcRequestBuilders
                .post(uri)
                .headers(TestMethods.getHttpHeaders(userToken)))
                .andReturn();

        uri = "/api/profile";
        MvcResult mvcResult = TestMethods.getRequest(mvc, uri, userToken);
        int profileId = (int) TestMethods.mapFromJson(mvcResult).get("id");

        uri = "/api/profile/" + profileId + "/likedvideos";
        mvcResult = TestMethods.getRequest(mvc, uri, userToken);
        ArrayList<Map<String, Object>> requestBody = TestMethods.mapFromJsonList(mvcResult);

        Assertions.assertEquals(200, mvcResult.getResponse().getStatus());
        Assertions.assertNotEquals(0, requestBody.size());
    }

    @Test
    public void updateVideoName() throws Exception {
        MvcResult uploadedVideo = TestMethods
                .uploadVideoWithUriAndToken(mvc, "/api/channel/" + channelId + "/upload/video", userToken);

        int videoId = (int) TestMethods.mapFromJson(uploadedVideo).get("id");
        String uri = "/api/video/" + videoId + "/update/name";
        String requestBodyJson = TestMethods.mapToJson(Map.of("name", "updated name"));

        MvcResult mvcResult = TestMethods.postRequest(mvc, uri, userToken, requestBodyJson);
        Map<String, Object> responseBody = TestMethods.mapFromJson(mvcResult);

        Assertions.assertEquals(200, mvcResult.getResponse().getStatus());
        Assertions.assertEquals("updated name", responseBody.get("name"));
    }

    @Test
    public void updateVideoNameByRandomUser() throws Exception {
        MvcResult uploadedVideo = TestMethods
                .uploadVideoWithUriAndToken(mvc, "/api/channel/" + channelId + "/upload/video", userToken);
        int videoId = (int) TestMethods.mapFromJson(uploadedVideo).get("id");
        TestMethods.registerWithEmailAndUsername(
                mvc,
                "randomUserVideoNameUpdate@upload.com",
                "randomUserVideoNameUpdate");

        String randomUserToken = TestMethods.getTokenWithUsername(mvc, "randomUserVideoNameUpdate");
        String uri = "/api/video/" + videoId + "/update/name";
        String requestBodyJson = TestMethods.mapToJson(Map.of("name", "updated name"));

        MvcResult mvcResult = TestMethods.postRequest(mvc, uri, randomUserToken, requestBodyJson);
        int status = mvcResult.getResponse().getStatus();

        Assertions.assertEquals(403, status);
    }

    @Test
    public void videoDelete() throws Exception {
        int videoId = (int) TestMethods.mapFromJson(TestMethods
                .uploadVideoWithUriAndToken(mvc, "/api/channel/" + channelId + "/upload/video", userToken)).get("id");

        TestMethods.postRequest(mvc, "/api/video/" + videoId + "/like", userToken, "");

        int amountOfLikesBefore = TestMethods
                .mapFromJsonList(TestMethods
                        .getRequest(mvc, "/api/profile/" + TestMethods
                                .mapFromJson(TestMethods
                                        .getRequest(mvc, "/api/profile", userToken))
                                .get("id") + "/likedvideos", userToken)).size();


        ArrayList<Map<String, Object>> beforeVideos = TestMethods.
                mapFromJsonList(TestMethods.getRequest(mvc, "/api/videos", userToken));

        Assertions.assertNotEquals(0, TestMethods.mapFromJsonList(TestMethods
                .getRequest(mvc, "/api/videos", userToken)).size());

        String uri = "/api/video/" + videoId;
        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.delete(uri)
                .headers(TestMethods.getHttpHeaders(userToken)))
                .andReturn();

        Assertions.assertEquals(200, mvcResult.getResponse().getStatus());

        ArrayList<Map<String, Object>> afterVideos = TestMethods
                .mapFromJsonList(TestMethods.getRequest(mvc, "/api/videos", userToken));

        int amountOfLikesAfter = TestMethods.mapFromJsonList(TestMethods.getRequest(mvc,
                "/api/profile/" + TestMethods
                        .mapFromJson(TestMethods
                                .getRequest(mvc,
                                        "/api/profile",
                                        userToken)).get("id") + "/likedvideos", userToken)).size();

        Assertions.assertEquals(beforeVideos.size(), afterVideos.size());
        Assertions.assertEquals(amountOfLikesBefore, amountOfLikesAfter);
    }

}
