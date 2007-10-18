/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2003, 2004, 2005, 2006, 2007 The Sakai Foundation.
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

package org.sakaiproject.memory.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.distribution.ReplicationControl;

import org.sakaiproject.memory.api.MultipleReferenceCache;
import org.sakaiproject.memory.api.ObjectNotCachedException;

/**
 * A cache, where the cached object depends on this cache
 * 
 * @author ieb
 */
public class ObjectDependsOnOthersCache implements MultipleReferenceCache
{

	private Cache cache;

	private Map<Object, Map<Object, Object>> dependentKeyMap = new ConcurrentHashMap<Object, Map<Object, Object>>();

	/**
	 * If true, invalidation will navigate the tree
	 */
	private boolean recursive = false;

	/**
	 * If true, all cluster invalidation messages will be sent, if false, only
	 * the root of the invalidation will be sent
	 */
	private boolean propagateTree = false;

	/**
	 * 
	 */
	public ObjectDependsOnOthersCache()
	{
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sakaiproject.memory.api.MultipleReferenceCache#exists(java.lang.Object)
	 */
	public boolean exists(Object key)
	{
		return cache.isKeyInCache(key);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sakaiproject.memory.api.MultipleReferenceCache#get(java.lang.Object)
	 */
	public Object get(Object key) throws ObjectNotCachedException
	{
		Element e = cache.get(key);
		if (e == null)
		{
			throw new ObjectNotCachedException("Cache miss " + key);
		}
		DependentPayload p = (DependentPayload) e.getObjectValue();
		return p;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sakaiproject.memory.api.MultipleReferenceCache#put(java.lang.Object,
	 *      java.lang.Object[], java.lang.Object)
	 */
	public void put(Object key, Object[] dependentKeys, Object payload)
	{
		try
		{
			remove(key);
		}
		catch (ObjectNotCachedException e)
		{
		}
		cache.put(new Element(key, new DependentPayload(payload, dependentKeys)));

		// build the reverse index on the depenant keys
		for (Object dkey : dependentKeys)
		{
			Map<Object, Object> dependsOnThis = dependentKeyMap.get(dkey);
			if (dependsOnThis == null)
			{
				dependsOnThis = new ConcurrentHashMap<Object, Object>();
				dependentKeyMap.put(dkey, dependsOnThis);
			}
			if (!dependsOnThis.containsKey(key))
			{
				dependsOnThis.put(key, key);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sakaiproject.memory.api.MultipleReferenceCache#remove(java.lang.Object)
	 */
	public void remove(Object key) throws ObjectNotCachedException
	{
		internalRemove(key, 0);
	}

	private void internalRemove(Object key, int level)
	{

		cache.remove(key);
		if (!propagateTree && level == 0)
		{
			ReplicationControl.disableCluster();
		}
		try
		{
			Map<Object, Object> dependsOnThis = dependentKeyMap.remove(key);
			if (dependsOnThis != null)
			{
				// remove everything that depends on this
				for (Object dkey : dependsOnThis.keySet())
				{
					// if the tree is recursive, recurse down all branches.
					if (recursive)
					{
						internalRemove(key, level + 1);
					}
					else
					{
						cache.remove(dkey);
					}
				}
			}
		}
		finally
		{
			if (!propagateTree && level == 0)
			{
				ReplicationControl.enableCluster();
			}

		}

	}

	/**
	 * @return the cache
	 */
	public Cache getCache()
	{
		return cache;
	}

	/**
	 * @param cache
	 *        the cache to set
	 */
	public void setCache(Cache cache)
	{
		this.cache = cache;
	}

	/**
	 * @return the propagateTree
	 */
	public boolean isPropagateTree()
	{
		return propagateTree;
	}

	/**
	 * @param propagateTree
	 *        the propagateTree to set
	 */
	public void setPropagateTree(boolean propagateTree)
	{
		this.propagateTree = propagateTree;
	}

	/**
	 * @return the recursive
	 */
	public boolean isRecursive()
	{
		return recursive;
	}

	/**
	 * @param recursive
	 *        the recursive to set
	 */
	public void setRecursive(boolean recursive)
	{
		this.recursive = recursive;
	}

}
