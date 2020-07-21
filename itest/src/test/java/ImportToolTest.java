/*
 * Copyright (c) 2002-2019 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


import org.junit.Rule;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.Iterables;
import org.neo4j.helpers.collection.Iterators;
import org.neo4j.test.rule.EmbeddedDatabaseRule;
import org.neo4j.test.rule.RandomRule;
import org.neo4j.test.rule.SuppressOutput;
import org.neo4j.tooling.ImportTool;

import java.io.File;
import java.io.IOException;
import java.util.function.IntPredicate;

import static org.junit.Assert.assertEquals;
import static org.neo4j.graphdb.Label.label;

public class ImportToolTest
{
    private static final int MAX_LABEL_ID = 4;
    private static final int RELATIONSHIP_COUNT = 10_000;
    private static final int NODE_COUNT = 100;
    private static final IntPredicate TRUE = i -> true;

    @Rule
    public final EmbeddedDatabaseRule dbRule = new EmbeddedDatabaseRule().startLazily();
    @Rule
    public final RandomRule random = new RandomRule();
    @Rule
    public final SuppressOutput suppressOutput = SuppressOutput.suppress( SuppressOutput.System.values() );
    private int dataIndex;




    @Test
    public void importBlobTest() throws Exception
    {

        // GIVEN
        File header = new File(  "test-header.csv"  );
        File data = new File( "test.csv"  );
        importTool( "--into", dbRule.getStoreDirAbsolutePath(),
                "--additional-config", "neo4j.conf",
                "--array-delimiter", "|",
                "--nodes", header.getAbsolutePath() + "," + data.getAbsolutePath() );


        try ( Transaction tx = dbRule.beginTx() )
        {
            long nodeCount = Iterables.count( dbRule.getAllNodes() );
            System.out.println("XXXX:"+nodeCount);
            assertEquals( 3, nodeCount );

            tx.success();
            ResourceIterator<Node> nodes = dbRule.findNodes(label("Movie"));
            assertEquals( 1, Iterators.asList( nodes ).size() );
        }
    }




    static void importTool( String... arguments ) throws IOException
    {
        ImportTool.main( arguments, true );
    }
}
