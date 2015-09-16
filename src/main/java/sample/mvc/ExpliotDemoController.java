/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sample.mvc;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import sample.data.Message;
import sample.data.MessageRepository;

@Controller
public class ExpliotDemoController {
	private final MessageRepository messageRepository;
	private final JsonMessageParser messageParser;

	@Autowired
	public ExpliotDemoController(MessageRepository messageRepository, JsonMessageParser messageParser) {
		super();
		this.messageRepository = messageRepository;
		this.messageParser = messageParser;
	}

	@RequestMapping(value = "/csrf/messages/", method = RequestMethod.POST)
	public void exploit(HttpServletRequest request, HttpServletResponse response) throws Exception {
		Message messageToSave = messageParser.parse(request.getInputStream());

		messageRepository.save(messageToSave);

		response.sendRedirect(request.getContextPath());
	}

	@RequestMapping("/xss/")
	public String xssIndex() {
		return "xss/index";
	}

	@RequestMapping("/xss/reflected")
	public String reflected(@RequestParam String message, Map<String,Object> model) {
		model.put("message",message);
		return "xss/reflected";
	}

	@RequestMapping("/xss/persistent")
	public String persistent(@RequestParam(defaultValue = "120") Long id, Map<String, Object> model) {
		Message message = messageRepository.findOne(id);
		model.put("message", message);
		return "xss/persistent";
	}

	@RequestMapping("/xss/fix")
	public String fixxss(@RequestParam(defaultValue = "120") Long id, Map<String, Object> model) {
		Message message = messageRepository.findOne(id);
		model.put("message", message);
		return "xss/fix";
	}
}
