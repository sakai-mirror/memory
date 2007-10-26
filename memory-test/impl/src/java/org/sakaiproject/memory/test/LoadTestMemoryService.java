/**
 * LoadTestMemoryService.java - memory-test - 2007 Oct 24, 2007 5:03:06 PM - azeckoski
 */

package org.sakaiproject.memory.test;

import java.text.DecimalFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.sakaiproject.memory.api.MemoryService;
import org.sakaiproject.testrunner.utils.SpringTestCase;
import org.sakaiproject.testrunner.utils.annotations.Autowired;
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
public class LoadTestMemoryService extends SpringTestCase {

      private static Log log = LogFactory.getLog(LoadTestMemoryService.class);

      private MemoryService memoryService;
      @Autowired
      public void setMemoryService(MemoryService memoryService) {
         this.memoryService = memoryService;
      }

      private SessionManager sessionManager;
      @Autowired
      public void setSessionManager(SessionManager sessionManager) {
         this.sessionManager = sessionManager;
      }

      private UserDirectoryService userDirectoryService;
      @Autowired
      public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
         this.userDirectoryService = userDirectoryService;
      }
      

      @Override
      protected void setUp() throws Exception {
         super.setUp();
      };

      public void testCanGetSakaiBeans() {
         assertNotNull(memoryService);
         assertNotNull(userDirectoryService);
         assertNotNull(sessionManager);
      }

      public void testMemoryServiceLoad() {
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
               User testUser = userDirectoryService.getUserByEid( USER_PREFIX + i );
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
         log.info("Created new fake users ("+newUsers+") for lookup in the total group of " + userCount 
               + ", total processing time of "+userMakeTime+" ms");


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
         double invalidMicro = ((double)(invalidLookupTime * 1000))/((double)invalidLoops);
         log.info("Attempted to lookup " + userCount + " invalid users " 
               + invalidLoops + " times in " + invalidLookupTime + " ms, "
               + df.format(invalidMicro) + " microsecs per user");


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
         double validMicro = ((double)(validLookupTime * 1000))/((double)validLoops);
         log.info("Attempted to lookup " + userCount + " valid users " 
               + validLoops + " times in " + validLookupTime + " ms, "
               + df.format(validMicro) + " microsecs per user");


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
         log.info("Removed fake users ("+removedUsers+") from the total group of " + userCount 
               + ", total processing time of "+userDestroyTime+" ms");


         long totaltime = System.currentTimeMillis() - starttime;
//       microtime = ((double)(totaltime * 1000))/((double)validLoops+invalidLoops);
         log.info("Test completed in "+totaltime+" ms");

         // switch user back
         currentSession.setUserId(currentUserId);
      }
   }

