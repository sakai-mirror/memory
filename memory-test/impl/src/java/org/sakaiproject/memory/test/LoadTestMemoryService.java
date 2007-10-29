/**
 * LoadTestMemoryService.java - memory-test - 2007 Oct 24, 2007 5:03:06 PM - azeckoski
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
 * This is a test to load up the memory service and then see how it performs under load
 * 
 * @author Aaron Zeckoski (aaron@caret.cam.ac.uk)
 */
public class LoadTestMemoryService extends SpringTestCase {

   private static Log log = LogFactory.getLog(LoadTestMemoryService.class);

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

//   public void testMemoryServiceHeavyLoad() {
//      final int itemCount = 20000;
//      final int accessMultiplier = 100; // this is the number of times each item will be retrieved
//
//      log.info("Heavy LOAD testing MEMORY ONLY cache (items="+itemCount+", accessMultiplier="+accessMultiplier+")");
//      runBasicLoadTest(memOnlyCacheName, itemCount, accessMultiplier);
//
//      log.info("Heavy LOAD testing DISK ONLY cache (items="+itemCount+", accessMultiplier="+accessMultiplier+")");
//      runBasicLoadTest(diskOnlyCacheName, itemCount, accessMultiplier);
//
//      log.info("Heavy LOAD testing DEFAULT (MEM + DISK) cache (items="+itemCount+", accessMultiplier="+accessMultiplier+")");
//      runBasicLoadTest(defaultCacheName, itemCount, accessMultiplier);
//
//   }

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


   /**
    * @param loopCount total number of operations
    * @param totalMilliSecs total number of milliseconds
    * @return the number of microsecs per operation
    */
   private String calcUSecsPerOp(long loopCount, long totalMilliSecs) {
      return df.format(((double)(totalMilliSecs * 1000))/((double)loopCount));
   }


   public void testConcurrentMemoryServiceDefaultCache() {

   }

// public void testMemoryServiceUsage() {
// final String USER_PREFIX = "fakeuser";
// final String INVALID_PREFIX = "invaliduser";
// final int userCount = 1000;
// final long invalidLoops = 100000;
// final long validLoops = 100000;
// DecimalFormat df = new DecimalFormat("#,##0.00");

// // switch to the admin to run this
// Session currentSession = sessionManager.getCurrentSession();
// String currentUserId = null;
// if (currentSession != null) {
// currentUserId = currentSession.getUserId();
// currentSession.setUserId("admin");
// } else {
// throw new RuntimeException("no CurrentSession, cannot set to admin user");
// }

// // make up invalid user ids
// String[] invalidUsers = new String[userCount];
// for (int i = 0; i < userCount; i++) {
// invalidUsers[i] = INVALID_PREFIX + i;
// }

// log.info("Starting large scale user performance test...");
// long starttime = System.currentTimeMillis();

// // insert all the fake users first (these are simple users but they have a full user record)
// long userMakeStart = System.currentTimeMillis();
// int newUsers = 0;
// String[] validUsers = new String[userCount];
// for (int i = 0; i < userCount; i++) {
// try {
// User testUser = userDirectoryService.getUserByEid( USER_PREFIX + i );
// if (testUser == null) {
// throw new UserNotDefinedException("User does not exist");
// } else {
// validUsers[i] = testUser.getId();
// }
// } catch (UserNotDefinedException e1) {
// // this is ok, create new user
// try {
// UserEdit user = userDirectoryService.addUser(null, USER_PREFIX + i);
// userDirectoryService.commitEdit(user);
// validUsers[i] = user.getId();
// newUsers++;
// } catch (UserIdInvalidException e) {
// throw new RuntimeException("Failure in user creation", e);
// } catch (UserAlreadyDefinedException e) {
// log.warn("User already exists: " + USER_PREFIX + i);
// } catch (UserPermissionException e) {
// throw new RuntimeException("Failure in user creation", e);
// }
// }
// }
// long userMakeTime = System.currentTimeMillis() - userMakeStart;
// log.info("Created new fake users ("+newUsers+") for lookup in the total group of " + userCount 
// + ", total processing time of "+userMakeTime+" ms");


// long invalidLookupStart = System.currentTimeMillis();
// for (int i = 0; i < invalidLoops; i++) {
// String userId = invalidUsers[i % userCount];
// User user = null;
// try {
// user = userDirectoryService.getUser(userId);
// fail("Should not have gotten here");
// } catch (UserNotDefinedException e) {
// // this is correct
// assertNotNull(e);
// }
// assertNull(user);
// }
// long invalidLookupTime = System.currentTimeMillis() - invalidLookupStart;
// double invalidMicro = ((double)(invalidLookupTime * 1000))/((double)invalidLoops);
// log.info("Attempted to lookup " + userCount + " invalid users " 
// + invalidLoops + " times in " + invalidLookupTime + " ms, "
// + df.format(invalidMicro) + " microsecs per user");


// long validLookupStart = System.currentTimeMillis();
// for (int i = 0; i < validLoops; i++) {
// String userId = validUsers[i % userCount];
// User user = null;
// try {
// user = userDirectoryService.getUser(userId);
// } catch (UserNotDefinedException e) {
// fail("Should not have gotten here");
// }
// assertNotNull(user);
// }
// long validLookupTime = System.currentTimeMillis() - validLookupStart;
// double validMicro = ((double)(validLookupTime * 1000))/((double)validLoops);
// log.info("Attempted to lookup " + userCount + " valid users " 
// + validLoops + " times in " + validLookupTime + " ms, "
// + df.format(validMicro) + " microsecs per user");


// // destroy all the fake users
// long userDestroyStart = System.currentTimeMillis();
// int removedUsers = 0;
// for (int i = 0; i < validUsers.length; i++) {
// try {
// UserEdit user = userDirectoryService.editUser(validUsers[i]);
// userDirectoryService.removeUser(user);
// removedUsers++;
// } catch (UserPermissionException e) {
// throw new RuntimeException("Failure in user removal", e);
// } catch (UserLockedException e) {
// throw new RuntimeException("Failure in user removal", e);
// } catch (UserNotDefinedException e) {
// // fine
// log.warn("Cound not remove user: " + validUsers[i]);
// }
// }
// long userDestroyTime = System.currentTimeMillis() - userDestroyStart;
// log.info("Removed fake users ("+removedUsers+") from the total group of " + userCount 
// + ", total processing time of "+userDestroyTime+" ms");


// long totaltime = System.currentTimeMillis() - starttime;
////microtime = ((double)(totaltime * 1000))/((double)validLoops+invalidLoops);
// log.info("Test completed in "+totaltime+" ms");

// // switch user back
// currentSession.setUserId(currentUserId);
// }
}

