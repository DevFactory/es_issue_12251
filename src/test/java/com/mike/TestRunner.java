package com.mike;

import java.io.IOException;

import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugin.analysis.icu.AnalysisICUPlugin;
import org.elasticsearch.test.ElasticsearchIntegrationTest;
import org.elasticsearch.test.ElasticsearchIntegrationTest.ClusterScope;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import com.carrotsearch.randomizedtesting.RandomizedContext;
import com.carrotsearch.randomizedtesting.annotations.Seed;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakFilters;

@ClusterScope(scope=ElasticsearchIntegrationTest.Scope.SUITE, numClientNodes = 1, numDataNodes = 1, transportClientRatio = 0)
@ThreadLeakFilters(defaultFilters = true, filters = {TestRunnerThreadsFilter.class})
//@Seed("635FC3A3BF78BA08:9E3727237FC39C12")  // fails
@Seed("1C45D45252C1042C")  // pass
public class TestRunner extends ElasticsearchIntegrationTest {

	// protected so that it is inherited by sub test classes
	protected static final String DEFAULT_INDEX = "test";

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	static {
		ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(true);
	}
	
	@Override
	protected Settings nodeSettings(int nodeOrdinal) {

		// Must explicitly set recycler!! Randomizer may select NONE, which is BAD
		Settings settings = ImmutableSettings.settingsBuilder()
				.put(super.nodeSettings(nodeOrdinal))
				.put("path.data", folder.getRoot())
				.put("http.enabled", true)
				.put("discovery.zen.ping.multicast.enabled", "false")
				.put("cache.recycler.page.type", "CONCURRENT")
				.put("type", "SOFT_CONCURRENT")
				.put("index.number_of_replicas", 0)
				// Don't specify shards in tests, according to: https://www.elastic.co/guide/en/elasticsearch/reference/current/integration-tests.html
				// .put("index.number_of_shards", 1)
				.put("threadpool.index.type", "cached")
				.put("threadpool.warmer.type", "cached")
				.put("threadpool.suggest.type", "cached")
				.put("threadpool.optimize.type", "fixed")
				.put("threadpool.flush.type", "fixed")
				.put("search.default_keep_alive", "1.7m")
				.put("search.keep_alive_interval", "51s")
				.put("cluster.routing.schedule", "66ms")
				.put("plugin.types", AnalysisICUPlugin.class.getName())
				.build();
		return settings;
	}

	@Override
	public Settings indexSettings() {
		Settings settings = ImmutableSettings.settingsBuilder()
				// we DO specify shards here otherwise risk OOM during junit run
				.put("index.number_of_shards", 1)
				.put("index.number_of_replicas", 0)
				.build();
		return settings;
	}

	@BeforeClass
	public static synchronized void setupTestEnvironment() throws Exception {
		beforeClass();
	}

	@AfterClass
	public static synchronized void cleanupTestEnvironment() {
	}

	@Before
	public void setup() throws IOException {

		System.out.println("MIKE SEED: " + RandomizedContext.current().getRunnerSeedAsString());

		// we have to create the index here so ElasticsearchIntegrationTest calls our overridden indexSettings()
		createIndex(DEFAULT_INDEX);
		ensureGreen();
		// add mappings:
		IndexManager.setClient(cluster().client(), DEFAULT_INDEX);
	}
	
	@After
	public void cleanup() {
	}

}
