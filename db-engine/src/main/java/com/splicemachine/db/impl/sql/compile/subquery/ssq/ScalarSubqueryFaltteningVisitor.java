/*
 * This file is part of Splice Machine.
 * Splice Machine is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3, or (at your option) any later version.
 * Splice Machine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License along with Splice Machine.
 * If not, see <http://www.gnu.org/licenses/>.
 *
 * Some parts of this source code are based on Apache Derby, and the following notices apply to
 * Apache Derby:
 *
 * Apache Derby is a subproject of the Apache DB project, and is licensed under
 * the Apache License, Version 2.0 (the "License"); you may not use these files
 * except in compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * Splice Machine, Inc. has modified the Apache Derby code in this file.
 *
 * All such Splice Machine modifications are Copyright 2012 - 2017 Splice Machine, Inc.,
 * and are licensed to you under the GNU Affero General Public License.
 */
package com.splicemachine.db.impl.sql.compile.subquery.ssq;

import com.splicemachine.db.iapi.error.StandardException;
import com.splicemachine.db.iapi.sql.compile.C_NodeTypes;
import com.splicemachine.db.iapi.sql.compile.Visitable;
import com.splicemachine.db.iapi.sql.compile.Visitor;
import com.splicemachine.db.impl.ast.AbstractSpliceVisitor;
import com.splicemachine.db.impl.ast.ColumnUtils;
import com.splicemachine.db.impl.sql.compile.*;
import com.splicemachine.db.impl.sql.compile.subquery.FlatteningUtils;
import org.apache.log4j.Logger;
import org.spark_project.guava.collect.Iterables;
import org.spark_project.guava.collect.Lists;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yxia on 10/11/17.
 * Flatten correlated SSQ in Select clause.
 */
public class ScalarSubqueryFaltteningVisitor extends AbstractSpliceVisitor implements Visitor {
    private static Logger LOG = Logger.getLogger(ScalarSubqueryFaltteningVisitor.class);
    private final int originalNestingLevel;
    private int flattenedCount = 0;

    public ScalarSubqueryFaltteningVisitor(int originalNestingLevel) {
        this.originalNestingLevel = originalNestingLevel;
    }

    @Override
    public boolean stopTraversal() {
        return false;
    }

    @Override
    public boolean visitChildrenFirst(Visitable node) {
        return false;
    }

    @Override
    public boolean skipChildren(Visitable node) {
        return true;
    }

    @Override
    public Visitable visit(Visitable node, QueryTreeNode parent) throws StandardException {
        // stop if selectnode does not contain SSQ
        if (!(node instanceof SelectNode) || ((SelectNode) node).getSelectSubquerys().isEmpty())
            return node;

        SelectNode topSelectNode = (SelectNode) node;

        /**
         * Stop if there are no subqueries we handle.
         */
        List<SubqueryNode> subqueryList = topSelectNode.getSelectSubquerys().getNodes();
        ScalarSubqueryPredicate scalarSubqueryPredicate = new ScalarSubqueryPredicate(topSelectNode);
        List<SubqueryNode> handledSubqueryList = Lists.newArrayList(Iterables.filter(subqueryList, scalarSubqueryPredicate));
        if (handledSubqueryList.isEmpty()) {
            return node;
        }

        /**
         * Flatten where applicable.
         */
        for (SubqueryNode subqueryNode : handledSubqueryList) {
            flatten(topSelectNode, subqueryNode);
        }

        /**
         * Finally remove the flattened subquery nodes from the top select node.
         */
        for (SubqueryNode subqueryNode : handledSubqueryList) {
            topSelectNode.getSelectSubquerys().removeElement(subqueryNode);
        }

        return node;
    }

