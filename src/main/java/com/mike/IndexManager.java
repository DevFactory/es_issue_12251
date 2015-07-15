package com.mike;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.util.ArrayList;

import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.hppc.cursors.ObjectCursor;
import org.elasticsearch.node.Node;

public class IndexManager {

	static private Client client = null;
	static private Node node = null;

	protected static synchronized void initializeIndex(Client client, String index){

		// Get cluster name or use default
		String clusterName = "my_cluster";

		// Create the node object (production)
		if (node == null) {
			node = nodeBuilder()
					.clusterName(clusterName)
					.node();
		}

		if (client == null) {
			client = node.client();
		}
		
		// Wait for the node to complete initialization
		client.admin().cluster().prepareHealth().setWaitForYellowStatus().execute().actionGet();

		boolean indexExists = client.admin().indices().prepareExists(index).execute().actionGet().isExists();
		
		// Create the index if it doesn't exist
		if(!indexExists){
			try {
				client.admin().indices().prepareCreate(index).execute().get();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			
			// Wait for all updates to complete before proceeding
			client.admin().cluster().prepareHealth().setWaitForYellowStatus().execute().actionGet();
		}
		
		// Apply mappings for the default tenant
		addMappings(client, index);

		// Wait for the client to complete initialization
		client.admin().cluster().prepareHealth().setWaitForYellowStatus().execute().actionGet();
	}

	protected static synchronized void addMappings(Client client, String index) {

		client.admin().indices().preparePutMapping(index)
		.setType("logs")
		.setSource("{\"_source\": {\"compress\": false},\"_all\": {\"enabled\": \"true\"},\"dynamic\": \"strict\",\"properties\": {\"appID\": {\"index\": \"not_analyzed\",\"type\": \"string\",\"doc_values\": true}}}}")		
		.execute()
		.actionGet();

		client.admin().cluster().prepareHealth().setWaitForYellowStatus().execute().actionGet();
	}
	
	public static ArrayList<String> getMappingsInIndex(Client client, String index){
		// Get all existing mappings
		ClusterStateResponse mappingExistsResp = client.admin().cluster().prepareState().execute().actionGet();
		ImmutableOpenMap<String, MappingMetaData> mappings = mappingExistsResp.getState().metaData().index(index).mappings();
		
		ArrayList<String> mappingList = new ArrayList<String>();
		for(ObjectCursor<String> mappingObject: mappings.keys()){
			mappingList.add(mappingObject.value);
		}
		
		return mappingList;
	}
	
	/**
	 * Primarily for testing
	 */
	protected static synchronized void setClient(Client newClient, String index){
		client = newClient;
		initializeIndex(client, index);
	}


}
