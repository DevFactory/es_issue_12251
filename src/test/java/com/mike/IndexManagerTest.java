package com.mike;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.elasticsearch.client.Client;
import org.elasticsearch.test.ElasticsearchIntegrationTest;
import org.junit.Test;

public class IndexManagerTest extends TestRunner {
	
	@Test
	public void testAddMappings() throws IOException {
		String index = "testindex";
		Client client = ElasticsearchIntegrationTest.client();
		
		IndexManager.initializeIndex(client, index);
		IndexManager.addMappings(client, index);
		
		boolean indexExists = client.admin().indices().prepareExists(index).execute().actionGet().isExists();
		assertTrue(indexExists);
		
		List<String> mappings = Arrays.asList(new String[]{"logs"});
		ArrayList<String> addedMappings = IndexManager.getMappingsInIndex(client, index);
		
		for(String mapping: addedMappings){
			if(!mappings.contains(mapping)){
				fail("The following mapping was not added: " + mapping);
			}
		}
		
		assertEquals(mappings.size(), addedMappings.size());
	}
		
}