    /**
     * Perform the actual flattening (we start to mutate the tree at this point).
     */
    private void flatten(SelectNode topSelectNode,
                         SubqueryNode subqueryNode) throws StandardException {

        /** we need to manufacture a new FromSubqueryNode with the following content:
         * 1. select list contains the original column, and all the columns in the correlated predicate;
         * 2. from clause is not changed;
         * 3. where clause contains all the non-correlated predicates
         * 4. Flag this FromSubqueryNode as "fromSSQ"
         * Also update the outer query as follows:
         * 1. add the newly created FromSubuqeryNode to fromList;
         * 2. add the correlated predicate to the Where clause
         */

        ResultSetNode subqueryResultSet = subqueryNode.getResultSet();
        SelectNode subquerySelectNode = (SelectNode) subqueryResultSet;

        /**
         * The following lines collect correlated predicates from the subquery where clause while removing them.
         */
        ValueNode subqueryWhereClause = subquerySelectNode.getWhereClause();
        List<BinaryRelationalOperatorNode> correlatedSubqueryPreds = new ArrayList<>();
        subqueryWhereClause = FlatteningUtils.findCorrelatedSubqueryPredicates(
                    subqueryWhereClause,
                    correlatedSubqueryPreds,
                    new org.spark_project.guava.base.Predicate<BinaryRelationalOperatorNode>() {
                        @Override
                        public boolean apply(BinaryRelationalOperatorNode bron) {
                            boolean isCorrelated;
                            try {
                            isCorrelated = ColumnUtils.isSubtreeCorrelated(bron.getLeftOperand()) ||
                                    ColumnUtils.isSubtreeCorrelated(bron.getRightOperand());
                            } catch (StandardException e) {
                                LOG.error("unexpected exception while considreing scalar subquery flattening", e);
                                /* not expected, programmer error */
                                throw new IllegalStateException(e);
                            }

                            return isCorrelated;
                        }
                    });
        subquerySelectNode.setWhereClause(subqueryWhereClause);
        subquerySelectNode.setOriginalWhereClause(subqueryWhereClause);

        ResultColumnList newRcl = subquerySelectNode.getResultColumns().copyListAndObjects();
        newRcl.genVirtualColumnNodes(subquerySelectNode, subquerySelectNode.getResultColumns());

        /*
         * Insert the new FromSubquery into to origSelectNode's From list.
         */
        FromSubquery fromSubquery = (FromSubquery) topSelectNode.getNodeFactory().getNode(C_NodeTypes.FROM_SUBQUERY,
                subqueryResultSet,
                null,                  // order by
                null,                  // offset
                null,                  // fetchFirst
                false,                 // hasJDBClimitClause
                getSubqueryAlias(),
                newRcl,
                null,
                topSelectNode.getContextManager());
        fromSubquery.setTableNumber(topSelectNode.getCompilerContext().getNextTableNumber());
        fromSubquery.setFromSSQ(true);

        topSelectNode.getFromList().addFromTable(fromSubquery);

        /*
         * replace subquery in Select clause with columnreference
         */
        ResultColumnList topRcl = topSelectNode.getResultColumns();
        ReplaceSubqueryWithColRefVisitor replaceSubqueryWithColRefVisitor = new ReplaceSubqueryWithColRefVisitor(fromSubquery, topSelectNode.getNestingLevel());

        for (ResultColumn rc: topRcl) {
            rc.accept(replaceSubqueryWithColRefVisitor);
        }
        /*
         * Add correlated predicates from subquery to outer query where clause.
         */
        ValueNode newTopWhereClause = topSelectNode.getWhereClause();
        int subqueryNestingLevel = subquerySelectNode.getNestingLevel();
        ScalarSubqueryCorrelatedPredicateVisitor scalarSubqueryCorrelatedPredicateVisitor =
                  new ScalarSubqueryCorrelatedPredicateVisitor(fromSubquery, subqueryNestingLevel, topSelectNode);

        for (int i = 0; i < correlatedSubqueryPreds.size(); i++) {
            BinaryRelationalOperatorNode pred = correlatedSubqueryPreds.get(i);
            pred.accept(scalarSubqueryCorrelatedPredicateVisitor);
            /*
             * Finally add the predicate to the outer query.
             */
            if (newTopWhereClause != null)
                newTopWhereClause = FlatteningUtils.addPredToTree(newTopWhereClause, pred);
            else
                newTopWhereClause = pred;
        }

        topSelectNode.setOriginalWhereClause(newTopWhereClause);
        topSelectNode.setWhereClause(newTopWhereClause);
        return;
    }

    private String getSubqueryAlias() {
        return String.format("SSQFlatSub-%s-%s", originalNestingLevel, ++flattenedCount);
    }
}