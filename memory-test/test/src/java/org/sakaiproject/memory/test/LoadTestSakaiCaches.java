/**
 * LoadTestMemoryService.java - memory-test - 2007 Oct 24, 2007 5:03:06 PM - azeckoski
 */

package org.sakaiproject.memory.test;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.authz.api.AuthzGroupService;
import org.sakaiproject.memory.api.Cache;
import org.sakaiproject.memory.api.MemoryService;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.testrunner.utils.SpringTestCase;
import org.sakaiproject.testrunner.utils.annotations.Autowired;
import org.sakaiproject.testrunner.utils.annotations.Resource;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserAlreadyDefinedException;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserEdit;
import org.sakaiproject.user.api.UserIdInvalidException;
import org.sakaiproject.user.api.UserLockedException;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.user.api.UserPermissionException;


/**
 * This is a test to load up the memory service and then see how it performs under load
 * 
 * @author Aaron Zeckoski (aaron@caret.cam.ac.uk)
 */
public class LoadTestSakaiCaches extends SpringTestCase {

   private static Log log = LogFactory.getLog(LoadTestSakaiCaches.class);

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

   private AuthzGroupService authzGroupService;
   @Autowired
   public void setAuthzGroupService(AuthzGroupService authzGroupService) {
      this.authzGroupService = authzGroupService;
   }

   private SiteService siteService;
   @Autowired
   public void setSiteService(SiteService siteService) {
      this.siteService = siteService;
   }

   private UserDirectoryService userDirectoryService;
   @Autowired
   public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
      this.userDirectoryService = userDirectoryService;
   }


   /**
    * Number of total thread simulation iterations to run
    */
   protected final int iterations = 1000000;
   /**
    * This is the maxsize of the cache being tested
    */
   protected final long maxCacheSize = 10000;

   protected DecimalFormat df = new DecimalFormat("#,##0.00");
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


   @Override
   protected void setUp() throws Exception {
      super.setUp();
   };

   public void testCanGetSakaiBeans() {
      assertNotNull(memoryService);
      assertNotNull(sessionManager);
      assertNotNull(authzGroupService);
      assertNotNull(siteService);
      assertNotNull(userDirectoryService);
   }

   public void testUserCache() {
      final String USER_PREFIX = "fakeuser";
      Random rGen = new Random();

      // switch to the admin to run this
      Session currentSession = sessionManager.getCurrentSession();
      String currentUserId = null;
      if (currentSession != null) {
         currentUserId = currentSession.getUserId();
         currentSession.setUserId("admin");
      } else {
         throw new RuntimeException("no CurrentSession, cannot set to admin user");
      }

      // output the current state of the cache
      memoryService.getStatus();
      
      final int userCount = userDirectoryService.countUsers();
      String[] validUsers = null;
      if (userCount < 1000) {
         log.info("Not enough real users in the system ("+userCount+"), we have to make 1000 fake ones...");
         validUsers = new String[userCount+1000];
         // put the current users at the end
         List<User> allUsers = userDirectoryService.getUsers();
         for (int i = 0; i < allUsers.size(); i++) {
            validUsers[i+1000] = allUsers.get(i).getId();
         }
         // insert all the fake users (these are simple users but they have a full user record)         
         int newUsers = 0;
         for (int i = 0; i < 1000; i++) {
            try {
               User testUser = userDirectoryService.getUserByEid(USER_PREFIX + i);
               if (testUser == null) {
                  throw new UserNotDefinedException("User does not exist");
               } else {
                  validUsers[i] = testUser.getId();
               }
            } catch (UserNotDefinedException e1) {
               // this is ok, create new user
               try {
                  UserEdit user = userDirectoryService.addUser(null, USER_PREFIX + i);
                  userDirectoryService.commitEdit(user);
                  validUsers[i] = user.getId();
                  newUsers++;
               } catch (UserIdInvalidException e) {
                  throw new RuntimeException("Failure in user creation", e);
               } catch (UserAlreadyDefinedException e) {
                  log.warn("User already exists: " + USER_PREFIX + i);
               } catch (UserPermissionException e) {
                  throw new RuntimeException("Failure in user creation", e);
               }
            }
         }
      } else {
         validUsers = new String[userCount];
         // put all the current users in the list (this should prime the cache)
         List<User> allUsers = userDirectoryService.getUsers();
         for (int i = 0; i < allUsers.size(); i++) {
            validUsers[i] = allUsers.get(i).getId();
         }
      }

      // output the current state of the cache
      memoryService.getStatus();

      log.info("Starting user cache performance test with "+validUsers.length+" users...");

      long start = System.currentTimeMillis();
      for (int i = 0; i < iterations; i++) {
         // pick out and fetch a random user
         String userId = validUsers[rGen.nextInt(validUsers.length)];;
         User user = null;
         try {
            user = userDirectoryService.getUser(userId);
         } catch (UserNotDefinedException e) {
            fail("Should not have gotten here");
         }
         assertNotNull(user);
      }
      long total = System.currentTimeMillis() - start;
      log.info("Completed "+iterations+" iterations in "+total+" ms ("+calcUSecsPerOp(iterations, total)+" microsecs per iteration)");

      // output the current state of the cache
      memoryService.getStatus();

      // destroy all the fake users
      if (userCount < 1000) {
         int removedUsers = 0;
         for (int i = 0; i < 1000; i++) {
            try {
               UserEdit user = userDirectoryService.editUser(validUsers[i]);
               userDirectoryService.removeUser(user);
               removedUsers++;
            } catch (UserPermissionException e) {
               throw new RuntimeException("Failure in user removal", e);
            } catch (UserLockedException e) {
               throw new RuntimeException("Failure in user removal", e);
            } catch (UserNotDefinedException e) {
               // fine
               log.warn("Cound not remove user: " + validUsers[i]);
            }
         }
         log.info("Removed fake users (" + removedUsers + ") from the total group of " + validUsers.length);
      }

      // switch user back
      currentSession.setUserId(currentUserId);
   }

   /**
    * This test is meant to test the 3 top hit caches in Sakai 
    * (User, Authz, Site) and see how they perform,
    * it is attempting to simulate realistic load on the caches
    */
