package com.shoppilot.shoppilot;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class ShoppilotApplicationTests {

	@Autowired
	private MongoTemplate mongoTemplate;

	@Test
	void contextLoads() {
		assertNotNull(mongoTemplate);
		Document pingResult = mongoTemplate.executeCommand(new Document("ping", 1));
		assertEquals(1, ((Number) pingResult.get("ok")).intValue());
	}

}

