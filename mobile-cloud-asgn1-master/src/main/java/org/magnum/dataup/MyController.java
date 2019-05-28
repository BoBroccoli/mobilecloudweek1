/*
 * 
 * Copyright 2014 Jules White
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package org.magnum.dataup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.magnum.dataup.model.VideoStatus.VideoState;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class MyController {

	private static final AtomicLong currentId = new AtomicLong(0L);

	private Map<Long, Video> videos = new HashMap<Long, Video>();
	
	private List<Video> videoList = new ArrayList<>();

	private void checkAndSetId(Video entity) {
		if(entity.getId() == 0){
			entity.setId(currentId.incrementAndGet());
		}
	}
	
	private String getDataUrl(long videoId){
        String url = getUrlBaseForLocalServer() + "/video/" + videoId + "/data";
        return url;
    }

 	private String getUrlBaseForLocalServer() {
	   HttpServletRequest request = 
	       ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
	   String base = 
	      "http://"+request.getServerName() 
	      + ((request.getServerPort() != 80) ? ":"+request.getServerPort() : "");
	   return base;
	}
 	
	@ResponseBody
	@RequestMapping(value = "/video", method = RequestMethod.POST)
	public Video addVideoMetaData(@RequestBody Video videoMeta) {
		Video video = Video.create().withContentType(videoMeta.getContentType())
				.withDuration(videoMeta.getDuration()).withSubject(videoMeta.getSubject())
				.withTitle(videoMeta.getTitle()).build();
		checkAndSetId(video);
		video.setDataUrl(getDataUrl(video.getId()));
		videos.put(video.getId(), video);
		videoList.add(video);
		return video;
	}
	@ResponseBody
	@RequestMapping(value = "/video", method = RequestMethod.GET)
	public List<Video> getVideoList(){
		return videoList;
	}
	
	@ResponseBody
	@RequestMapping(value = "/video/{id}/data", method = RequestMethod.POST)
	public VideoStatus setMultipartData(@PathVariable long id, @RequestParam("data") MultipartFile videoData) {
		if(!videos.containsKey(id))
			throw new ResourceNotFoundException();
		try {
			InputStream inputStream = videoData.getInputStream();
			File file = new File("src/test/resources/uploaded.mp4");
			FileUtils.copyInputStreamToFile(inputStream, file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		VideoStatus videoStatus = new VideoStatus(VideoState.READY);
		return videoStatus;
	}
	
	@ResponseBody
	@RequestMapping(value = "/video/{id}/data", method = RequestMethod.GET)
	public byte[] getData(@PathVariable("id") long id) {
		if(!videos.containsKey(id))
			throw new ResourceNotFoundException();
		File file = new File("src/test/resources/uploaded.mp4");
		InputStream inputStream = null;
		try {
			inputStream = new FileInputStream(file);
			return IOUtils.toByteArray(inputStream);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