/**
   public void testSimulatedMultiCacheSingleThread() {
      final String USER_PREFIX = "fakeuser";
      final String INVALID_PREFIX = "invaliduser";
      final int userCount = 1000;
      final long invalidLoops = 100000;
      final long validLoops = 100000;
      DecimalFormat df = new DecimalFormat("#,##0.00");

      // switch to the admin to run this
      Session currentSession = sessionManager.getCurrentSession();
      String currentUserId = null;
      if (currentSession != null) {
         currentUserId = currentSession.getUserId();
         currentSession.setUserId("admin");
      } else {
         throw new RuntimeException("no CurrentSession, cannot set to admin user");
      }

      // make up invalid user ids
      String[] invalidUsers = new String[userCount];
      for (int i = 0; i < userCount; i++) {
         invalidUsers[i] = INVALID_PREFIX + i;
      }

      log.info("Starting large scale user performance test...");
      long starttime = System.currentTimeMillis();

      // insert all the fake users first (these are simple users but they have a full user record)
      long userMakeStart = System.currentTimeMillis();
      int newUsers = 0;
      String[] validUsers = new String[userCount];
      for (int i = 0; i < userCount; i++) {
         try {
            User testUser = userDirectoryService.getUserByEid(USER_PREFIX + i);
            if (testUser == null) {
               throw new UserNotDefinedException("User does not exist");
            } else {
               validUsers[i] = testUser.getId();
            }
         } catch (UserNotDefinedException e1) {
            // this is ok, create new user
            try {
               UserEdit user = userDirectoryService.addUser(null, USER_PREFIX + i);
               userDirectoryService.commitEdit(user);
               validUsers[i] = user.getId();
               newUsers++;
            } catch (UserIdInvalidException e) {
               throw new RuntimeException("Failure in user creation", e);
            } catch (UserAlreadyDefinedException e) {
               log.warn("User already exists: " + USER_PREFIX + i);
            } catch (UserPermissionException e) {
               throw new RuntimeException("Failure in user creation", e);
            }
         }
      }
      long userMakeTime = System.currentTimeMillis() - userMakeStart;
      log.info("Created new fake users (" + newUsers + ") for lookup in the total group of "
            + userCount + ", total processing time of " + userMakeTime + " ms");

      long invalidLookupStart = System.currentTimeMillis();
      for (int i = 0; i < invalidLoops; i++) {
         String userId = invalidUsers[i % userCount];
         User user = null;
         try {
            user = userDirectoryService.getUser(userId);
            fail("Should not have gotten here");
         } catch (UserNotDefinedException e) {
            // this is correct
            assertNotNull(e);
         }
         assertNull(user);
      }
      long invalidLookupTime = System.currentTimeMillis() - invalidLookupStart;
      double invalidMicro = ((double) (invalidLookupTime * 1000)) / ((double) invalidLoops);
      log.info("Attempted to lookup " + userCount + " invalid users " + invalidLoops + " times in "
            + invalidLookupTime + " ms, " + df.format(invalidMicro) + " microsecs per user");

      long validLookupStart = System.currentTimeMillis();
      for (int i = 0; i < validLoops; i++) {
         String userId = validUsers[i % userCount];
         User user = null;
         try {
            user = userDirectoryService.getUser(userId);
         } catch (UserNotDefinedException e) {
            fail("Should not have gotten here");
         }
         assertNotNull(user);
      }
      long validLookupTime = System.currentTimeMillis() - validLookupStart;
      double validMicro = ((double) (validLookupTime * 1000)) / ((double) validLoops);
      log.info("Attempted to lookup " + userCount + " valid users " + validLoops + " times in "
            + validLookupTime + " ms, " + df.format(validMicro) + " microsecs per user");

      // destroy all the fake users
      long userDestroyStart = System.currentTimeMillis();
      int removedUsers = 0;
      for (int i = 0; i < validUsers.length; i++) {
         try {
            UserEdit user = userDirectoryService.editUser(validUsers[i]);
            userDirectoryService.removeUser(user);
            removedUsers++;
         } catch (UserPermissionException e) {
            throw new RuntimeException("Failure in user removal", e);
         } catch (UserLockedException e) {
            throw new RuntimeException("Failure in user removal", e);
         } catch (UserNotDefinedException e) {
            // fine
            log.warn("Cound not remove user: " + validUsers[i]);
         }
      }
      long userDestroyTime = System.currentTimeMillis() - userDestroyStart;
      log.info("Removed fake users (" + removedUsers + ") from the total group of " + userCount
            + ", total processing time of " + userDestroyTime + " ms");

      long totaltime = System.currentTimeMillis() - starttime;
      //    microtime = ((double)(totaltime * 1000))/((double)validLoops+invalidLoops);
      log.info("Test completed in " + totaltime + " ms");

      // switch user back
      currentSession.setUserId(currentUserId);
   }


   public void testSimulatedSingleCacheMultiThread() {
      final int threads = 30;
      final int threadIterations = iterations / threads;

      // generate a test cache
      final Cache testCache = memoryService.newCache("AZ-MULTITHREAD-SIMULATION");
      testCache.resetCache();

      log.info("Starting concurrent caching load test with "+threads+" threads...");
      long start = System.currentTimeMillis();
      for (int t = 0; t < threads; t++) {
         final int threadnum = t+1;
         Thread thread = new Thread( new Runnable() {
            public void run() {
               runCacheTestThread(threadnum, threads, testCache, threadIterations, maxCacheSize);
            }
         }, threadnum+"");
         thread.start();
      }
      startThreadMonitor();
      long total = System.currentTimeMillis() - start;
      log.info(threads + " threads completed "+iterations+" iterations in "
            +total+" ms ("+calcUSecsPerOp(iterations, total)+" microsecs per iteration)");
      log.info("STATS: " + testCache.getDescription());
   }
**/

   /**
    * @param threadnum
    * @param threads
    * @param testCache
    * @param iterations
    * @param maxCacheSize
    */
   private void runCacheTestThread(int threadnum, int threads, Cache testCache, final int iterations,
         long maxCacheSize) {
      long missCount = 0;
      long hitCount = 0;
      long readCount = 0;
      int insertCount = 0;
      int deleteCount = 0;
      Random rGen = new Random();
      String keyPrefix = "key-" + threadnum + "-";
      monitoringThread();
      long start = System.currentTimeMillis();
      for (int i = 0; i < iterations; i++) {
         int random = rGen.nextInt(100);
         if ( (i < 100 || random >= 95) && ((insertCount*threads) < maxCacheSize) ) {
            int num = insertCount++;
            testCache.put(keyPrefix + num, "Number=" + num + ": " + testPayload);
         }
         if (i > 2) {
            // do 10 reads from this threads cache
            for (int j = 0; j < 10; j++) {
               readCount++;
               if (testCache.get(keyPrefix + rGen.nextInt(insertCount)) == null) {
                  missCount++;
               } else {
                  hitCount++;
               }
            }
            // do 5 more from a random threads cache
            String otherKeyPrefix = "key-" + (rGen.nextInt(threads)+1) + "-";
            for (int j = 0; j < 5; j++) {
               readCount++;
               if (testCache.get(otherKeyPrefix + rGen.nextInt(insertCount)) == null) {
                  missCount++;
               } else {
                  hitCount++;
               }
            }
         }
         if ( random < 1 && ((deleteCount*threads) < (maxCacheSize/8)) ) {
            testCache.remove(keyPrefix + rGen.nextInt(insertCount));
            deleteCount++;
         }
         if (i > 0 && i % (iterations/5) == 0) {
            monitoringThread();
            //log.info("thread: " + threadnum + " " + (i*100/iterations) + "% complete");
         }
      }
      long total = System.currentTimeMillis() - start;
      endMonitoringThread();
      final String hitPercentage = ((hitCount+missCount) > 0) ? ((100l * hitCount) / (hitCount + missCount)) + "%" : "N/A";
      log.info("Thread "+threadnum+": completed "+iterations+" iterations with "+insertCount+" inserts " +
            "and "+deleteCount+" removes and "+readCount+" reads " +
            "(hits: " + hitCount + ", misses: " + missCount + ", hit%: "+hitPercentage+") " +
            "in "+total+" ms ("+calcUSecsPerOp(iterations, total)+" microsecs per iteration)");
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

      log.info("SUMMARY:: insert: " + results.get(INSERT) + " ms, " +
            "removal: " + results.get(REMOVE) + " ms, " +
            "get: " + results.get(GET) + " ms");

      return results;
   }

}

