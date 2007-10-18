/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2005, 2006 The Sakai Foundation.
 * 
 * Licensed under the Educational Community License, Version 1.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 *      http://www.opensource.org/licenses/ecl1.php
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 *
 **********************************************************************************/

package org.sakaiproject.memory.api;

import java.util.Collection;

/**
 * MultiRefCache is a cache that holds objects and a set of references that the cached entry are dependent on - if any change, the entry is invalidated.
 * @deprecated This is no longer supported, invalidate the entries in your own code instead, 
 * stop using this immediately as it will be deprecated shortly 07/Oct/2007 -AZ
 */
public interface MultiRefCache extends Cache
{
	/**
	 * Cache an object
	 * 
	 * @param key
	 *        The key with which to find the object.
	 * @param payload
	 *        The object to cache.
	 * @param duration
	 *        The time to cache the object (seconds).
	 * @param ref
	 *        One entity reference that, if changed, will invalidate this entry.
	 * @param azgIds
	 *        AuthzGroup ids that, if the changed, will invalidate this entry.
	 * @deprecated no longer supported, invalidate the entries in your own code instead
	 */
	void put(String key, Object payload, int duration, String ref, Collection azgIds);
}
