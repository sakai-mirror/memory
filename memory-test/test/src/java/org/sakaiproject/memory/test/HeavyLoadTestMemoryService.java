/**
 * HeavyLoadTestMemoryService.java - memory-test - 2007 Oct 29, 2007 5:03:06 PM - azeckoski
 */

package org.sakaiproject.memory.test;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.memory.api.Cache;
import org.sakaiproject.memory.api.MemoryService;
import org.sakaiproject.testrunner.utils.SpringTestCase;
import org.sakaiproject.testrunner.utils.annotations.Autowired;
import org.sakaiproject.testrunner.utils.annotations.Resource;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.user.api.UserDirectoryService;


/**
 * This test is primarily for experimenting with settings and seeing the differences between
 * in-memory and disk caches, it also let's us understand the costs associated with 
 * cache misses and mixed caches
 * 
 * @author Aaron Zeckoski (aaron@caret.cam.ac.uk)
 */
public class HeavyLoadTestMemoryService extends SpringTestCase {

   private static Log log = LogFactory.getLog(HeavyLoadTestMemoryService.class);

   private MemoryService memoryService;
   @Autowired
   public void setMemoryService(MemoryService memoryService) {
      this.memoryService = memoryService;
   }

   private SessionManager sessionManager;
   @Resource(name="org.sakaiproject.tool.api.SessionManager")
   public void setSessionManager(SessionManager sessionManager) {
      this.sessionManager = sessionManager;
   }

   private UserDirectoryService userDirectoryService;
   @Autowired
   public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
      this.userDirectoryService = userDirectoryService;
   }

   protected DecimalFormat df = new DecimalFormat("#,##0.00");
   protected final String defaultCacheName = "org.sakaiproject.memory.cache.Default";
   protected final String memOnlyCacheName = "org.sakaiproject.memory.cache.MemOnly";
   protected final String diskOnlyCacheName = "org.sakaiproject.memory.cache.DiskOnly";
   protected final String testPayload = 
      "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
      "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
      "RRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRR" +
      "OOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO" +
      "NNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNN" +
      "ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ" +
      "EEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE" +
      "CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC" +
      "KKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKK" +
      "OOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO" +
      "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
      "KKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKK" +
      "IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII";
   protected final String INSERT = "insert";
   protected final String REMOVE = "remove";
   protected final String GET = "get";
   protected final String RESET = "reset";


   @Override
   protected void setUp() throws Exception {
      super.setUp();
   };

   public void testCanGetSakaiBeans() {
      assertNotNull(memoryService);
      assertNotNull(userDirectoryService);
      assertNotNull(sessionManager);
   }

   public void testMemoryServiceBasicLoad() {
      final int itemCount = 10000;
      final int accessMultiplier = 100; // this is the number of times each item will be retrieved

      log.info("LOAD testing MEMORY ONLY cache (items="+itemCount+", accessMultiplier="+accessMultiplier+")");
      runBasicLoadTest(memOnlyCacheName, itemCount, accessMultiplier);

      log.info("LOAD testing DISK ONLY cache (items="+itemCount+", accessMultiplier="+accessMultiplier+")");
      runBasicLoadTest(diskOnlyCacheName, itemCount, accessMultiplier);

      log.info("LOAD testing DEFAULT (MEM + DISK) cache (items="+itemCount+", accessMultiplier="+accessMultiplier+")");
      runBasicLoadTest(defaultCacheName, itemCount, accessMultiplier);

   }

   public void testMemoryServiceHeavyLoad() {
      final int itemCount = 100000;
      final int accessMultiplier = 200; // this is the number of times each item will be retrieved

      log.info("Heavy LOAD testing MEMORY ONLY cache (items="+itemCount+", accessMultiplier="+accessMultiplier+")");
      runBasicLoadTest(memOnlyCacheName, itemCount, accessMultiplier);

      log.info("Heavy LOAD testing DISK ONLY cache (items="+itemCount+", accessMultiplier="+accessMultiplier+")");
      runBasicLoadTest(diskOnlyCacheName, itemCount, accessMultiplier);

      log.info("Heavy LOAD testing DEFAULT (MEM + DISK) cache (items="+itemCount+", accessMultiplier="+accessMultiplier+")");
      runBasicLoadTest(defaultCacheName, itemCount, accessMultiplier);

   }

   /**
    * Simulate cache activity and return stats
    * @param cacheName the name of the cache to use
    * @param itemCount number of items to put in the cache
    * @param accessMultiplier number of times to retrieve each item
    */
   private Map<String, Long> runBasicLoadTest(String cacheName, int itemCount, int accessMultiplier) {
      long start = 0;
      long total = 0;
      Map<String, Long> results = new HashMap<String, Long>();

      // generate a test cache
      Cache testCache = memoryService.newCache(cacheName);
      testCache.resetCache();

      // test the insertion timing for mass items
      Set<String> cacheKeys = new HashSet<String>(); // using this set to randomize the list of keys
      start = System.currentTimeMillis();
      for (int i = 0; i < itemCount; i++) {
         String key = "KEY" + i;
         testCache.put(key,  "Number=" + i + ": " + testPayload);
         cacheKeys.add(key);
      }
      total = System.currentTimeMillis() - start;
      results.put(INSERT, total);
      log.info("Completed INSERT of "+itemCount+" items into the cache in "+total
            +" ms ("+calcUSecsPerOp(itemCount, total)+" microsecs per operation)," +
            		" Cache size is now " + testCache.getSize());

      // test removing mass cached items timing
      start = System.currentTimeMillis();
      for (String key : cacheKeys) {
         testCache.remove(key);
      }
      total = System.currentTimeMillis() - start;
      results.put(REMOVE, total);
      log.info("Completed REMOVAL of "+itemCount+" items from the cache in "+total
            +" ms ("+calcUSecsPerOp(itemCount, total)+" microsecs per operation)," +
                  " Cache size is now " + testCache.getSize());

      // clear
      testCache.resetCache();
      cacheKeys.clear();

      // repopulate the cache
      for (int i = 0; i < itemCount; i++) {
         String key = "AKEY" + i;
         testCache.put(key,  "Number=" + i + ": " + testPayload);
         cacheKeys.add(key);
      }
      long missCount = 0;
      long hitCount = 0;
      // test accessing mass cached items timing
      start = System.currentTimeMillis();
      for (int j = 0; j < accessMultiplier; j++) {
         for (String key : cacheKeys) {
            if (testCache.get(key) == null) {
               missCount++;
            } else {
               hitCount++;
            }            
         }
      }
      total = System.currentTimeMillis() - start;
      results.put(GET, total);
      log.info("Completed GET of "+itemCount+" items from the cache "+accessMultiplier
            +" times each ("+(itemCount*accessMultiplier)+" total gets) in "+total
            +" ms ("+calcUSecsPerOp(itemCount*accessMultiplier, total)+" microsecs per operation)," +
            		" Cache hits: " + hitCount + ", misses: " + missCount);
      log.info("STATS: " + testCache.getDescription());

      // test the reset timing
      start = System.currentTimeMillis();
      testCache.resetCache();
      total = System.currentTimeMillis() - start;
      results.put(RESET, total);
      log.info("Completed reset of the cache in "+total+" ms");

      log.info("SUMMARY:: insert: " + results.get(INSERT) + " ms, removal: " + results.get(REMOVE) + " ms, " +
      		"get: " + results.get(GET) + " ms, reset: " + results.get(RESET) + " ms");

      return results;
   }

}

