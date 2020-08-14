package com.therearenotasksforus.videohostingapi;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@SpringBootTest(properties = { "spring.jpa.hibernate.ddl-auto=create-drop" })
@RunWith(SpringRunner.class)
class VideoTests extends AbstractTest{

    @Test
    public void videoListLoads() throws Exception {
        super.register();
        String token = super.getToken();
        String uri = "/api/videos";

        MvcResult mvcResult = super.getRequest(uri, token);
        int status = mvcResult.getResponse().getStatus();

        assertEquals(200, status);
    }

    @Test
    public void uploadVideoByChannelOwner() throws Exception {
        super.register();
        String token = super.getToken();
        int channelId = (int)super.mapFromJson(super.createChannel(token)).get("id");
        String uri = "/api/channel/" + channelId + "/upload/video";

        final MockMultipartFile videoFile = new MockMultipartFile("file",
                "test.mp4",
                "video/mp4",
                "test video".getBytes());

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders
                .multipart(uri)
                .file(videoFile)
                .headers(this.getHttpHeaders(token))
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andReturn();

        Map<String, Object> responseBody = super.mapFromJson(mvcResult);

        int status = mvcResult.getResponse().getStatus();

        assertEquals(201, status);
        assertEquals(channelId, responseBody.get("channel"));

        mvcResult = super.getRequest("/api/channel/" + channelId + "/videos", token);

        ArrayList<Map<String, Object>> responseBodyChannelVideosList = super
                .mapFromJsonList(mvcResult);
        assertNotEquals(0, responseBodyChannelVideosList.size());
    }

    @Test
    public void uploadVideoByChannelRandomUser() throws Exception {
        super.register();
        String token = super.getToken();
        int channelId = (int)super.mapFromJson(super.createChannel(token)).get("id");
        String uri = "/api/channel/" + channelId + "/upload/video";

        super.registerWithEmailAndUsername(
                        "randomUserVideo@upload.com",
                        "randomUserVideo");

        String randomUserToken = super.getTokenWithUsername("randomUserVideo");

        final MockMultipartFile videoFile = new MockMultipartFile("file",
                "test.mp4",
                "video/mp4",
                "test video".getBytes());

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders
                .multipart(uri)
                .file(videoFile)
                .headers(this.getHttpHeaders(randomUserToken))
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andReturn();

        int status = mvcResult.getResponse().getStatus();

        assertEquals(400, status);
    }

    @Test
    public void videoLoadsById() throws Exception {
        super.register();
        String token = super.getToken();
        int channelId = (int)super.mapFromJson(super.createChannel(token)).get("id");

        MvcResult uploadedVideo = super
                .uploadVideoWithUriAndToken("/api/channel/" + channelId + "/upload/video", token);
        String uri = "/api/video/" + (int)mapFromJson(uploadedVideo).get("id");

        MvcResult mvcResult = super.getRequest(uri, token);
        int status = mvcResult.getResponse().getStatus();

        assertEquals(200, status);
    }

    @Test
    public void likeVideo() throws Exception {
        super.register();
        String token = super.getToken();
        int channelId = (int)super.mapFromJson(super.createChannel(token)).get("id");

        MvcResult uploadedVideo = super
                .uploadVideoWithUriAndToken("/api/channel/" + channelId + "/upload/video", token);
        String uri = "/api/video/" + mapFromJson(uploadedVideo).get("id") + "/like";

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders
                .post(uri)
                .headers(this.getHttpHeaders(token)))
                .andReturn();

        int status = mvcResult.getResponse().getStatus();
        ArrayList<Object> likeList = super.getLongArrayByKey(super.mapFromJson(mvcResult), "likes");

        assertEquals(200, status);
        assertNotEquals(0, likeList.size());

        mvcResult = mvc.perform(MockMvcRequestBuilders
                .post(uri)
                .headers(this.getHttpHeaders(token)))
                .andReturn();

        status = mvcResult.getResponse().getStatus();
        likeList = super.getLongArrayByKey(super.mapFromJson(mvcResult), "likes");

        assertEquals(200, status);
        assertEquals(0, likeList.size());
    }
}
