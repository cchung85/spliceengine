/*

   Derby - Class com.splicemachine.db.iapi.sql.compile.AccessPath

   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to you under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */

package com.splicemachine.db.iapi.sql.compile;

import com.splicemachine.db.iapi.sql.dictionary.ConglomerateDescriptor;
import com.splicemachine.db.iapi.sql.dictionary.TableDescriptor;
import com.splicemachine.db.iapi.sql.dictionary.DataDictionary;
import com.splicemachine.db.iapi.error.StandardException;

/**
 * AccessPath represents a proposed access path for an Optimizable.
 * An Optimizable may have more than one proposed AccessPath.
 */

public interface AccessPath {
	/**
	 * Set the conglomerate descriptor for this access path.
	 *
	 * @param cd	A ConglomerateDescriptor
	 */
	void setConglomerateDescriptor(ConglomerateDescriptor cd);

	/**
	 * Get whatever was last set as the conglomerate descriptor.
	 * Returns null if nothing was set since the last call to startOptimizing()
	 */
	ConglomerateDescriptor getConglomerateDescriptor();

	/**
	 * Set the given cost estimate in this AccessPath.  Generally, this will
	 * be the CostEstimate for the plan currently under consideration.
	 */
	void setCostEstimate(CostEstimate costEstimate);

	/**
	 * Get the cost estimate for this AccessPath.  This is the last one
	 * set by setCostEstimate.
	 */
	CostEstimate getCostEstimate();

	/**
	 * Set whether or not to consider a covering index scan on the optimizable.
	 */
	void setCoveringIndexScan(boolean coveringIndexScan);

	/**
	 * Return whether or not the optimizer is considering a covering index
	 * scan on this AccessPath. 
	 *
	 * @return boolean Whether or not the optimizer chose a covering index scan.
	 */
	boolean getCoveringIndexScan();

	/**
	 * Set whether or not to consider a non-matching index scan on this AccessPath.
	 */
	void setNonMatchingIndexScan(boolean nonMatchingIndexScan);

	/**
	 * Return whether or not the optimizer is considering a non-matching
	 * index scan on this AccessPath. We expect to call this during
	 * generation, after access path selection is complete.
	 *
	 * @return boolean		Whether or not the optimizer is considering
	 *						a non-matching index scan.
	 */
	boolean getNonMatchingIndexScan();

	/**
	 * Remember the given join strategy
	 *
	 * @param joinStrategy	The best join strategy
	 */
	void setJoinStrategy(JoinStrategy joinStrategy);

	/**
	 * Get the join strategy, as set by setJoinStrategy().
	 */
	JoinStrategy getJoinStrategy();

	/**
	 * Set the lock mode
	 */
	void setLockMode(int lockMode);

	/**
	 * Get the lock mode, as last set in setLockMode().
	 */
	int getLockMode();

	/**
	 * Copy all information from the given AccessPath to this one.
	 */
	void copy(AccessPath copyFrom);

	/**
	 * Get the optimizer associated with this access path.
	 *
	 * @return	The optimizer associated with this access path.
	 */
	Optimizer getOptimizer();
	
	/**
	 * Sets the "name" of the access path. if the access path represents an
	 * index then set the name to the name of the index. if it is an index
	 * created for a constraint, use the constraint name. This is called only
	 * for base tables.
	 * 
	 * @param 	td		TableDescriptor of the base table.
	 * @param 	dd		Datadictionary.
	 *
	 * @exception StandardException 	on error.
	 */
	void initializeAccessPathName(DataDictionary dd,TableDescriptor td) throws StandardException;
}	
